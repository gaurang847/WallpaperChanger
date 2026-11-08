package com.masquerade817;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.drive.Drive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class WallpaperSyncServiceTest {

    private Path tempLocalDir;
    private AppConfig config;

    @Before
    public void setUp() throws IOException {
        tempLocalDir = Files.createTempDirectory("wp_test_local_");
        config = AppConfig.getInstance();
    }

    @After
    public void tearDown() throws IOException {
        if (tempLocalDir != null && Files.exists(tempLocalDir)) {
            try (var stream = Files.walk(tempLocalDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {}
                        });
            }
        }
    }

    private Drive createMockDrive(Map<String, String> responses) {
        var transport = new MockHttpTransport() {
            @Override
            public LowLevelHttpRequest buildRequest(String method, String url) {
                return new MockLowLevelHttpRequest() {
                    @Override
                    public LowLevelHttpResponse execute() {
                        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
                        var matcher = Pattern.compile("'([^']+)' in parents").matcher(decoded);
                        String folderId = matcher.find() ? matcher.group(1) : "";

                        String json = responses.getOrDefault(folderId, "{\"files\": []}");
                        var response = new MockLowLevelHttpResponse();
                        response.setContentType("application/json");
                        if (url.contains("alt=media")) {
                            response.setContent("fake-image-bytes");
                        } else {
                            response.setContent(json);
                        }
                        return response;
                    }
                };
            }
        };

        return new Drive.Builder(transport, GsonFactory.getDefaultInstance(), null)
                .setApplicationName("WallpaperSyncServiceTest")
                .build();
    }

    @Test
    public void testSyncFailsGracefullyWhenDriveFolderHasNoImages() throws IOException {
        // Place an existing wallpaper on disk
        var existingWallpaper = tempLocalDir.resolve("my_current_wallpaper.jpg");
        Files.writeString(existingWallpaper, "initial wallpaper content");

        // Set config to empty drive folder
        config.update(new AppConfig.ConfigData(
                "empty_root_id",
                tempLocalDir.toAbsolutePath().toString(),
                60,
                true,
                false,
                "",
                existingWallpaper.toAbsolutePath().toString(),
                "drive_old_id",
                "2",
                true,
                3
        ));

        var responses = new HashMap<String, String>();
        responses.put("empty_root_id", "{\"files\": []}");

        var mockDrive = createMockDrive(responses);
        var connector = new DriveConnector(mockDrive);
        var syncService = new WallpaperSyncService(config, connector, new WallpaperSetter());

        var syncFailedCalled = new AtomicBoolean(false);
        var failureReason = new AtomicReference<String>("");

        syncService.addListener(new WallpaperSyncService.SyncListener() {
            @Override
            public void onSyncStarted() {}

            @Override
            public void onProgress(String status) {}

            @Override
            public void onSyncSuccess(String wallpaperName, Path localPath) {}

            @Override
            public void onSyncFailed(String reason, Throwable error) {
                syncFailedCalled.set(true);
                failureReason.set(reason);
            }

            @Override
            public void onScheduleUpdated(java.util.Date nextRunTime) {}
        });

        boolean result = syncService.performSyncAndChange();

        // 1. Result must be false
        assertFalse(result);

        // 2. Notification must explain no images found
        assertTrue(syncFailedCalled.get());
        assertTrue(failureReason.get().contains("No wallpaper images found"));

        // 3. Existing local wallpaper must NOT be deleted!
        assertTrue(Files.exists(existingWallpaper));
        assertEquals("initial wallpaper content", Files.readString(existingWallpaper));
    }

    @Test
    public void testSyncSucceedsWhenTenFoldersOneHasImages() throws IOException {
        var responses = new HashMap<String, String>();
        // Root has 10 subfolders
        var rootSubfolders = new ArrayList<String>();
        for (int i = 1; i <= 10; i++) {
            rootSubfolders.add("{\"id\": \"folder" + i + "\", \"name\": \"Sub" + i + "\", \"mimeType\": \"application/vnd.google-apps.folder\"}");
        }
        responses.put("root_10", "{\"files\": [" + String.join(",", rootSubfolders) + "]}");

        // Only folder5 has an image
        responses.put("folder5", """
                {"files": [{"id": "img5", "name": "found_wallpaper.jpg", "mimeType": "image/jpeg"}]}
                """);

        // All other folders are empty
        for (int i = 1; i <= 10; i++) {
            if (i != 5) {
                responses.put("folder" + i, "{\"files\": []}");
            }
        }

        config.update(new AppConfig.ConfigData(
                "root_10",
                tempLocalDir.toAbsolutePath().toString(),
                60,
                true,
                false,
                "",
                "",
                "",
                "2",
                true,
                3
        ));

        var mockDrive = createMockDrive(responses);
        var connector = new DriveConnector(mockDrive);
        var syncService = new WallpaperSyncService(config, connector, new WallpaperSetter());

        var syncSuccessCalled = new AtomicBoolean(false);
        var successWallpaper = new AtomicReference<String>("");

        syncService.addListener(new WallpaperSyncService.SyncListener() {
            @Override
            public void onSyncStarted() {}

            @Override
            public void onProgress(String status) {}

            @Override
            public void onSyncSuccess(String wallpaperName, Path localPath) {
                syncSuccessCalled.set(true);
                successWallpaper.set(wallpaperName);
            }

            @Override
            public void onSyncFailed(String reason, Throwable error) {}

            @Override
            public void onScheduleUpdated(java.util.Date nextRunTime) {}
        });

        boolean result = syncService.performSyncAndChange();

        assertTrue(result);
        assertTrue(syncSuccessCalled.get());
        assertEquals("found_wallpaper.jpg", successWallpaper.get());
        assertTrue(Files.exists(tempLocalDir.resolve("found_wallpaper.jpg")));
    }
}
