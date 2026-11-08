package com.gaurang847;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinDef;
import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;

import java.nio.file.Files;
import java.nio.file.Path;

public class WallpaperSetter {

    private static final String REGISTRY_DESKTOP_KEY = "Control Panel\\Desktop";

    /**
     * Checks if the host operating system is Windows.
     */
    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Translates human-readable wallpaper style to Windows registry value using modern switch expression.
     */
    public static String toRegistryStyle(String styleName) {
        return switch (styleName) {
            case "Fit" -> "6";
            case "Center" -> "0";
            case "Tile" -> "1";
            case "Span" -> "22";
            default -> "2"; // Fill / Stretch
        };
    }

    /**
     * Updates Windows desktop registry values for wallpaper display style.
     */
    public void setRegistryValues(String style) {
        if (!isWindows()) {
            System.out.println("[WallpaperSetter] Skipping registry update (not running on Windows).");
            return;
        }

        try {
            boolean isTiled = "1".equals(style);
            Advapi32Util.registrySetStringValue(
                    HKEY_CURRENT_USER, REGISTRY_DESKTOP_KEY, "TileWallpaper", isTiled ? "1" : "0"
            );

            Advapi32Util.registrySetStringValue(
                    HKEY_CURRENT_USER, REGISTRY_DESKTOP_KEY, "WallpaperStyle", style != null ? style : "2"
            );
        } catch (Exception e) {
            System.err.println("[WallpaperSetter] Failed to set registry values: " + e.getMessage());
        }
    }

    /**
     * Sets the specified image as the Windows desktop background.
     *
     * @param imagePath Path to the image file
     * @return true if successful, false otherwise
     */
    public boolean setWallpaper(Path imagePath) {
        if (imagePath == null) {
            System.err.println("[WallpaperSetter] Invalid image path: null.");
            return false;
        }

        if (!Files.isRegularFile(imagePath)) {
            System.err.println("[WallpaperSetter] File does not exist: " + imagePath);
            return false;
        }

        var absolutePath = imagePath.toAbsolutePath().toString();

        if (!isWindows()) {
            System.out.println("[WallpaperSetter] Simulating wallpaper set (not on Windows): " + absolutePath);
            return true;
        }

        try {
            var style = AppConfig.getInstance().data().wallpaperStyle();
            setRegistryValues(style);

            boolean result = SPI.INSTANCE.SystemParametersInfo(
                    new WinDef.UINT_PTR(SPI.SPI_SETDESKWALLPAPER),
                    new WinDef.UINT_PTR(0),
                    absolutePath,
                    new WinDef.UINT_PTR(SPI.SPIF_UPDATEINIFILE | SPI.SPIF_SENDWININICHANGE)
            );

            int lastError = Native.getLastError();
            if (!result || lastError != 0) {
                System.err.println("[WallpaperSetter] SystemParametersInfo reported code: " + lastError);
            }

            return result;
        } catch (Throwable t) {
            System.err.println("[WallpaperSetter] Error setting wallpaper: " + t.getMessage());
            return false;
        }
    }

    public boolean setWallpaper(String imagePath) {
        return imagePath != null && !imagePath.isBlank() && setWallpaper(Path.of(imagePath));
    }
}
