# WallpaperChanger

**WallpaperChanger** is a lightweight, modern Windows background application that keeps your desktop wallpaper synchronized with a centralized Google Drive folder. It periodically downloads a random wallpaper from your Drive folder, applies it as your Windows desktop background, and cleans up old wallpapers once they have been unset so that your local drive stays completely uncluttered.

---

## Key Features

1. **Centralized Google Drive Storage**:
   - Maintain a single Google Drive folder of wallpapers shared across all your Windows devices.
   - Supports pasting either a Google Drive Folder ID or a full shareable browser URL (e.g. `https://drive.google.com/drive/folders/...`).
   - Automatically filters for valid images (`.jpg`, `.jpeg`, `.png`, `.bmp`, `.webp`) and excludes trashed files.
   - Paginates through large folders to discover all images.

2. **Clean Local Storage (Unset Deletion)**:
   - Only the currently active wallpaper is stored locally.
   - As soon as a new wallpaper is selected and set, the previously unset wallpaper file is automatically removed from your local disk.
   - Prevents gigabytes of wallpaper files from accumulating on your computer over time.

3. **Background Execution & System Tray**:
   - Runs silently in the background with a Windows System Tray icon.
   - Tray context menu options:
     - **Change Wallpaper Now**: Immediately fetch and set a new wallpaper.
     - **Pause / Resume Auto-Change**: Temporarily pause automatic rotations.
     - **Open Wallpaper Folder**: Quick access to the local folder in Windows File Explorer.
     - **Settings...**: Open the configuration interface.
     - **Exit**: Cleanly shut down the application.
   - Balloon notifications alert you when wallpapers update or if attention is needed.

4. **Modern Configuration Interface**:
   - **Menu Bar & Global Shortcuts**: File menu with shortcuts (`Ctrl+Q` to exit, `Esc` to hide to tray, `Ctrl+R` to change wallpaper, `Ctrl+S` to save).
   - **1-Click OAuth Credentials Picker**: Select your downloaded credentials JSON directly from `Downloads` with automatic backup.
   - **Status Bar**: Clean 2-row layout displaying the active wallpaper filename (with smart truncation and full hover tooltip) alongside the next scheduled rotation time.
   - **Action Bar**: Includes a prominent **Exit Application** button alongside **Change Wallpaper Now**, **Save Settings**, and **Close to Tray**.
   - **Test Folder Connection**: Instant verification that tests your Drive link and counts how many wallpaper images exist.
   - **Customizable Intervals**: Presets for 5m, 15m, 30m, 1h, 2h, 6h, 12h, 24h, or custom minutes.
   - **Wallpaper Styling**: Fill/Stretch, Fit, Center, Tile, or Span.

5. **Single-Instance Enforcement**:
   - Uses native OS file locking and local loopback IPC to guarantee only one instance runs at a time.
   - Launching a second instance automatically brings the existing Settings window to the front and terminates the duplicate process.

6. **Windows Startup Integration**:
   - Starts automatically with Windows in background mode (`--background`).
   - Uses native Windows Registry (`HKCU\Software\Microsoft\Windows\CurrentVersion\Run`) via JNA without requiring Administrator privileges.
   - Toggleable directly from the Settings GUI checkbox or configured automatically via `install.bat`.

7. **Built on Java 21 LTS & Modern Libraries**:
   - **Virtual Threads** (`Thread.ofVirtual()`): Background downloads and Drive API calls run on lightweight virtual threads.
   - **FlatLaf Modern UI**: Clean, rounded, high-DPI modern Swing theme.
   - **Records**: Boilerplate-free immutable configuration models.
   - **Jackson JSON**: Persistent settings stored in human-readable `config.json`.
   - **Standalone Executable Fat JAR**: Bundles all dependencies into `target/WallpaperChanger-1.0.0.jar`.

---

## Requirements

- **Operating System**: Windows 11 / 10 / 8 / 7 (64-bit or 32-bit)
- **Java**: Java Development Kit (JDK) 21 or higher
- **Maven**: Apache Maven 3.6+ (to build the project)

---

## Build Instructions

To build the executable standalone JAR:

```bash
mvn clean package
```

The output fat JAR will be generated at:
```
target/WallpaperChanger-1.0.0.jar
```

To run all unit tests:
```bash
mvn test
```

---

## Installation & Running

