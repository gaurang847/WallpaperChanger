@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo   WallpaperChanger - Uninstaller
echo ===================================================
echo.

REM 1. Stop any running WallpaperChanger instances (including background tray javaw)
echo Stopping any running WallpaperChanger instances...
taskkill /F /FI "WINDOWTITLE eq Wallpaper Changer*" >nul 2>&1
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*WallpaperChanger*' -and $_.ProcessId -ne $PID } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }" >nul 2>&1

REM 2. Remove Windows Startup Registry key
echo Removing from Windows Startup Registry...
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "WallpaperChanger" /f >nul 2>&1

REM 3. Remove Start Menu shortcut
echo Removing Start Menu shortcut...
del /F /Q "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Wallpaper Changer.lnk" >nul 2>&1

REM 4. Remove installed program files (%LOCALAPPDATA%\WallpaperChanger)
set "APP_DIR=%LOCALAPPDATA%\WallpaperChanger"
if exist "%APP_DIR%" (
    echo Removing installed application files from %APP_DIR%...
    rmdir /S /Q "%APP_DIR%" >nul 2>&1
)

REM 5. Prompt to remove user configuration and credentials (%USERPROFILE%\.wallpaperchanger)
set "USER_DATA_DIR=%USERPROFILE%\.wallpaperchanger"
if exist "%USER_DATA_DIR%" (
    echo.
    echo ---------------------------------------------------
    echo Your saved settings, Google credentials, and login
    echo tokens are stored in:
    echo   %USER_DATA_DIR%
    echo ---------------------------------------------------
    set "DEL_DATA=N"
    if /i "%~1"=="--all" (
        set "DEL_DATA=y"
    ) else if /i "%~1"=="-y" (
        set "DEL_DATA=y"
    ) else if /i "%~1"=="/y" (
        set "DEL_DATA=y"
    ) else (
        set /p "DEL_DATA=Do you want to permanently delete settings and credentials? (y/N): "
    )

    if /i "%DEL_DATA%"=="y" (
        echo Removing user settings and credentials...
        rmdir /S /Q "%USER_DATA_DIR%" >nul 2>&1
        echo [OK] Settings and credentials deleted.
    ) else (
        echo [INFO] Kept settings and credentials in %USER_DATA_DIR%
    )
)

echo.
echo ===================================================
echo   SUCCESS! Wallpaper Changer has been uninstalled.
echo ===================================================
pause
