package com.gaurang847;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.drive.Drive;
import com.gaurang847.DriveConnector.WallpaperItem;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class DriveConnectorTest {

    private DriveConnector connector;

    @Before
    public void setUp() {
        connector = new DriveConnector();
    }

    private Drive createMockDrive(Map<String, String> folderResponses) {
        var transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() throws IOException {
                        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
                        var matcher = Pattern.compile("'([^']+)' in parents").matcher(decoded);
                        String folderId = matcher.find() ? matcher.group(1) : "";

                        String json = folderResponses.getOrDefault(folderId, "{\"files\": []}");
                        if ("__ERROR__".equals(json)) {
                            throw new IOException("Simulated 403 Forbidden for subfolder " + folderId);
                        }

                        var response = new MockLowLevelHttpResponse();
                        response.setContentType("application/json");
                        response.setContent(json);
                        return response;
                    }
                };
            }
        };

        return new Drive.Builder(transport, GsonFactory.getDefaultInstance(), null)
                .setApplicationName("DriveConnectorTest")
                .build();
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
        assertFalse(connector.isImageFile("subfolder_name", "application/vnd.google-apps.folder"));
    }

    @Test
    public void testIsImageFileByExtension() {
        assertTrue(connector.isImageFile("photo.JPG", "application/octet-stream"));
        assertTrue(connector.isImageFile("artwork.png", null));
        assertFalse(connector.isImageFile("archive.zip", null));
        assertFalse(connector.isImageFile("document.pdf", "application/pdf"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetImagesFromDriveFolderThrowsOnBlankId() throws Exception {
        connector.getImagesFromDriveFolder("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCountImagesInFolderThrowsOnNullId() throws Exception {
        connector.countImagesInFolder(null, true, 3);
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

        var selectedWithBoth = connector.selectRandomWallpaper(files, "id1", "img1.jpg");
        assertEquals("id1", selectedWithBoth.id());
    }

    @Test
    public void testSelectRandomWallpaperExcludesCurrentByNameWhenMultiple() {
        var files = List.of(
                new WallpaperItem("id1", "same_name.jpg", "image/jpeg"),
                new WallpaperItem("id2", "same_name.jpg", "image/jpeg"),
                new WallpaperItem("id3", "distinct.jpg", "image/jpeg")
        );

        // When excluding same_name.jpg, should pick distinct.jpg
        var selected = connector.selectRandomWallpaper(files, "id1", "same_name.jpg");
        assertNotNull(selected);
        assertEquals("id3", selected.id());
    }

    @Test
    public void testTenFoldersOneHasImagesRestEmptyOrNonImage() throws Exception {
        var responses = new HashMap<String, String>();

        // Root folder has 10 subfolders
        responses.put("root", """
                {
                    "files": [
                        {"id": "sub1", "name": "folder1", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub2", "name": "folder2", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub3", "name": "folder3", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub4", "name": "folder4", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub5", "name": "folder5", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub6", "name": "folder6", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub7", "name": "folder7", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub8", "name": "folder8", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub9", "name": "folder9", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub10", "name": "folder10", "mimeType": "application/vnd.google-apps.folder"}
                    ]
                }
                """);

        // sub2 has only documents/PDFs (non-images)
        responses.put("sub2", """
                {
                    "files": [
                        {"id": "doc1", "name": "report.pdf", "mimeType": "application/pdf"},
                        {"id": "doc2", "name": "budget.xlsx", "mimeType": "application/vnd.ms-excel"}
                    ]
                }
                """);

        // sub5 has 3 valid wallpapers
        responses.put("sub5", """
                {
                    "files": [
                        {"id": "img1", "name": "mountain.jpg", "mimeType": "image/jpeg"},
                        {"id": "img2", "name": "sunset.png", "mimeType": "image/png"},
                        {"id": "img3", "name": "cyberpunk.webp", "mimeType": "image/webp"}
                    ]
                }
                """);

        // sub1, sub3, sub4, sub6..sub10 are completely empty
        responses.put("sub1", "{\"files\": []}");
        responses.put("sub3", "{\"files\": []}");
        responses.put("sub4", "{\"files\": []}");
        responses.put("sub6", "{\"files\": []}");
        responses.put("sub7", "{\"files\": []}");
        responses.put("sub8", "{\"files\": []}");
        responses.put("sub9", "{\"files\": []}");
        responses.put("sub10", "{\"files\": []}");

        var mockDrive = createMockDrive(responses);
        var mockConnector = new DriveConnector(mockDrive);

        // Subfolders enabled
        var images = mockConnector.getImagesFromDriveFolder("root", true, 3);
        assertNotNull(images);
        assertEquals(3, images.size());

        var names = images.stream().map(WallpaperItem::name).toList();
        assertTrue(names.contains("mountain.jpg"));
        assertTrue(names.contains("sunset.png"));
        assertTrue(names.contains("cyberpunk.webp"));

        // Count should also equal 3
        int count = mockConnector.countImagesInFolder("root", true, 3);
        assertEquals(3, count);
    }

    @Test
    public void testTenFoldersWithSubfoldersDisabledReturnsZero() throws Exception {
        var responses = new HashMap<String, String>();
        responses.put("root", """
                {
                    "files": [
                        {"id": "sub1", "name": "folder1", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub2", "name": "folder2", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "sub5", "name": "folder5", "mimeType": "application/vnd.google-apps.folder"}
                    ]
                }
                """);
        responses.put("sub5", """
                {
                    "files": [
                        {"id": "img1", "name": "mountain.jpg", "mimeType": "image/jpeg"}
                    ]
                }
                """);

        var mockDrive = createMockDrive(responses);
        var mockConnector = new DriveConnector(mockDrive);

        // When includeSubfolders = false, only root is checked (which has 0 direct images)
        var images = mockConnector.getImagesFromDriveFolder("root", false, 3);
        assertNotNull(images);
        assertEquals(0, images.size());

        int count = mockConnector.countImagesInFolder("root", false, 3);
        assertEquals(0, count);
    }

    @Test
    public void testDepthLimitingInNestedFolders() throws Exception {
        var responses = new HashMap<String, String>();
        // root (0) -> subLevel1 (1) -> subLevel2 (2) -> subLevel3 (3, contains deep.png)
        responses.put("root", """
                {"files": [{"id": "lvl1", "name": "subLevel1", "mimeType": "application/vnd.google-apps.folder"}]}
                """);
        responses.put("lvl1", """
                {"files": [{"id": "lvl2", "name": "subLevel2", "mimeType": "application/vnd.google-apps.folder"}]}
                """);
        responses.put("lvl2", """
                {"files": [{"id": "lvl3", "name": "subLevel3", "mimeType": "application/vnd.google-apps.folder"}]}
                """);
        responses.put("lvl3", """
                {"files": [{"id": "deep_img", "name": "deep.png", "mimeType": "image/png"}]}
                """);

        var mockDrive = createMockDrive(responses);
        var mockConnector = new DriveConnector(mockDrive);

        // Depth 2: Visits root (0), lvl1 (1), lvl2 (2). Does NOT explore lvl3 (3).
        var depth2Images = mockConnector.getImagesFromDriveFolder("root", true, 2);
        assertEquals(0, depth2Images.size());

        // Depth 3: Explores lvl3, finds deep.png
        var depth3Images = mockConnector.getImagesFromDriveFolder("root", true, 3);
        assertEquals(1, depth3Images.size());
        assertEquals("deep.png", depth3Images.getFirst().name());
    }

    @Test
    public void testCyclicFolderShortcutsDoNotCauseInfiniteLoop() throws Exception {
        var responses = new HashMap<String, String>();
        // root contains folderA; folderA contains folderB and points back to root;
        // folderB points back to folderA and contains an image
        responses.put("root", """
                {"files": [{"id": "folderA", "name": "A", "mimeType": "application/vnd.google-apps.folder"}]}
                """);
        responses.put("folderA", """
                {
                    "files": [
                        {"id": "folderB", "name": "B", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "root", "name": "RootShortcut", "mimeType": "application/vnd.google-apps.folder"}
                    ]
                }
                """);
        responses.put("folderB", """
                {
                    "files": [
                        {"id": "folderA", "name": "AShortcut", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "pic", "name": "photo.jpg", "mimeType": "image/jpeg"}
                    ]
                }
                """);

        var mockDrive = createMockDrive(responses);
        var mockConnector = new DriveConnector(mockDrive);

        // BFS with visited set must terminate and find the image
        var images = mockConnector.getImagesFromDriveFolder("root", true, 5);
        assertEquals(1, images.size());
        assertEquals("photo.jpg", images.getFirst().name());
    }

    @Test
    public void testSubfolderPermissionErrorDoesNotFailEntireSync() throws Exception {
        var responses = new HashMap<String, String>();
        responses.put("root", """
                {
                    "files": [
                        {"id": "forbidden_sub", "name": "PrivateFolder", "mimeType": "application/vnd.google-apps.folder"},
                        {"id": "public_sub", "name": "PublicFolder", "mimeType": "application/vnd.google-apps.folder"}
                    ]
                }
                """);
        responses.put("forbidden_sub", "__ERROR__");
        responses.put("public_sub", """
                {"files": [{"id": "pic", "name": "public_wallpaper.jpg", "mimeType": "image/jpeg"}]}
                """);

        var mockDrive = createMockDrive(responses);
        var mockConnector = new DriveConnector(mockDrive);

        // Even though forbidden_sub throws IOException, public_sub is still read
        var images = mockConnector.getImagesFromDriveFolder("root", true, 3);
        assertEquals(1, images.size());
        assertEquals("public_wallpaper.jpg", images.getFirst().name());
    }

    @Test
    public void testCompletelyEmptyRootFolderReturnsEmptyList() throws Exception {
        var responses = new HashMap<String, String>();
        responses.put("empty_root", "{\"files\": []}");

        var mockDrive = createMockDrive(responses);
        var mockConnector = new DriveConnector(mockDrive);

        var images = mockConnector.getImagesFromDriveFolder("empty_root", true, 3);
        assertNotNull(images);
        assertTrue(images.isEmpty());
    }
}