### Option 1: One-Click Automatic Install (Recommended)
Double-click [`install.bat`](file:///home/gaurang/projects/WallpaperChanger/install.bat). It will:
1. Automatically compile the project with Maven (if not already compiled).
2. Install the application into `%LOCALAPPDATA%\WallpaperChanger\`.
3. Add a shortcut to your Windows **Start Menu** (`Wallpaper Changer`).
4. Register the app in Windows Startup (starts silently in the background on every boot).
5. Launch the Settings window immediately so you can connect your Google account and set your Drive folder.

*(To completely remove the app later, simply double-click [`uninstall.bat`](file:///home/gaurang/projects/WallpaperChanger/uninstall.bat)).*

### Option 2: Run in-place (Portable / No installation needed)
Simply double-click [`run.bat`](file:///home/gaurang/projects/WallpaperChanger/run.bat):
- It automatically builds the JAR if needed and starts the application directly.

### Option 3: Silent Background Launch (Tray Only)
Double-click [`run-background.vbs`](file:///home/gaurang/projects/WallpaperChanger/run-background.vbs) or execute:
```cmd
javaw -jar target\WallpaperChanger-1.0.0.jar --background
```

---

## Google Drive Setup Guide

Connecting Wallpaper Changer to your Google Drive takes about 2 minutes. Follow this step-by-step guide:

### Step 1: Create a Google Cloud Project
1. Open the [Google Cloud Project Creation Page](https://console.cloud.google.com/projectcreate).
2. Enter a **Project name** (e.g. `Wallpaper Changer`).
3. Click **Create** and wait a few seconds. Ensure your new project is selected in the top navigation bar.

### Step 2: Enable the Google Drive API
1. Open the [Google Drive API Library Page](https://console.cloud.google.com/apis/library/drive.googleapis.com).
2. Click the blue **Enable** button.

### Step 3: Configure the OAuth Consent Screen
1. Open the [OAuth Consent Screen](https://console.cloud.google.com/apis/credentials/consent).
2. Under **User Type**, select **External** and click **Create**.
3. Fill in the required fields:
   - **App name**: `Wallpaper Changer`
   - **User support email**: Choose your email address.
   - **Developer contact information**: Enter your email address.
4. Click **Save and Continue**.
5. **Scopes Screen**:
   - Click **Add or Remove Scopes**.
   - Search for `drive.readonly` and check `.../auth/drive.readonly` (*See and download all your Google Drive files*).
   - Click **Update**, then **Save and Continue**.
6. **Test Users Screen** *(Important)*:
   - Click **+ Add Users**.
   - Enter your personal Gmail address (the Google account whose Drive contains your wallpapers).
   - Click **Save and Continue**, then click **Back to Dashboard**.

### Step 4: Generate and Download Credentials JSON
1. Open the [Google Cloud Credentials Page](https://console.cloud.google.com/apis/credentials).
2. Click **+ Create Credentials** at the top → select **OAuth client ID**.
3. In the **Application type** dropdown, select **Desktop app**.
4. Set **Name** to `Wallpaper Changer Desktop` and click **Create**.
5. A popup titled *"OAuth client created"* will appear. Click **Download JSON** (this saves a file like `client_secret_xxxxxxxx.json` to your `Downloads` folder).

---

### Step 5: Connect in Wallpaper Changer (No manual file moving needed!)

1. Open Wallpaper Changer (run `run.bat` or `install.bat`).
2. Under **Google Drive Settings**, click the **Browse...** button next to **OAuth Credentials (.json)**.
   - The file picker will automatically open in your `Downloads` folder.
   - Select the JSON file you just downloaded.
   - *Wallpaper Changer automatically saves a secure copy so you can safely empty your Downloads folder later.*
3. Click **Authenticate with Google**.
   - Your default web browser will open Google's authorization page.
   - Sign in with the Gmail account you added under Test Users.
   - If prompted with *"Google hasn't verified this app"*, click **Advanced** → **Go to Wallpaper Changer (unsafe)** → **Continue / Allow**.
4. Copy the URL of your Google Drive wallpapers folder from your browser address bar:
   `https://drive.google.com/drive/folders/1UUL26QCa7U7paRpxYDYNZUFwrNlQ74uh`
5. Paste it into **Drive Folder ID / URL** in Settings.
6. Click **Test Folder** to verify that images are detected, then click **Save Settings**!

---

## How "Unset Deletion" Works

1. Wallpaper Changer checks Google Drive and downloads a newly selected random wallpaper into your local directory.
2. It calls the Windows API `SystemParametersInfo` to apply the image as the active desktop background.
3. Windows updates the wallpaper and transcodes it.
4. Wallpaper Changer pauses briefly (~1.5 seconds) to allow Windows to release file locks on the previous wallpaper.
5. The previously active wallpaper file is deleted from your local disk.
6. Only the single currently active wallpaper remains in your local directory!

---

## Windows Startup & Management

To have Wallpaper Changer run silently every time Windows boots:
- Enable the **"Start application automatically at Windows startup"** checkbox in the Settings window and click **Save Settings**, OR
- Run `install.bat` (which configures startup automatically).

To disable startup:
- Uncheck the **"Start application automatically at Windows startup"** checkbox in Settings and click **Save Settings**, OR
- Run `uninstall.bat` to completely remove the application and its startup registry keys.

---

## Configuration & Storage Locations

Settings and credentials are kept securely in your Windows user profile:
- Application Settings: `%USERPROFILE%\.wallpaperchanger\config.json`
- OAuth Client Secrets: `%USERPROFILE%\.wallpaperchanger\credentials.json`
- User OAuth Tokens: `%USERPROFILE%\.wallpaperchanger\tokens\`
- Single Instance Locks: `%USERPROFILE%\.wallpaperchanger\app.lock` and `app.port`
