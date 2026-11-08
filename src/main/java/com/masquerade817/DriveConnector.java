package com.masquerade817;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class DriveConnector {

    /**
     * Java 21 Record representing a Google Drive wallpaper item.
     */
    public record WallpaperItem(String id, String name, String mimeType) {}

    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".bmp", ".webp"
    );

    private Drive driveService;

    public DriveConnector() {
    }

    private Drive getService() throws IOException, GeneralSecurityException {
        if (driveService == null) {
            driveService = DriveAPI.getDriveService();
        }
        return driveService;
    }

    /**
     * Retrieves all valid image files contained in the specified Google Drive folder.
     * Searches root folder only.
     */
    public List<WallpaperItem> getImagesFromDriveFolder(String folderId) throws IOException, GeneralSecurityException {
        return getImagesFromDriveFolder(folderId, false, 1);
    }

    /**
     * Retrieves valid image files from Google Drive, optionally traversing nested subfolders
     * up to a specified maximum depth using Breadth-First Search (BFS).
     */
    public List<WallpaperItem> getImagesFromDriveFolder(String folderId, boolean includeSubfolders, int maxDepth)
            throws IOException, GeneralSecurityException {
        if (folderId == null || folderId.isBlank()) {
            throw new IllegalArgumentException("Google Drive folder ID cannot be empty.");
        }

        var cleanFolderId = AppConfig.extractDriveFolderId(folderId);
        var service = getService();

        var imageFiles = new ArrayList<WallpaperItem>();
        int effectiveMaxDepth = Math.max(1, maxDepth);

        record FolderVisit(String id, int depth) {}
        Queue<FolderVisit> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        queue.add(new FolderVisit(cleanFolderId, 0));
        visited.add(cleanFolderId);

        while (!queue.isEmpty()) {
            var current = queue.poll();
            String pageToken = null;
            var query = "'%s' in parents and trashed = false".formatted(current.id());

            try {
                do {
                    FileList result = service.files().list()
                            .setQ(query)
                            .setPageSize(100)
                            .setFields("nextPageToken, files(id, name, mimeType)")
                            .setPageToken(pageToken)
                            .execute();

                    List<File> files = result.getFiles();
                    if (files != null) {
                        for (var file : files) {
                            var mime = file.getMimeType();
                            var id = file.getId();
                            var name = file.getName();

                            if ("application/vnd.google-apps.folder".equals(mime)) {
                                if (includeSubfolders && current.depth() < effectiveMaxDepth) {
                                    if (visited.add(id)) {
                                        queue.add(new FolderVisit(id, current.depth() + 1));
                                    }
                                }
                            } else if (isImageFile(name, mime)) {
                                imageFiles.add(new WallpaperItem(id, name, mime));
                            }
                        }
                    }
                    pageToken = result.getNextPageToken();
                } while (pageToken != null);
            } catch (IOException e) {
                // If the root folder fails, rethrow so the user gets notified of error.
                // If an internal subfolder fails (e.g. permission restriction), log and continue.
                if (current.depth() == 0) {
                    throw e;
                } else {
                    System.err.println("[DriveConnector] Warning: Could not list subfolder " + current.id() + ": " + e.getMessage());
                }
            }
        }

        return List.copyOf(imageFiles);
    }

    /**
     * Checks if a file is an image based on its MIME type or file extension.
     */
    public boolean isImageFile(String fileName, String mimeType) {
        if (fileName == null) {
            return false;
        }

        if (mimeType != null && mimeType.toLowerCase().startsWith("image/")) {
            return true;
        }

        var lowerName = fileName.toLowerCase();
        return SUPPORTED_IMAGE_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
    }

    /**
     * Selects a random image file from the list.
     * Uses Java 21 Sequenced Collections (getFirst()).
     */
    public WallpaperItem selectRandomWallpaper(List<WallpaperItem> files, String excludeDriveId) {
        if (files == null || files.isEmpty()) {
            throw new IllegalStateException("No wallpapers found in the selected Google Drive folder.");
        }

        if (files.size() == 1) {
            return files.getFirst();
        }

        List<WallpaperItem> candidates = files;
        if (excludeDriveId != null && !excludeDriveId.isBlank()) {
            var filtered = files.stream()
                    .filter(item -> !excludeDriveId.equals(item.id()))
                    .toList();
            if (!filtered.isEmpty()) {
                candidates = filtered;
            }
        }

        var rand = new Random();
        int index = rand.nextInt(candidates.size());
        return candidates.get(index);
    }

    /**
     * Sanitizes a file name for safe storage on Windows file systems.
     */
    public static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "wallpaper_%d.jpg".formatted(System.currentTimeMillis());
        }
        var clean = name.replaceAll("[\\\\/:*?\"<>|]", "_").strip();
        return clean.isBlank() ? "wallpaper_%d.jpg".formatted(System.currentTimeMillis()) : clean;
    }

    /**
     * Downloads an image file from Google Drive into the target directory.
     * Uses modern NIO Path and Files.
     */
    public Path downloadWallpaper(WallpaperItem item, Path targetDirectory,
                                 CustomProgressListener.DownloadCallback callback)
            throws IOException, GeneralSecurityException {

        if (!Files.exists(targetDirectory)) {
            Files.createDirectories(targetDirectory);
        }

        var service = getService();
        var safeName = sanitizeFileName(item.name());
        var destinationPath = targetDirectory.resolve(safeName);

        var request = service.files().get(item.id());
        request.getMediaHttpDownloader()
                .setProgressListener(new CustomProgressListener(callback))
                .setDirectDownloadEnabled(true);

        try (var buffer = new ByteArrayOutputStream()) {
            request.executeMediaAndDownloadTo(buffer);
            try (var out = Files.newOutputStream(destinationPath)) {
                buffer.writeTo(out);
            }
        }

        return destinationPath;
    }

    /**
     * Tests connection to the Drive folder and returns the count of image files found (root only).
     */
    public int countImagesInFolder(String folderId) throws IOException, GeneralSecurityException {
        return countImagesInFolder(folderId, false, 1);
    }

    /**
     * Tests connection to the Drive folder and returns the count of image files found,
     * optionally including subfolders up to maxDepth.
     */
    public int countImagesInFolder(String folderId, boolean includeSubfolders, int maxDepth)
            throws IOException, GeneralSecurityException {
        return getImagesFromDriveFolder(folderId, includeSubfolders, maxDepth).size();
    }
}
