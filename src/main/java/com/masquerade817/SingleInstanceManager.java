package com.masquerade817;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ensures only a single instance of the application is running.
 * Uses an OS-level file lock combined with a local loopback IPC socket.
 * If another instance is already running, it brings the existing instance's window to the front and exits.
 */
public class SingleInstanceManager {

    public interface InstanceListener {
        void onActivateRequested();
    }

    private static final Path APP_DIR = resolveAppDir();
    private static final Path LOCK_FILE = APP_DIR.resolve("app.lock");
    private static final Path PORT_FILE = APP_DIR.resolve("app.port");

    private static Path resolveAppDir() {
        var defaultDir = Path.of(System.getProperty("user.home"), ".wallpaperchanger");
        try {
            if (!Files.exists(defaultDir)) {
                Files.createDirectories(defaultDir);
            }
            var testFile = defaultDir.resolve(".test_rw");
            Files.writeString(testFile, "test");
            Files.deleteIfExists(testFile);
            return defaultDir;
        } catch (Exception e) {
            var fallback = Path.of(System.getProperty("java.io.tmpdir"), ".wallpaperchanger");
            try {
                if (!Files.exists(fallback)) {
                    Files.createDirectories(fallback);
                }
                return fallback;
            } catch (Exception ignored) {
                return Path.of(".wallpaperchanger");
            }
        }
    }

    private static FileChannel lockChannel;
    private static FileLock fileLock;
    private static ServerSocket serverSocket;
    private static volatile boolean isRunning = false;

    /**
     * Attempts to acquire the single-instance lock.
     *
     * @param listener Callback triggered when another instance requests activation
     * @return true if this is the only running instance; false if another instance is already active
     */
    public static boolean checkAndAcquireLock(InstanceListener listener) {
        try {
            if (!Files.exists(APP_DIR)) {
                Files.createDirectories(APP_DIR);
            }

            var raf = new RandomAccessFile(LOCK_FILE.toFile(), "rw");
            lockChannel = raf.getChannel();
            fileLock = lockChannel.tryLock();

            if (fileLock == null) {
                // Lock held by another active process
                notifyExistingInstance();
                cleanupChannel();
                return false;
            }

            // Lock acquired successfully! Start local IPC listener
            startIpcServer(listener);
            return true;

        } catch (Exception e) {
            // Any lock collision or IO error indicates another instance is holding the file
            notifyExistingInstance();
            cleanupChannel();
            return false;
        }
    }

    private static void startIpcServer(InstanceListener listener) {
        try {
            serverSocket = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
            int port = serverSocket.getLocalPort();
            Files.writeString(PORT_FILE, String.valueOf(port));
            isRunning = true;

            // Virtual Thread listener for activation requests
            Thread.ofVirtual().name("single-instance-listener").start(() -> {
                while (isRunning && !serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        Thread.ofVirtual().start(() -> handleClient(client, listener));
                    } catch (Exception e) {
                        if (!isRunning) break;
                    }
                }
            });

            // Clean shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(SingleInstanceManager::cleanup));

        } catch (Exception e) {
            System.err.println("[SingleInstanceManager] Could not start IPC server: " + e.getMessage());
        }
    }

    private static void handleClient(Socket client, InstanceListener listener) {
        try (client;
             var reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             var writer = new PrintWriter(client.getOutputStream(), true)) {

            String command = reader.readLine();
            if ("ACTIVATE".equalsIgnoreCase(command)) {
                writer.println("OK");
                if (listener != null) {
                    listener.onActivateRequested();
                }
            } else if ("SHUTDOWN".equalsIgnoreCase(command) || "EXIT".equalsIgnoreCase(command)) {
                writer.println("OK");
                cleanup();
                System.exit(0);
            }
        } catch (Exception ignored) {
        }
    }

    private static void notifyExistingInstance() {
        System.out.println("[SingleInstanceManager] Another instance of Wallpaper Changer is already running.");
        try {
            if (Files.exists(PORT_FILE)) {
                int port = Integer.parseInt(Files.readString(PORT_FILE).trim());
                try (var socket = new Socket("127.0.0.1", port);
                     var writer = new PrintWriter(socket.getOutputStream(), true);
                     var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    socket.setSoTimeout(2500);
                    writer.println("ACTIVATE");
                    reader.readLine();
                    System.out.println("[SingleInstanceManager] Sent focus signal to existing instance.");
                }
            }
        } catch (Exception e) {
            System.err.println("[SingleInstanceManager] Could not notify existing instance: " + e.getMessage());
        }
    }

    private static synchronized void cleanupChannel() {
        try {
            if (lockChannel != null && lockChannel.isOpen()) {
                lockChannel.close();
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void cleanup() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}

        try {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
            }
        } catch (Exception ignored) {}

        cleanupChannel();

        try {
            Files.deleteIfExists(PORT_FILE);
            Files.deleteIfExists(LOCK_FILE);
        } catch (Exception ignored) {}
    }
}
