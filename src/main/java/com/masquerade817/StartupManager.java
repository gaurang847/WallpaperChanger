package com.masquerade817;

import com.sun.jna.platform.win32.Advapi32Util;
import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages Windows Startup registry entry (HKCU\Software\Microsoft\Windows\CurrentVersion\Run).
 * Allows the application to start automatically on system startup in the background.
 */
public class StartupManager {

    public static final String APP_NAME = "WallpaperChanger";
    private static final String RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    /**
     * Checks if startup management is supported on the current platform.
     */
    public static boolean isSupported() {
        return WallpaperSetter.isWindows();
    }

    /**
     * Checks if WallpaperChanger is currently registered to start at Windows startup.
     */
    public static boolean isStartupEnabled() {
        if (!isSupported()) {
            return false;
        }
        try {
            return Advapi32Util.registryValueExists(HKEY_CURRENT_USER, RUN_KEY, APP_NAME);
        } catch (Exception e) {
            System.err.println("[StartupManager] Unable to check startup status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds the running JAR file path, or null if running outside a packaged JAR.
     */
    public static Path getRunningJar() {
        try {
            URI uri = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            var path = Path.of(uri);
            if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".jar")) {
                return path;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Constructs the command to run at startup using javaw (no console window) in --background mode.
     */
    public static String buildStartupCommand() {
        var jarPath = getRunningJar();
        var javaHome = Path.of(System.getProperty("java.home"));
        var javawPath = javaHome.resolve("bin").resolve("javaw.exe");
        var javaBin = Files.exists(javawPath) ? javawPath.toAbsolutePath().toString() : "javaw";

        if (jarPath != null) {
            return "\"%s\" -jar \"%s\" --background".formatted(javaBin, jarPath.toAbsolutePath());
        } else {
            return "\"%s\" -cp \"%s\" com.masquerade817.Main --background".formatted(
                    javaBin, System.getProperty("java.class.path")
            );
        }
    }

    /**
     * Enables or disables running at system startup.
     */
    public static boolean setStartupEnabled(boolean enable) {
        if (!isSupported()) {
            System.out.println("[StartupManager] Skipping startup registration (not on Windows).");
            return false;
        }

        try {
            if (enable) {
                var command = buildStartupCommand();
                Advapi32Util.registrySetStringValue(HKEY_CURRENT_USER, RUN_KEY, APP_NAME, command);
                System.out.println("[StartupManager] Registered startup command: " + command);
                return true;
            } else {
                if (Advapi32Util.registryValueExists(HKEY_CURRENT_USER, RUN_KEY, APP_NAME)) {
                    Advapi32Util.registryDeleteValue(HKEY_CURRENT_USER, RUN_KEY, APP_NAME);
                    System.out.println("[StartupManager] Removed startup registry entry.");
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("[StartupManager] Error updating Windows startup registry: " + e.getMessage());
            return false;
        }
    }
}
