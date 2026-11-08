package com.gaurang847;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.TrayIcon;
import java.nio.file.Path;

public class Main {

    private static SettingsFrame settingsFrame;
    private static TrayManager trayManager;
    private static WallpaperSyncService syncService;

    public static void main(String[] args) {
        // Enforce single running instance. If an instance is already active,
        // it is brought to the foreground and this duplicate process terminates.
        boolean isOnlyInstance = SingleInstanceManager.checkAndAcquireLock(() -> {
            SwingUtilities.invokeLater(() -> {
                if (settingsFrame != null) {
                    settingsFrame.setVisible(true);
                    settingsFrame.toFront();
                    settingsFrame.requestFocus();
                }
            });
        });

        if (!isOnlyInstance) {
            System.exit(0);
            return;
        }

        boolean backgroundMode = false;
        for (var arg : args) {
            var lower = arg.toLowerCase();
            if (lower.equals("--background") || lower.equals("-b") ||
                lower.equals("--minimized") || lower.equals("--tray") ||
                lower.equals("--silent")) {
                backgroundMode = true;
                break;
            }
        }

        final boolean runInBackground = backgroundMode;

        // Initialize modern FlatLaf Look and Feel with fallback to System Look and Feel
        try {
            FlatLightLaf.setup();
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        SwingUtilities.invokeLater(() -> initializeApp(runInBackground));
    }

    private static void initializeApp(boolean runInBackground) {
        var config = AppConfig.getInstance();

        // Ensure Windows Startup registration is in sync with user config
        if (config.data().startOnStartup() && StartupManager.isSupported()) {
            if (!StartupManager.isStartupEnabled()) {
                StartupManager.setStartupEnabled(true);
            }
        }

        // Initialize background wallpaper sync service
        syncService = new WallpaperSyncService(config);

        // Initialize System Tray
        trayManager = new TrayManager(config, syncService, () -> {
            if (settingsFrame != null) {
                settingsFrame.setVisible(true);
                settingsFrame.toFront();
                settingsFrame.requestFocus();
            }
        });
        trayManager.initTray();

        // Listen for sync events to display System Tray notifications
        syncService.addListener(new WallpaperSyncService.SyncListener() {
            private String lastFailureReason = null;

            @Override
            public void onSyncStarted() {
            }

            @Override
            public void onProgress(String status) {
            }

            @Override
            public void onSyncSuccess(String wallpaperName, Path localPath) {
                lastFailureReason = null;
                trayManager.showNotification(
                        "Wallpaper Changed",
                        "New desktop background set: " + wallpaperName,
                        TrayIcon.MessageType.INFO
                );
            }

            @Override
            public void onSyncFailed(String reason, Throwable error) {
                // Throttle notifications so repeated intervals with the same empty/warning state do not spam balloons
                if (!java.util.Objects.equals(reason, lastFailureReason)) {
                    lastFailureReason = reason;
                    trayManager.showNotification(
                            "Wallpaper Sync Warning",
                            reason,
                            TrayIcon.MessageType.WARNING
                    );
                }
            }

            @Override
            public void onScheduleUpdated(java.util.Date nextRunTime) {
            }
        });

        // Create the Settings GUI Frame
        settingsFrame = new SettingsFrame(config, syncService, trayManager);

        // Start background scheduler
        syncService.start();

        // If launched in background mode and folder is already configured, stay in tray.
        // Otherwise, open the settings frame so user can configure Drive and preferences.
        var folderId = config.data().driveFolderId();
        boolean isConfigured = folderId != null && !folderId.isBlank();

        if (!runInBackground || !isConfigured) {
            settingsFrame.setVisible(true);
        } else {
            System.out.println("[WallpaperChanger] Running in background mode (System Tray).");
            trayManager.showNotification(
                    "Wallpaper Changer",
                    "Started in background mode. Click the tray icon to open settings.",
                    TrayIcon.MessageType.INFO
            );
        }
    }

    public static void exitApplication() {
        if (settingsFrame != null) {
            settingsFrame.performExit();
        } else {
            if (trayManager != null) {
                trayManager.cleanup();
            }
            if (syncService != null) {
                syncService.shutdown();
            }
            SingleInstanceManager.cleanup();
            System.exit(0);
        }
    }
}
