# WallpaperChanger

**WallpaperChanger** is a lightweight, modern Windows background application that keeps your desktop wallpaper synchronized with a centralized Google Drive folder. It periodically selects and downloads a random wallpaper from your Drive folder, applies it as your Windows desktop background, and automatically cleans up unset wallpapers so that your local disk stays completely uncluttered.

---

## Table of Contents

- [Key Features](#key-features)
- [Quick Start (For Users)](#quick-start-for-users)
  - [Option 1: One-Click Automatic Install (Recommended)](#option-1-one-click-automatic-install-recommended)
  - [Option 2: Run Portable (In-Place)](#option-2-run-portable-in-place)
  - [Option 3: Silent Background Launch](#option-3-silent-background-launch)
- [Google Drive Setup Guide](#google-drive-setup-guide)
  - [Method 1: Service Account (Recommended - Never Expires!)](#method-1-service-account-recommended---never-expires)
  - [Method 2: OAuth 2.0 User Client (Alternative)](#method-2-oauth-20-user-client-alternative)
- [How It Works (Under the Hood)](#how-it-works-under-the-hood)
  - [Clean Local Storage ("Unset Deletion")](#clean-local-storage-unset-deletion)
  - [Single-Instance Enforcement & IPC](#single-instance-enforcement--ipc)
  - [Windows Startup Integration](#windows-startup-integration)
- [Developer Guide (Building from Source)](#developer-guide-building-from-source)
  - [Prerequisites](#prerequisites)
  - [Build Standalone Fat JAR](#build-standalone-fat-jar)
  - [Running Unit Tests](#running-unit-tests)
  - [Automated GitHub Releases (CI/CD)](#automated-github-releases-cicd)
- [Configuration & File Locations](#configuration--file-locations)
- [Uninstallation](#uninstallation)

---

## Key Features

1. **Centralized Google Drive Sync**:
   - Maintain a single Google Drive folder of wallpapers accessible across all your Windows devices.
   - Accepts either a Google Drive folder ID or a standard shareable browser link.
   - Automatically detects valid images (`.jpg`, `.jpeg`, `.png`, `.bmp`, `.webp`) and excludes trashed items.
   - **Subfolder Search**: Optionally traverse nested subfolders with selectable search depth (from 1 level up to 10 levels).

2. **Zero Local Clutter (Automatic Disk Cleanup)**:
   - Keeps only the currently active wallpaper on your computer.
   - Previously downloaded wallpapers are automatically removed when a new one is applied, saving disk space.
   - Safely preserves your current desktop wallpaper if the Drive folder is ever empty.

3. **Silent System Tray Operation**:
   - Runs quietly in the background without cluttering your taskbar.
   - Right-click tray menu provides quick controls: **Change Wallpaper Now**, **Pause / Resume**, **Open Wallpaper Folder**, **Settings**, and **Exit**.
   - Desktop notifications let you know when a new wallpaper is applied.

4. **Flexible Rotation & Display Settings**:
   - **Customizable Intervals**: Choose rotation intervals from 5m, 15m, 30m, 1h, 2h, 6h, 12h, 24h, or custom minutes.
   - **Wallpaper Styling**: Supports Fill/Stretch, Fit, Center, Tile, or Span.
   - **Test Folder Connection**: Verify folder access and see total image count before saving.

5. **Easy Setup & Dual Authentication Support**:
   - **Service Account (Recommended)**: Set up once for permanent background sync with no 7-day token expirations or browser login popups.
   - **OAuth 2.0**: Sign in via your browser with your Google account.
   - Built-in file browser automatically detects your credentials type directly from your `Downloads` folder.

6. **Windows Startup Integration**:
   - Optionally start silently in the background whenever Windows boots, with no Administrator privileges required.

7. **Single-Instance Protection**:
   - Ensures only one background instance runs at a time. Launching the app again brings your existing Settings window to the front.

---

## Quick Start (For Users)

> **Requirement**: Ensure [Java Runtime (JRE / JDK) 21 or higher](https://adoptium.net/temurin/releases/?version=21) is installed on your Windows PC.

If you downloaded the ready-to-run release zip (`WallpaperChanger-windows.zip`) from [GitHub Releases](https://github.com):

1. Extract the `.zip` archive to any folder on your computer.
2. Choose one of the launch options below:

### Option 1: One-Click Automatic Install (Recommended)
Double-click [`install.bat`](file:///home/gaurang/projects/WallpaperChanger/install.bat). It will:
1. Install the application into `%LOCALAPPDATA%\WallpaperChanger\`.
2. Add a shortcut to your Windows **Start Menu** (`Wallpaper Changer`).
3. Register the app in Windows Startup (starts silently in the background on every boot).
4. Launch the Settings window immediately so you can connect your Google account.

### Option 2: Run Portable (In-Place)
Double-click [`run.bat`](file:///home/gaurang/projects/WallpaperChanger/run.bat):
- Starts Wallpaper Changer directly from its current folder without modifying your system.

### Option 3: Silent Background Launch
Double-click [`run-background.vbs`](file:///home/gaurang/projects/WallpaperChanger/run-background.vbs) or execute:
```cmd
javaw -jar WallpaperChanger.jar --background
```
- Starts directly in the Windows System Tray without showing any console or window.

---

## Google Drive Setup Guide

Connecting Wallpaper Changer to your Google Drive takes about 2 minutes. You can choose either the **Service Account method (Recommended - never expires)** or the traditional **OAuth 2.0 method**:

---

### Method 1: Service Account (Recommended - Never Expires!)

A **Google Service Account** acts as an automated background bot identity for your app.
* **Benefits:** 
  - **No 7-day token expiration!** Permanent credentials that never expire.
  - **No web browser popups or login consent screens.**
  - **No domain ownership or privacy policy requirements.**

#### Step 1: Create a Google Cloud Project & Enable Drive API
1. Open the [Google Cloud Project Creation Page](https://console.cloud.google.com/projectcreate) and name it `Wallpaper Changer` → click **Create**.
2. Open the [Google Drive API Library Page](https://console.cloud.google.com/apis/library/drive.googleapis.com) → click **Enable**.

#### Step 2: Create a Service Account & Download Key
1. Open the [Service Accounts Page](https://console.cloud.google.com/iam-admin/serviceaccounts).
2. Click **+ CREATE SERVICE ACCOUNT** at the top.
   - Set **Service account name** to `Wallpaper Bot`.
   - Click **Create and Continue**, then click **Done**.
3. In the list, click on your newly created service account (or click the three dots `⋮` on the right → **Manage keys**).
4. Go to the **KEYS** tab at the top → click **ADD KEY** → **Create new key**.
5. Choose **JSON** and click **CREATE**.
   - A `.json` private key file will download to your `Downloads` folder!
6. Copy the **email address** of your service account (e.g. `wallpaper-bot@your-project.iam.gserviceaccount.com`).

#### Step 3: Share Your Google Drive Wallpapers Folder
1. Go to [Google Drive](https://drive.google.com/) and find your Wallpapers folder.
2. Right-click the folder → click **Share**.
3. Paste the service account email address, leave the role as **Viewer**, uncheck "Notify people", and click **Send / Share**.

#### Step 4: Connect in Wallpaper Changer
1. Open Wallpaper Changer (run `run.bat` or `install.bat`).
2. Under **Google Drive Settings**, click **Browse...** next to **OAuth / Service Account (.json)**.
3. Select the JSON file you downloaded in Step 2.
   - The app will automatically detect it as a Service Account!
4. Paste your Drive folder URL in the **Drive Folder ID / URL** field.
5. Click **Test Folder** to verify that images are detected, then click **Save Settings**!

---

### Method 2: OAuth 2.0 User Client (Alternative)

If you prefer logging in with your personal Google account via your browser:

#### Step 1: Create Project & Enable Drive API
1. Create a project at [Google Cloud Project Creation](https://console.cloud.google.com/projectcreate).
2. Enable the [Google Drive API](https://console.cloud.google.com/apis/library/drive.googleapis.com).

#### Step 2: Configure OAuth Consent Screen
1. Go to the [OAuth Consent Screen](https://console.cloud.google.com/apis/credentials/consent) → select **External** → click **Create**.
2. Fill in App Name (`Wallpaper Changer`) and your email address.
3. Under **Scopes**, add `.../auth/drive.readonly`.
4. Under **Test Users**, add your personal Gmail address → click **Save**.
   *(Note: In Testing mode, Google requires re-authenticating every 7 days unless published).*

#### Step 3: Generate Desktop Credentials JSON
1. Go to [Google Cloud Credentials](https://console.cloud.google.com/apis/credentials) → **+ Create Credentials** → **OAuth client ID**.
2. Choose **Desktop app** → name it `Wallpaper Changer Desktop` → click **Create**.
3. Click **Download JSON**.

#### Step 4: Connect in Wallpaper Changer
1. Open Wallpaper Changer → click **Browse...** → select the downloaded OAuth JSON.
2. Click **Authenticate with Google** and complete the browser sign-in.
3. Paste your Drive folder link and click **Save Settings**!

---

## How It Works (Under the Hood)

### Clean Local Storage ("Unset Deletion")
1. Wallpaper Changer queries the Google Drive API and downloads a newly selected random wallpaper into your local directory.
2. It calls the Windows User32 API `SystemParametersInfo(SPI_SETDESKWALLPAPER)` to set the image as the active background.
3. Windows switches to the new wallpaper and transcodes it into the operating system cache.
4. Wallpaper Changer pauses briefly (~1.5 seconds) allowing Windows to release file locks on the previous wallpaper.
5. The previously active wallpaper file is deleted from local disk. Only the single active wallpaper remains locally!

### Single-Instance Enforcement & IPC
1. On startup, the application acquires an exclusive file lock on `%USERPROFILE%\.wallpaperchanger\app.lock`.
2. It binds an ephemeral loopback socket on `127.0.0.1` and records the assigned port in `app.port`.
3. If a duplicate instance is launched, lock acquisition fails. The duplicate process reads `app.port`, sends a focus signal to the primary process, and immediately terminates.
4. The primary instance intercepts the signal on the Swing Event Dispatch Thread and brings the Settings window to the front.

### Windows Startup Integration
1. Toggleable via the Settings GUI checkbox or configured automatically by `install.bat`.
2. Registers a startup command in `HKCU\Software\Microsoft\Windows\CurrentVersion\Run\WallpaperChanger` pointing to a lightweight VBS launcher (`run.vbs`) running `javaw --background`.
3. Requires no Administrator / UAC privileges.

---

## Developer Guide (Building from Source)

### Prerequisites
- **Operating System**: Windows 11 / 10 / 8 / 7, or Linux / macOS (for compilation)
- **Java**: Java Development Kit (JDK) 21 or higher
- **Maven**: Apache Maven 3.6+

### Build Standalone Fat JAR
To compile the source code, run tests, and package the shaded executable fat JAR:
```bash
mvn clean package
```

The output fat JAR will be located at:
```
target/WallpaperChanger-1.0.0.jar
```

### Running Unit Tests
To execute all 23 automated unit tests:
```bash
mvn test
```

### Automated GitHub Releases (CI/CD)
The project includes a GitHub Actions release pipeline ([`.github/workflows/release.yml`](file:///home/gaurang/projects/WallpaperChanger/.github/workflows/release.yml)). Whenever a git version tag is pushed (e.g. `v1.0.0`):
1. GitHub Actions sets up Java 21 and runs `mvn clean package`.
2. Bundles `WallpaperChanger.jar`, `install.bat`, `run.bat`, `uninstall.bat`, `run-background.vbs`, and a `QUICKSTART.txt` guide into `WallpaperChanger-windows.zip`.
3. Automatically publishes a GitHub Release with the zip asset attached.

To trigger a release:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Configuration & File Locations

All application data is isolated within the user's home profile directory:

| Resource | Path | Description |
| :--- | :--- | :--- |
| **Settings** | `%USERPROFILE%\.wallpaperchanger\config.json` | JSON configuration (rotation interval, Drive folder, style, etc.) |
| **OAuth Credentials** | `%USERPROFILE%\.wallpaperchanger\credentials.json` | Google Cloud OAuth Client ID & Secret |
| **OAuth Tokens** | `%USERPROFILE%\.wallpaperchanger\tokens\` | User authorization tokens generated during browser login |
| **Instance Locks** | `%USERPROFILE%\.wallpaperchanger\app.lock` / `app.port` | Concurrency lock and loopback IPC port |
| **Installed App** | `%LOCALAPPDATA%\WallpaperChanger\` | Application JAR and launcher script (when using `install.bat`) |
| **Start Menu** | `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Wallpaper Changer.lnk` | Start Menu shortcut |

---

## Uninstallation

To completely uninstall Wallpaper Changer:
1. Double-click [`uninstall.bat`](file:///home/gaurang/projects/WallpaperChanger/uninstall.bat).
2. The script will:
   - Terminate any running Wallpaper Changer background processes.
   - Remove the Windows Startup Registry key (`HKCU\...\Run\WallpaperChanger`).
   - Delete the Start Menu shortcut.
   - Remove the `%LOCALAPPDATA%\WallpaperChanger\` application directory.
   - Prompt whether you want to delete your saved settings and Google credentials (`%USERPROFILE%\.wallpaperchanger\`).
