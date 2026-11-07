package com.masquerade817;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Modern Java 21 configuration manager using records and JSON persistence.
 */
public class AppConfig {

    private static final String CONFIG_DIR_NAME = ".wallpaperchanger";
    private static final String CONFIG_FILE_NAME = "config.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Pattern FOLDER_PATTERN = Pattern.compile("folders/([a-zA-Z0-9_-]+)");
    private static final Pattern ID_PARAM_PATTERN = Pattern.compile("[?&]id=([a-zA-Z0-9_-]+)");
    private static final Pattern D_PATTERN = Pattern.compile("/d/([a-zA-Z0-9_-]+)");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConfigData(
            String driveFolderId,
            String localFolderPath,
            int frequencyMinutes,
            boolean deleteUnsetWallpaper,
            boolean startOnStartup,
            String customCredentialsPath,
            String lastWallpaperPath,
            String lastWallpaperDriveId,
            String wallpaperStyle
    ) {
        public static ConfigData defaultSettings() {
            var defaultLocal = Path.of(System.getProperty("user.home"), "Pictures", "WallpaperChanger")
                    .toAbsolutePath()
                    .toString();
            return new ConfigData("", defaultLocal, 60, true, true, "", "", "", "2");
        }

        public ConfigData withLastWallpaper(String path, String driveId) {
            return new ConfigData(
                    driveFolderId, localFolderPath, frequencyMinutes, deleteUnsetWallpaper,
                    startOnStartup, customCredentialsPath, path, driveId, wallpaperStyle
            );
        }

        public ConfigData withDriveFolderId(String folderId) {
            return new ConfigData(
                    folderId, localFolderPath, frequencyMinutes, deleteUnsetWallpaper,
                    startOnStartup, customCredentialsPath, lastWallpaperPath, lastWallpaperDriveId, wallpaperStyle
            );
        }
    }

    private static AppConfig instance;
    private ConfigData data;
    private final Path configPath;

    public AppConfig() {
        var userHome = Path.of(System.getProperty("user.home"));
        var configDir = userHome.resolve(CONFIG_DIR_NAME);

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            var testFile = configDir.resolve(".test_rw");
            Files.writeString(testFile, "test");
            Files.deleteIfExists(testFile);
        } catch (Exception e) {
            configDir = Path.of(System.getProperty("java.io.tmpdir"), CONFIG_DIR_NAME);
            try {
                if (!Files.exists(configDir)) {
                    Files.createDirectories(configDir);
                }
            } catch (Exception ignored) {
            }
        }

        this.configPath = configDir.resolve(CONFIG_FILE_NAME);
        this.data = ConfigData.defaultSettings();
        load();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public ConfigData data() {
        return data;
    }

    public synchronized void update(ConfigData newData) {
        this.data = newData;
        save();
    }

    public synchronized void load() {
        if (!Files.exists(configPath)) {
            // Check if legacy properties file exists and migrate
            var legacyPropFile = configPath.getParent().resolve("config.properties");
            if (Files.exists(legacyPropFile)) {
                migrateLegacyProperties(legacyPropFile);
            } else {
                save();
            }
            return;
        }

        try {
            this.data = MAPPER.readValue(configPath.toFile(), ConfigData.class);
        } catch (IOException e) {
            System.err.println("Warning: Unable to read config.json, using defaults: " + e.getMessage());
            this.data = ConfigData.defaultSettings();
        }
    }

    public synchronized void save() {
        try {
            MAPPER.writeValue(configPath.toFile(), data);
        } catch (IOException e) {
            System.err.println("Error saving config.json: " + e.getMessage());
        }
    }

    private void migrateLegacyProperties(Path legacyPropFile) {
        try (var in = Files.newInputStream(legacyPropFile)) {
            var props = new java.util.Properties();
            props.load(in);

            var folderId = props.getProperty("drive.folder.id", "").trim();
            var localPath = props.getProperty("local.folder.path", data.localFolderPath()).trim();
            int freq = 60;
            try {
                freq = Integer.parseInt(props.getProperty("change.frequency.minutes", "60"));
            } catch (NumberFormatException ignored) {}

            boolean deleteUnset = Boolean.parseBoolean(props.getProperty("delete.unset.wallpaper", "true"));
            boolean startBoot = Boolean.parseBoolean(props.getProperty("start.on.startup", "true"));
            var customCreds = props.getProperty("custom.credentials.path", "").trim();
            var lastPath = props.getProperty("last.wallpaper.path", "").trim();
            var lastId = props.getProperty("last.wallpaper.drive.id", "").trim();
            var style = props.getProperty("wallpaper.style", "2").trim();

            this.data = new ConfigData(folderId, localPath, freq, deleteUnset, startBoot, customCreds, lastPath, lastId, style);
            save();
            System.out.println("Migrated legacy config.properties to modern config.json");
        } catch (Exception e) {
            System.err.println("Failed to migrate legacy properties: " + e.getMessage());
        }
    }

    /**
     * Extracts Google Drive folder ID from raw ID or URLs.
     */
    public static String extractDriveFolderId(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        var trimmed = input.strip();

        var folderMatcher = FOLDER_PATTERN.matcher(trimmed);
        if (folderMatcher.find()) return folderMatcher.group(1);

        var idMatcher = ID_PARAM_PATTERN.matcher(trimmed);
        if (idMatcher.find()) return idMatcher.group(1);

        var dMatcher = D_PATTERN.matcher(trimmed);
        if (dMatcher.find()) return dMatcher.group(1);

        int queryIdx = trimmed.indexOf('?');
        if (queryIdx != -1) {
            trimmed = trimmed.substring(0, queryIdx);
        }

        return trimmed.strip();
    }
}
