package com.masquerade817;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppConfigTest {

    @Test
    public void testExtractDriveFolderIdFromRawId() {
        var id = "1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh";
        assertEquals("1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh", AppConfig.extractDriveFolderId(id));
    }

    @Test
    public void testExtractDriveFolderIdFromStandardUrl() {
        var url = "https://drive.google.com/drive/folders/1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh";
        assertEquals("1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh", AppConfig.extractDriveFolderId(url));
    }

    @Test
    public void testExtractDriveFolderIdFromShareUrlWithParams() {
        var url = "https://drive.google.com/drive/u/0/folders/1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh?usp=sharing";
        assertEquals("1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh", AppConfig.extractDriveFolderId(url));
    }

    @Test
    public void testExtractDriveFolderIdFromOpenParamUrl() {
        var url = "https://drive.google.com/open?id=1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh&authuser=0";
        assertEquals("1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh", AppConfig.extractDriveFolderId(url));
    }

    @Test
    public void testExtractDriveFolderIdEmptyOrNull() {
        assertEquals("", AppConfig.extractDriveFolderId(null));
        assertEquals("", AppConfig.extractDriveFolderId("   "));
    }

    @Test
    public void testConfigDefaultsUsingRecord() {
        var config = new AppConfig();
        var data = config.data();
        assertNotNull(data);
        assertTrue(data.frequencyMinutes() > 0);
        assertTrue(data.deleteUnsetWallpaper());
        assertNotNull(data.localFolderPath());
    }

    @Test
    public void testRecordImmutabilityAndWithMethods() {
        var data = AppConfig.ConfigData.defaultSettings();
        var updated = data.withLastWallpaper("C:\\test\\wall.jpg", "drive123");

        assertEquals("C:\\test\\wall.jpg", updated.lastWallpaperPath());
        assertEquals("drive123", updated.lastWallpaperDriveId());
        assertEquals(data.frequencyMinutes(), updated.frequencyMinutes());
    }

    @Test
    public void testSubfolderSettingsDefaultsAndWithMethods() {
        var data = AppConfig.ConfigData.defaultSettings();
        assertFalse(data.includeSubfolders());
        assertEquals(3, data.subfolderDepth());

        var updated = data.withSubfolders(true, 5);
        assertTrue(updated.includeSubfolders());
        assertEquals(5, updated.subfolderDepth());
    }

    @Test
    public void testSubfolderDepthSanitization() {
        var data = new AppConfig.ConfigData(
                "folder", "C:\\path", 30, true, true, "", "", "", "2", true, 0
        );
        assertEquals(3, data.subfolderDepth());

        var negative = new AppConfig.ConfigData(
                "folder", "C:\\path", 30, true, true, "", "", "", "2", true, -10
        );
        assertEquals(3, negative.subfolderDepth());
    }

    @Test
    public void testBackwardCompatibilityDeserializationWithoutSubfolderFields() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String jsonWithoutSubfolderKeys = """
                {
                    "driveFolderId": "folder123",
                    "localFolderPath": "/local/path",
                    "frequencyMinutes": 15,
                    "deleteUnsetWallpaper": true,
                    "startOnStartup": false,
                    "wallpaperStyle": "6"
                }
                """;
        var parsed = mapper.readValue(jsonWithoutSubfolderKeys, AppConfig.ConfigData.class);
        assertEquals("folder123", parsed.driveFolderId());
        assertFalse(parsed.includeSubfolders());
        assertEquals(3, parsed.subfolderDepth());
    }
}
