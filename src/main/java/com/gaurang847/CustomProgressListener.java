package com.gaurang847;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;

public class CustomProgressListener implements MediaHttpDownloaderProgressListener {

    public interface DownloadCallback {
        void onProgress(double progress);
        void onComplete();
    }

    private final DownloadCallback callback;

    public CustomProgressListener() {
        this(null);
    }

    public CustomProgressListener(DownloadCallback callback) {
        this.callback = callback;
    }

    @Override
    public void progressChanged(MediaHttpDownloader downloader) {
        switch (downloader.getDownloadState()) {
            case MEDIA_IN_PROGRESS -> {
                double progress = downloader.getProgress();
                System.out.println("Download Progress: %.1f%%".formatted(progress * 100));
                if (callback != null) {
                    callback.onProgress(progress);
                }
            }
            case MEDIA_COMPLETE -> {
                System.out.println("Download complete!");
                if (callback != null) {
                    callback.onComplete();
                }
            }
            default -> {}
        }
    }
}
