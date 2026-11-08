@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo   WallpaperChanger - Automatic Windows Installer
echo ===================================================

set JAR_SRC=
if exist "%~dp0WallpaperChanger.jar" set "JAR_SRC=%~dp0WallpaperChanger.jar"
if not defined JAR_SRC if exist "%~dp0WallpaperChanger-1.0.0.jar" set "JAR_SRC=%~dp0WallpaperChanger-1.0.0.jar"
if not defined JAR_SRC if exist "%~dp0target\WallpaperChanger-1.0.0.jar" set "JAR_SRC=%~dp0target\WallpaperChanger-1.0.0.jar"

if not defined JAR_SRC (
    echo Building project with Maven...
    call mvn clean package
    if exist "%~dp0target\WallpaperChanger-1.0.0.jar" set "JAR_SRC=%~dp0target\WallpaperChanger-1.0.0.jar"
)

if not defined JAR_SRC (
    echo [ERROR] Build failed or JAR not found.
    echo Please make sure Java 21 and Maven are installed.
    pause
    exit /b 1
)

set APP_DIR=%LOCALAPPDATA%\WallpaperChanger
if not exist "%APP_DIR%" mkdir "%APP_DIR%"

echo Installing application files into %APP_DIR%...
copy /Y "%JAR_SRC%" "%APP_DIR%\WallpaperChanger.jar" >nul

REM Create silent background launcher VBS
echo Set WshShell = CreateObject("WScript.Shell") > "%APP_DIR%\run.vbs"
echo WshShell.Run "javaw -jar """ ^& "%APP_DIR%\WallpaperChanger.jar" ^& """ --background", 0, False >> "%APP_DIR%\run.vbs"

REM Register in Windows Startup (Run key)
echo Registering in Windows Startup (HKCU\...\Run)...
reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "WallpaperChanger" /t REG_SZ /d "wscript.exe \"%APP_DIR%\run.vbs\"" /f >nul

REM Register AppUserModelId for clean Action Center toast notification title and icon
reg add "HKCU\Software\Classes\AppUserModelId\Gaurang847.WallpaperChanger" /v "DisplayName" /t REG_SZ /d "Wallpaper Changer" /f >nul 2>&1
if exist "%USERPROFILE%\.wallpaperchanger\app_icon.png" (
    reg add "HKCU\Software\Classes\AppUserModelId\Gaurang847.WallpaperChanger" /v "IconUri" /t REG_SZ /d "%USERPROFILE%\.wallpaperchanger\app_icon.png" /f >nul 2>&1
)

REM Create Start Menu shortcut
set SHORTCUT_PATH=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Wallpaper Changer.lnk
powershell -Command "$s=(New-Object -COM WScript.Shell).CreateShortcut('%SHORTCUT_PATH%');$s.TargetPath='javaw.exe';$s.Arguments='-jar \"%APP_DIR%\WallpaperChanger.jar\"';$s.WorkingDirectory='%APP_DIR%';$s.Save()" >nul 2>&1

echo.
echo ===================================================
echo   SUCCESS! Wallpaper Changer is installed!
echo ===================================================
echo - Installed to: %APP_DIR%
echo - Registered to start with Windows automatically
echo - Shortcut added to Start Menu ("Wallpaper Changer")
echo.
echo Launching Wallpaper Changer settings now...
start javaw -jar "%APP_DIR%\WallpaperChanger.jar"
exit /b 0
