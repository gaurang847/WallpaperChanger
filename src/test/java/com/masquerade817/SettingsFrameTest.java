package com.masquerade817;

import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsFrameTest {

    @Test
    public void testTruncateShortFileName() {
        String shortName = "sunset.jpg";
        assertEquals("sunset.jpg", SettingsFrame.truncateFileName(shortName, 40));
    }

    @Test
    public void testTruncateLongFileNamePreservesExtension() {
        String longName = "extremely_long_wallpaper_file_name_from_google_drive_4k_resolution.jpeg";
        String truncated = SettingsFrame.truncateFileName(longName, 35);

        assertTrue(truncated.length() <= 35);
        assertTrue(truncated.contains("..."));
        assertTrue(truncated.endsWith(".jpeg"));
    }

    @Test
    public void testTruncateNullOrEmpty() {
        assertEquals("", SettingsFrame.truncateFileName("", 20));
        assertEquals("", SettingsFrame.truncateFileName(null, 20));
    }

    @Test
    public void testTruncateExactLength() {
        String exact = "1234567890.png";
        assertEquals(exact, SettingsFrame.truncateFileName(exact, exact.length()));
    }
}
