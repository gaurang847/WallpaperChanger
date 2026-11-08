package com.masquerade817;

import com.masquerade817.DriveConnector.WallpaperItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WallpaperSyncService {

    /**
     * Java 21 Record for sync result notification.
     */
    public record SyncResult(boolean success, String wallpaperName, Path localPath, String errorMessage) {}

    public interface SyncListener {
        void onSyncStarted();
        void onProgress(String status);
        void onSyncSuccess(String wallpaperName, Path localPath);
        void onSyncFailed(String reason, Throwable error);
        void onScheduleUpdated(Date nextRunTime);
    }

    private final AppConfig config;
    private final DriveConnector driveConnector;
    private final WallpaperSetter wallpaperSetter;
    private final List<SyncListener> listeners = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "WallpaperSync-Scheduler");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> scheduledTask;
    private boolean isPaused = false;
    private boolean isSyncInProgress = false;
    private Date lastSyncTime;
    private Date nextSyncTime;
    private String lastStatusMessage = "Ready";

    public WallpaperSyncService(AppConfig config) {
        this(config, new DriveConnector(), new WallpaperSetter());
    }

    WallpaperSyncService(AppConfig config, DriveConnector driveConnector, WallpaperSetter wallpaperSetter) {
        this.config = config;
        this.driveConnector = driveConnector;
        this.wallpaperSetter = wallpaperSetter;
    }

    public void addListener(SyncListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SyncListener listener) {
        listeners.remove(listener);
    }

    public synchronized void start() {
        scheduleNext(config.data().frequencyMinutes(), false);
    }

    public synchronized void reschedule() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduleNext(config.data().frequencyMinutes(), false);
    }

    private synchronized void scheduleNext(long minutes, boolean immediate) {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }

        long initialDelay = immediate ? 0 : minutes;
        scheduledTask = scheduler.scheduleWithFixedDelay(
                this::runScheduledSyncOnVirtualThread,
                initialDelay,
                minutes,
                TimeUnit.MINUTES
        );

        this.nextSyncTime = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(initialDelay));
        notifyScheduleUpdated(this.nextSyncTime);
    }

    /**
     * Executes sync on a modern Java 21 Virtual Thread so it doesn't block OS threads.
     */
    private void runScheduledSyncOnVirtualThread() {
        if (isPaused) {
            System.out.println("[WallpaperSyncService] Periodic sync skipped: Service is paused.");
            return;
        }
        Thread.ofVirtual().name("wallpaper-sync-worker").start(this::performSyncAndChange);
    }

    /**
     * Triggers immediate wallpaper change on a Virtual Thread.
     */
    public void changeWallpaperNow() {
        Thread.ofVirtual().name("wallpaper-sync-now").start(this::performSyncAndChange);
    }

    /**
     * Core synchronization and wallpaper rotation logic.
     */
    public synchronized boolean performSyncAndChange() {
        if (isSyncInProgress) {
            System.out.println("[WallpaperSyncService] Sync already in progress, skipping.");
            return false;
        }

        isSyncInProgress = true;
        notifySyncStarted();

        try {
            var cfg = config.data();

            // 1. Validation
            var folderId = cfg.driveFolderId();
            if (folderId == null || folderId.isBlank()) {
                throw new IllegalStateException("Google Drive folder ID is not configured.");
            }

            var localFolderStr = cfg.localFolderPath();
            if (localFolderStr == null || localFolderStr.isBlank()) {
                throw new IllegalStateException("Local wallpaper directory is not configured.");
            }

            var localDirectory = Path.of(localFolderStr);
            if (!Files.exists(localDirectory)) {
                Files.createDirectories(localDirectory);
            }

            notifyProgress("Checking Google Drive folder...");
            var images = driveConnector.getImagesFromDriveFolder(
                    folderId,
                    cfg.includeSubfolders(),
                    cfg.subfolderDepth()
            );

            if (images.isEmpty()) {
                String emptyMsg = cfg.includeSubfolders()
                        ? "No wallpaper images found in Google Drive folder or its subfolders."
                        : "No wallpaper images found in Google Drive folder. If images are in subfolders, enable 'Include subfolders'.";
                System.out.println("[WallpaperSyncService] " + emptyMsg + " Existing desktop wallpaper was kept.");
                this.lastStatusMessage = emptyMsg;
                this.nextSyncTime = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(config.data().frequencyMinutes()));
                notifySyncFailed(emptyMsg, null);
                notifyScheduleUpdated(this.nextSyncTime);
                return false;
            }

            // 2. Select random wallpaper
            var lastDriveId = cfg.lastWallpaperDriveId();
            var selectedFile = driveConnector.selectRandomWallpaper(images, lastDriveId);

            notifyProgress("Downloading %s...".formatted(selectedFile.name()));
            var downloadedPath = driveConnector.downloadWallpaper(
                    selectedFile,
                    localDirectory,
                    new CustomProgressListener.DownloadCallback() {
                        @Override
                        public void onProgress(double progress) {
                            notifyProgress("Downloading %s (%.0f%%)...".formatted(selectedFile.name(), progress * 100));
                        }

                        @Override
                        public void onComplete() {
                            notifyProgress("Download complete. Applying wallpaper...");
                        }
                    }
            );

            // 3. Set Desktop Wallpaper
            notifyProgress("Setting desktop background...");
            boolean success = wallpaperSetter.setWallpaper(downloadedPath);
            if (!success) {
                throw new IOException("Failed to set desktop wallpaper using Windows API.");
            }

            // 4. Delete unset wallpaper if enabled
            var previousWallpaperStr = cfg.lastWallpaperPath();
            if (cfg.deleteUnsetWallpaper()) {
                cleanupUnsetWallpapers(localDirectory, downloadedPath, previousWallpaperStr);
            }

            // 5. Update state
            config.update(cfg.withLastWallpaper(downloadedPath.toAbsolutePath().toString(), selectedFile.id()));

            this.lastSyncTime = new Date();
            this.lastStatusMessage = "Updated to " + selectedFile.name();
            this.nextSyncTime = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(config.data().frequencyMinutes()));

            notifySyncSuccess(selectedFile.name(), downloadedPath);
            notifyScheduleUpdated(this.nextSyncTime);
            return true;

        } catch (Throwable t) {
            var msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
            this.lastStatusMessage = "Failed: " + msg;
            System.err.println("[WallpaperSyncService] Error during wallpaper sync: " + msg);
            notifySyncFailed(msg, t);
            return false;
        } finally {
            isSyncInProgress = false;
        }
    }

    /**
     * Cleans up unset wallpapers using modern Java NIO Streams.
     */
    private void cleanupUnsetWallpapers(Path localDirectory, Path currentActiveFile, String previousPathStr) {
        // Allow Windows brief moment to release handles on old wallpaper
        try {
            Thread.sleep(Duration.ofMillis(1500));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        Path activeRealPath;
        try {
            activeRealPath = currentActiveFile.toRealPath();
        } catch (IOException e) {
            activeRealPath = currentActiveFile.toAbsolutePath();
        }

        // Delete explicitly tracked previous wallpaper
        if (previousPathStr != null && !previousPathStr.isBlank()) {
            var prevPath = Path.of(previousPathStr);
            try {
                if (Files.exists(prevPath) && !prevPath.toRealPath().equals(activeRealPath)) {
                    Files.deleteIfExists(prevPath);
                    System.out.println("[WallpaperSyncService] Deleted unset wallpaper: " + prevPath.getFileName());
                }
            } catch (IOException ignored) {}
        }

        // Clean any leftover orphan files using NIO Stream
        try (var stream = Files.list(localDirectory)) {
            final var active = activeRealPath;
            stream.filter(Files::isRegularFile)
                  .forEach(file -> {
                      try {
                          if (!file.toRealPath().equals(active)) {
                              Files.deleteIfExists(file);
                              System.out.println("[WallpaperSyncService] Cleaned orphan unset file: " + file.getFileName());
                          }
                      } catch (IOException ignored) {}
                  });
        } catch (IOException ignored) {}
    }

    public synchronized void setPaused(boolean paused) {
        this.isPaused = paused;
        this.lastStatusMessage = paused ? "Paused" : "Running";
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isSyncInProgress() {
        return isSyncInProgress;
    }

    public Date getLastSyncTime() {
        return lastSyncTime;
    }

    public Date getNextSyncTime() {
        return nextSyncTime;
    }

    public String getLastStatusMessage() {
        return lastStatusMessage;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    // Listener notifications

    private void notifySyncStarted() {
        for (var l : listeners) {
            try { l.onSyncStarted(); } catch (Exception ignored) {}
        }
    }

    private void notifyProgress(String status) {
        for (var l : listeners) {
            try { l.onProgress(status); } catch (Exception ignored) {}
        }
    }

    private void notifySyncSuccess(String name, Path path) {
        for (var l : listeners) {
            try { l.onSyncSuccess(name, path); } catch (Exception ignored) {}
        }
    }

    private void notifySyncFailed(String reason, Throwable error) {
        for (var l : listeners) {
            try { l.onSyncFailed(reason, error); } catch (Exception ignored) {}
        }
    }

    private void notifyScheduleUpdated(Date nextRun) {
        for (var l : listeners) {
            try { l.onScheduleUpdated(nextRun); } catch (Exception ignored) {}
        }
    }
}
