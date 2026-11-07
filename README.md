# WallpaperChanger

**WallpaperChanger** is a lightweight Windows background application that keeps your desktop wallpaper synchronized with a centralized Google Drive folder. It periodically downloads a random wallpaper from your Drive folder, applies it as your Windows desktop background, and cleans up old wallpapers once they have been unset so that your local drive stays uncluttered.

---

## Key Features

1. **Centralized Google Drive Storage**:
   - Maintain a single Google Drive folder of wallpapers that can be shared across any of your Windows machines.
   - Supports pasting either a Google Drive Folder ID or a full shareable URL (e.g. `https://drive.google.com/drive/folders/...`).
   - Automatically filters for valid images (`.jpg`, `.jpeg`, `.png`, `.bmp`, `.webp`) and excludes trashed files.
   - Paginates through large folders to discover all images.

2. **Clean Local Storage (Unset Deletion)**:
   - Only the currently active wallpaper is stored locally.
   - As soon as a new wallpaper is selected and set, the previously unset wallpaper file is automatically removed from your local disk.
   - Prevents gigabytes of wallpaper files from accumulating on your computer.

3. **Background Execution & System Tray**:
   - Runs silently in the background with a System Tray icon.
   - Context menu options:
     - **Change Wallpaper Now**: Immediately fetch and set a new wallpaper.
     - **Pause / Resume Auto-Change**: Temporarily pause automatic rotations.
     - **Open Wallpaper Folder**: Quick access to the local folder in Windows File Explorer.
     - **Settings...**: Open the configuration interface.
     - **Exit**: Cleanly shut down the application.
   - Balloon notifications alert you when wallpapers update or if attention is needed.

4. **Modern Configuration Interface**:
   - One-click Google OAuth authentication via your default browser.
   - Test folder connection button that counts how many wallpaper images exist.
   - Choose local destination folder with built-in folder browser.
   - Change frequency presets (5m, 15m, 30m, 1h, 2h, 6h, 12h, 24h, or custom minutes).
   - Wallpaper styling selector (Fill/Stretch, Fit, Center, Tile, Span).
   - Toggle options for startup launch and unset deletion.

5. **Windows Startup Integration**:
   - Starts automatically with Windows in background mode (`--background`).
   - Uses native Windows Registry (`HKCU\Software\Microsoft\Windows\CurrentVersion\Run`) via JNA without requiring Administrator privileges.
   - Toggleable from the Settings GUI checkbox or via included scripts.

6. **Converted to Maven**:
   - Fully converted from Gradle to Maven (`pom.xml`).
   - Packages into a single executable fat JAR (`target/WallpaperChanger-1.0.0.jar`) with all dependencies bundled.

7. **Built on Java 21 LTS & Modern Libraries**:
   - **Virtual Threads** (`Thread.ofVirtual()`): Background image downloads and Drive API calls run on lightweight virtual threads without blocking operating system threads.
   - **FlatLaf Modern UI**: Clean, rounded, high-DPI modern Swing theme.
   - **Records**: Boilerplate-free immutable data representations.
   - **Jackson JSON Configuration**: Settings persisted in clean, human-readable `config.json`.
   - **Modern Java NIO.2**: Robust file operations using `java.nio.file.Path` and `Files`.

---

## Requirements

- **Operating System**: Windows 10 / 11 / 8 / 7 (64-bit or 32-bit)
- **Java**: Java Development Kit (JDK) 21 or higher
- **Maven**: Apache Maven 3.6+ (to build the project)

---

## Build Instructions

To build the executable standalone JAR:

```bash
mvn clean package
```

The output JAR will be placed at:
```
target/WallpaperChanger-1.0.0.jar
```

---

## Installation & Running

