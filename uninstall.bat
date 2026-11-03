@echo off
setlocal

echo ===================================================
echo   WallpaperChanger - Uninstaller
echo ===================================================

echo Stopping any running WallpaperChanger instances...
taskkill /F /FI "WINDOWTITLE eq Wallpaper Changer*" >nul 2>&1

echo Removing from Windows Startup Registry...
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "WallpaperChanger" /f >nul 2>&1

echo Removing Start Menu shortcut...
del /F /Q "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Wallpaper Changer.lnk" >nul 2>&1

set APP_DIR=%LOCALAPPDATA%\WallpaperChanger
if exist "%APP_DIR%" (
    echo Removing installed files from %APP_DIR%...
    rmdir /S /Q "%APP_DIR%" >nul 2>&1
)

echo.
echo [SUCCESS] WallpaperChanger has been completely removed.
pause
