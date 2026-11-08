@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ===================================================
echo   WallpaperChanger - Uninstaller
echo ===================================================
echo.

REM 1. Stop any running WallpaperChanger instances (including background tray javaw)
echo Stopping any running WallpaperChanger instances...
taskkill /F /FI "WINDOWTITLE eq Wallpaper Changer*" >nul 2>&1
powershell -NonInteractive -NoProfile -InputFormat None -Command "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*WallpaperChanger*' -and $_.ProcessId -ne $PID } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }" <nul >nul 2>&1

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
if not exist "%USER_DATA_DIR%" (
    if exist "%HOMEDRIVE%%HOMEPATH%\.wallpaperchanger" (
        set "USER_DATA_DIR=%HOMEDRIVE%%HOMEPATH%\.wallpaperchanger"
    )
)

if not exist "%USER_DATA_DIR%" (
    echo.
    echo ---------------------------------------------------
    echo User settings directory was not found:
    echo   %USER_DATA_DIR%
    echo It may have already been removed.
    echo ---------------------------------------------------
    goto finish
)

echo.
echo ---------------------------------------------------
echo Your saved settings, Google credentials, and login
echo tokens are stored in:
echo   %USER_DATA_DIR%
echo ---------------------------------------------------
echo.

if /i "%~1"=="--all" goto delete_config
if /i "%~1"=="-y" goto delete_config
if /i "%~1"=="/y" goto delete_config

REM Use choice.exe which reads keyboard input directly and never skips
where choice >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    choice /C YN /M "Do you want to permanently delete settings and credentials"
    if errorlevel 2 goto keep_config
    if errorlevel 1 goto delete_config
) else (
    set "DEL_DATA="
    set /p "DEL_DATA=Do you want to permanently delete settings and credentials? (y/N): "
    if /i "!DEL_DATA!"=="y" goto delete_config
    if /i "!DEL_DATA!"=="yes" goto delete_config
    goto keep_config
)

:delete_config
echo.
echo Removing user settings and credentials...
rmdir /S /Q "%USER_DATA_DIR%" >nul 2>&1
if exist "%USER_DATA_DIR%" (
    timeout /t 1 /nobreak >nul 2>&1
    rmdir /S /Q "%USER_DATA_DIR%" >nul 2>&1
)
if exist "%USER_DATA_DIR%" (
    echo [WARNING] Could not completely delete %USER_DATA_DIR%. Some files may still be in use.
) else (
    echo [OK] Settings and credentials deleted.
)
goto finish

:keep_config
echo.
echo [INFO] Kept settings and credentials in %USER_DATA_DIR%
goto finish

:finish
echo.
echo ===================================================
echo   SUCCESS! Wallpaper Changer has been uninstalled.
echo ===================================================
pause