### Option 1: One-Click Automatic Install (Recommended)
Double-click [`install.bat`](file:///home/gaurang/projects/WallpaperChanger/install.bat). It will:
1. Automatically build the JAR using Maven (if not yet built).
2. Install the app to `%LOCALAPPDATA%\WallpaperChanger\`.
3. Add a shortcut to your Windows **Start Menu** (`Wallpaper Changer`).
4. Register the app in Windows Startup (starts silently in the background on every boot).
5. Launch the Settings window immediately so you can paste your Drive link and sign in.

*(To completely remove the app later, simply double-click [`uninstall.bat`](file:///home/gaurang/projects/WallpaperChanger/uninstall.bat)).*

### Option 2: Run in-place (No installation needed)
You do **not** need to copy the JAR anywhere. Simply double-click [`run.bat`](file:///home/gaurang/projects/WallpaperChanger/run.bat):
- It automatically checks for `target\WallpaperChanger-1.0.0.jar` (and runs `mvn clean package` if needed), then starts the app directly.

### Option 3: Silent Background Launch (Tray Only)
Double-click [`run-background.vbs`](file:///home/gaurang/projects/WallpaperChanger/run-background.vbs) or execute:
```cmd
javaw -jar target\WallpaperChanger-1.0.0.jar --background
```

---

## Google Drive Setup Guide

### 1. How to Create Your Google OAuth `credentials.json` (Takes ~2 minutes)

Google requires an OAuth 2.0 Client ID so desktop applications can connect to Google Drive. Follow these exact steps:

#### Step 1: Create a Project in Google Cloud
1. Go to the [Google Cloud Project Creation Page](https://console.cloud.google.com/projectcreate).
2. Enter a **Project name** (e.g., `Wallpaper Changer`).
3. Click **Create** and wait a few seconds for the project to be created, then ensure it is selected in the top navigation bar.

#### Step 2: Enable the Google Drive API
1. Open the [Google Drive API Library Page](https://console.cloud.google.com/apis/library/drive.googleapis.com).
2. Click the blue **Enable** button.

#### Step 3: Configure the OAuth Consent Screen
1. Go to the [OAuth Consent Screen](https://console.cloud.google.com/apis/credentials/consent).
2. Under **User Type**, select **External** and click **Create**.
3. Fill in the required fields:
   - **App name**: `Wallpaper Changer`
   - **User support email**: Choose your email address.
   - **Developer contact information**: Enter your email address.
4. Click **Save and Continue**.
5. **Scopes** screen:
   - Click **Add or Remove Scopes**.
   - Search for `drive.readonly` and check the box for `.../auth/drive.readonly` (*See and download all your Google Drive files*).
   - Click **Update**, then **Save and Continue**.
6. **Test Users** screen *(Important)*:
   - Click **+ Add Users**.
   - Enter your Gmail address (the Google account whose Drive contains your wallpapers).
   - Click **Save and Continue**, then click **Back to Dashboard**.

#### Step 4: Generate and Download Credentials JSON
1. Go to the [Google Cloud Credentials Page](https://console.cloud.google.com/apis/credentials).
2. Click **+ Create Credentials** at the top and select **OAuth client ID**.
3. In the **Application type** dropdown, select **Desktop app**.
4. Set **Name** to `Wallpaper Changer Desktop` (or any name you prefer) and click **Create**.
5. A popup window titled *"OAuth client created"* will appear. Click **Download JSON** (this saves a file like `client_secret_xxxxxxxx.json` to your **Downloads** folder).

---

### 2. Connect Your Account in Wallpaper Changer (No file moving needed!)

1. Open the **Wallpaper Changer Settings** window (run `run.bat` or `install.bat`).
2. Under **Google Drive Settings**, click **Browse...** next to **OAuth Credentials (.json)**.
3. Select the JSON file you just downloaded (the file picker automatically opens in your `Downloads` folder).
4. Click **Authenticate with Google**.
   - Your browser will open the authorization page.
   - Sign in with your Google account and click **Continue / Allow**.
5. Paste your Drive wallpaper folder link in the **Drive Folder ID / URL** field.
6. Click **Test Folder** to verify, then click **Save Settings**!

*(The app automatically saves and protects your credentials so you can safely clear your Downloads folder later).*

---

### 3. Set Your Wallpapers Folder
1. In Google Drive, open your wallpapers folder and copy the URL from your browser address bar:
   `https://drive.google.com/drive/folders/1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh`
2. Paste it into the **Drive Folder ID / URL** field in Settings.
3. Click **Test Folder** to verify that images are detected.
4. Click **Save Settings** (or **Change Wallpaper Now**).

---

## How "Unset Deletion" Works

1. Wallpaper Changer checks Google Drive and downloads a newly selected random wallpaper into your local directory.
2. It calls Windows API `SystemParametersInfo` to apply the image as the active desktop background.
3. Windows switches to the new wallpaper and transcodes it.
4. Wallpaper Changer pauses briefly (~1.5 seconds) to allow Windows to release file locks on the previous wallpaper.
5. The previously active wallpaper file is deleted from your local disk.
6. Only the single currently active wallpaper remains in your local directory!

---

## System Startup

To have Wallpaper Changer run silently every time Windows boots:
- Enable the **"Start application automatically at Windows startup"** checkbox in the Settings window and click **Save Settings**, OR
- Double-click `install-startup.bat`.

To remove from startup:
- Uncheck the checkbox in Settings and click **Save Settings**, OR
- Double-click `uninstall-startup.bat`.

---

## Configuration & Storage Locations

Settings and credentials are kept securely in your user profile:
- Application Settings: `%USERPROFILE%\.wallpaperchanger\config.json`
- OAuth Client Secrets: `%USERPROFILE%\.wallpaperchanger\credentials.json`
- User OAuth Tokens: `%USERPROFILE%\.wallpaperchanger\tokens\`
