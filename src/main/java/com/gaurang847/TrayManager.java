package com.gaurang847;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TrayManager {

    private final AppConfig config;
    private final WallpaperSyncService syncService;
    private final Runnable openSettingsCallback;

    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private MenuItem pauseMenuItem;

    public TrayManager(AppConfig config, WallpaperSyncService syncService, Runnable openSettingsCallback) {
        this.config = config;
        this.syncService = syncService;
        this.openSettingsCallback = openSettingsCallback;
    }

    /**
     * Initializes and displays the System Tray icon and menu if supported.
     */
    public boolean initTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("[TrayManager] System Tray is not supported on this platform.");
            return false;
        }

        try {
            this.systemTray = SystemTray.getSystemTray();
            Image iconImage = AppIconUtil.getAppIcon(16);

            var popup = createPopupMenu();
            trayIcon = new TrayIcon(iconImage, "Wallpaper Changer", popup);
            trayIcon.setImageAutoSize(true);

            // Double click or single click opens Settings
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() >= 1 && e.getButton() == MouseEvent.BUTTON1) {
                        if (openSettingsCallback != null) {
                            openSettingsCallback.run();
                        }
                    }
                }
            });

            systemTray.add(trayIcon);
            updateTooltip();
            return true;
        } catch (AWTException e) {
            System.err.println("[TrayManager] Failed to add icon to System Tray: " + e.getMessage());
            return false;
        }
    }

    private PopupMenu createPopupMenu() {
        var popup = new PopupMenu();

        var titleItem = new MenuItem("Wallpaper Changer");
        titleItem.setEnabled(false);
        popup.add(titleItem);
        popup.addSeparator();

        var changeNow = new MenuItem("Change Wallpaper Now");
        changeNow.addActionListener(e -> {
            showNotification("Wallpaper Changer", "Downloading and changing wallpaper...", TrayIcon.MessageType.INFO);
            syncService.changeWallpaperNow();
        });
        popup.add(changeNow);

        pauseMenuItem = new MenuItem(syncService.isPaused() ? "Resume Auto-Change" : "Pause Auto-Change");
        pauseMenuItem.addActionListener(e -> {
            boolean willPause = !syncService.isPaused();
            syncService.setPaused(willPause);
            pauseMenuItem.setLabel(willPause ? "Resume Auto-Change" : "Pause Auto-Change");
            updateTooltip();
            showNotification(
                    "Wallpaper Changer",
                    willPause ? "Periodic wallpaper change paused." : "Periodic wallpaper change resumed.",
                    TrayIcon.MessageType.INFO
            );
        });
        popup.add(pauseMenuItem);

        var openFolder = new MenuItem("Open Wallpaper Folder");
        openFolder.addActionListener(e -> openLocalFolder());
        popup.add(openFolder);

        var settings = new MenuItem("Settings...");
        settings.addActionListener(e -> {
            if (openSettingsCallback != null) {
                openSettingsCallback.run();
            }
        });
        popup.add(settings);

        popup.addSeparator();

        var exit = new MenuItem("Exit");
        exit.addActionListener(e -> Main.exitApplication());
        popup.add(exit);

        return popup;
    }

    private void openLocalFolder() {
        try {
            var folderStr = config.data().localFolderPath();
            if (folderStr != null && !folderStr.isBlank()) {
                var folder = Path.of(folderStr);
                if (!Files.exists(folder)) {
                    Files.createDirectories(folder);
                }
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(folder.toFile());
                }
            }
        } catch (IOException e) {
            showNotification("Wallpaper Changer", "Unable to open folder: " + e.getMessage(), TrayIcon.MessageType.WARNING);
        }
    }

    public void updateTooltip() {
        if (trayIcon != null) {
            var status = syncService.isPaused() ? "Paused" : "Running";
            trayIcon.setToolTip("Wallpaper Changer - " + status);
        }
    }

    public void showNotification(String title, String message, TrayIcon.MessageType type) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, type);
        }
    }

    public void cleanup() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }
}
