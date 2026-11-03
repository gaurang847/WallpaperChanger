package com.masquerade817;

import com.masquerade817.DriveConnector.WallpaperItem;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DriveConnectorTest {

    private DriveConnector connector;

    @Before
    public void setUp() {
        connector = new DriveConnector();
    }

    @Test
    public void testSanitizeFileName() {
        assertEquals("wallpaper_test.jpg", DriveConnector.sanitizeFileName("wallpaper:test.jpg"));
        assertEquals("my_image_file.png", DriveConnector.sanitizeFileName("my/image\\file.png"));
    }

    @Test
    public void testIsImageFileByMimeType() {
        assertTrue(connector.isImageFile("document", "image/jpeg"));
        assertTrue(connector.isImageFile("vector", "image/png"));
        assertFalse(connector.isImageFile("readme.txt", "text/plain"));
    }

    @Test
    public void testIsImageFileByExtension() {
        assertTrue(connector.isImageFile("photo.JPG", "application/octet-stream"));
        assertTrue(connector.isImageFile("artwork.png", null));
        assertFalse(connector.isImageFile("archive.zip", null));
    }

    @Test
    public void testSelectRandomWallpaperExcludesCurrentWhenMultiple() {
        var files = List.of(
                new WallpaperItem("id1", "img1.jpg", "image/jpeg"),
                new WallpaperItem("id2", "img2.jpg", "image/jpeg")
        );

        // When excluding id1, should always pick id2
        var selected = connector.selectRandomWallpaper(files, "id1");
        assertNotNull(selected);
        assertEquals("id2", selected.id());
    }

    @Test
    public void testSelectRandomWallpaperSingleFile() {
        var files = List.of(
                new WallpaperItem("id1", "img1.jpg", "image/jpeg")
        );

        var selected = connector.selectRandomWallpaper(files, "id1");
        assertEquals("id1", selected.id());
    }
}
