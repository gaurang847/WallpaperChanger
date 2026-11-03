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

### 1. Google OAuth Client Credentials (`credentials.json`)
For security and privacy, sensitive client secrets are **never committed to git**.

To connect your application:
1. Download your OAuth 2.0 Client credentials from the [Google Cloud Console](https://console.cloud.google.com/) (Application type: **Desktop App**).
2. Either:
   - Place the file at: `%USERPROFILE%\.wallpaperchanger\credentials.json`, or
   - In the Settings window, click **Browse...** next to **Custom credentials.json**.
3. **You only need to do this once on your PC.** The application remembers the path across reboots and background syncs.

*(A placeholder template is provided in `src/main/resources/credentials.example.json`).*

### 2. Connect Your Google Account
1. In the Wallpaper Changer Settings window, click **Authenticate with Google**.
2. Your default browser opens the Google OAuth authorization prompt.
3. Sign in with your Google account and grant read access to your Drive.
4. User OAuth tokens are saved securely on your PC in `%USERPROFILE%\.wallpaperchanger\tokens\`.

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
