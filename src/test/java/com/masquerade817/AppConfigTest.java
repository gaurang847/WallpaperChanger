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
}
