package com.masquerade817;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

public class DriveAPI {
    private static final String APPLICATION_NAME = "Wallpaper Changer";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final Path APP_DIR = Path.of(System.getProperty("user.home"), ".wallpaperchanger");
    private static final Path TOKENS_PATH = APP_DIR.resolve("tokens");
    private static final Path DEFAULT_USER_CREDENTIALS_PATH = APP_DIR.resolve("credentials.json");
    private static final String DEFAULT_CREDENTIALS_RESOURCE = "/credentials.json";

    private static final List<String> SCOPES = List.of(DriveScopes.DRIVE_READONLY);

    private static Drive cachedDriveService = null;

    /**
     * Checks if the active credentials file is a Google Service Account JSON.
     */
    public static boolean isServiceAccount() {
        if (!hasCredentialsFile()) return false;
        try (var in = getCredentialsInputStream()) {
            var mapper = new ObjectMapper();
            var node = mapper.readTree(in);
            return node != null && node.has("type") && "service_account".equalsIgnoreCase(node.get("type").asText());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieves the Service Account email address if active.
     */
    public static String getServiceAccountEmail() {
        if (!hasCredentialsFile()) return null;
        try (var in = getCredentialsInputStream()) {
            var mapper = new ObjectMapper();
            var node = mapper.readTree(in);
            if (node != null && node.has("client_email")) {
                return node.get("client_email").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Checks if valid stored credentials or OAuth tokens exist.
     * Service Accounts are always authorized once the key file exists.
     */
    public static synchronized boolean isAuthorized() {
        if (isServiceAccount()) {
            return true;
        }
        if (!Files.exists(TOKENS_PATH) || !Files.isDirectory(TOKENS_PATH)) {
            return false;
        }
        try (var stream = Files.list(TOKENS_PATH)) {
            return stream.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks whether a credentials file exists in any recognized location.
     */
    public static boolean hasCredentialsFile() {
        var customPath = AppConfig.getInstance().data().customCredentialsPath();
        if (customPath != null && !customPath.isBlank()) {
            if (Files.isRegularFile(Path.of(customPath.strip()))) {
                return true;
            }
        }

        if (Files.isRegularFile(DEFAULT_USER_CREDENTIALS_PATH)) return true;
        if (Files.isRegularFile(Path.of("credentials.json"))) return true;

        return DriveAPI.class.getResourceAsStream(DEFAULT_CREDENTIALS_RESOURCE) != null;
    }

    public static Path getDefaultCredentialsPath() {
        return DEFAULT_USER_CREDENTIALS_PATH;
    }

    /**
     * Signs out by resetting the client and deleting any stored tokens.
     */
    public static synchronized void signOut() throws IOException {
        cachedDriveService = null;
        if (Files.exists(TOKENS_PATH)) {
            FileUtils.deleteDirectory(TOKENS_PATH.toFile());
        }
    }

    /**
     * Opens credentials input stream from configured, user-level, or classpath sources.
     */
    private static InputStream getCredentialsInputStream() throws IOException {
        var config = AppConfig.getInstance();
        var customPath = config.data().customCredentialsPath();

        // 1. User-configured custom path
        if (customPath != null && !customPath.isBlank()) {
            var customFile = Path.of(customPath.strip());
            if (Files.isRegularFile(customFile)) {
                return Files.newInputStream(customFile);
            }
        }

        // 2. User directory ~/.wallpaperchanger/credentials.json
        if (Files.isRegularFile(DEFAULT_USER_CREDENTIALS_PATH)) {
            return Files.newInputStream(DEFAULT_USER_CREDENTIALS_PATH);
        }

        // 3. Current working directory ./credentials.json
        var localFile = Path.of("credentials.json");
        if (Files.isRegularFile(localFile)) {
            return Files.newInputStream(localFile);
        }

        // 4. Bundled resource (if bundled)
        var resourceIn = DriveAPI.class.getResourceAsStream(DEFAULT_CREDENTIALS_RESOURCE);
        if (resourceIn != null) {
            return resourceIn;
        }

        throw new FileNotFoundException("""
                Google credentials JSON not found!
                Please download your Service Account Key or OAuth Desktop credentials,
                and select it in the Wallpaper Changer Settings window.
                """);
    }

    /**
     * Creates an authorized HttpRequestInitializer.
     * Supports both Service Accounts (direct key) and OAuth 2.0 Installed Apps.
     */
    @SuppressWarnings("deprecation")
    private static HttpRequestInitializer getCredentials(final NetHttpTransport httpTransport) throws IOException {
        try (var in = getCredentialsInputStream()) {
            if (isServiceAccount()) {
                return GoogleCredential.fromStream(in, httpTransport, JSON_FACTORY)
                        .createScoped(SCOPES);
            }

            var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            if (!Files.exists(TOKENS_PATH)) {
                Files.createDirectories(TOKENS_PATH);
            }

            var flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(TOKENS_PATH.toFile()))
                    .setAccessType("offline")
                    .build();

            var receiver = new LocalServerReceiver();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }

    /**
     * Returns an authorized Google Drive service instance.
     */
    public static synchronized Drive getDriveService() throws IOException, GeneralSecurityException {
        if (cachedDriveService == null) {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var credential = getCredentials(httpTransport);
            cachedDriveService = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        return cachedDriveService;
    }

    /**
     * Explicitly forces authentication (used by Settings GUI button).
     */
    public static synchronized Drive authenticate() throws IOException, GeneralSecurityException {
        cachedDriveService = null;
        return getDriveService();
    }
}
