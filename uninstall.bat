@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo   WallpaperChanger - Uninstaller
echo ===================================================
echo.

REM Resolve user configuration directory
set "USER_DATA_DIR=%USERPROFILE%\.wallpaperchanger"
if not exist "%USER_DATA_DIR%" if exist "%HOMEDRIVE%%HOMEPATH%\.wallpaperchanger" (
    set "USER_DATA_DIR=%HOMEDRIVE%%HOMEPATH%\.wallpaperchanger"
)

REM 1. Stop any running WallpaperChanger java processes
echo Stopping any running WallpaperChanger instances...

REM 1a. If app.port exists, find the exact listening PID via netstat and kill it
if exist "%USER_DATA_DIR%\app.port" (
    for /f "usebackq tokens=*" %%P in ("%USER_DATA_DIR%\app.port") do (
        for /f "tokens=5" %%A in ('netstat -a -n -o ^| findstr /r /c:":%%P .*LISTENING"') do (
            taskkill /F /PID %%A >nul 2>&1
        )
    )
)

REM 1b. Terminate any window with title "Wallpaper Changer*"
taskkill /F /IM javaw.exe /FI "WINDOWTITLE eq Wallpaper Changer*" >nul 2>&1
taskkill /F /IM java.exe /FI "WINDOWTITLE eq Wallpaper Changer*" >nul 2>&1

REM 1c. If jps is available, kill any java process running WallpaperChanger
for /f "tokens=1" %%I in ('jps -l 2^>nul ^| findstr /i "WallpaperChanger"') do (
    taskkill /F /PID %%I >nul 2>&1
)

REM 1d. Thorough PowerShell termination of WallpaperChanger processes and startup scripts
powershell -NonInteractive -NoProfile -ExecutionPolicy Bypass -Command "$portFiles = @(Join-Path $env:USERPROFILE '.wallpaperchanger\app.port', Join-Path $env:TEMP '.wallpaperchanger\app.port'); foreach ($pf in $portFiles) { if (Test-Path $pf) { $p = (Get-Content $pf -ErrorAction SilentlyContinue).Trim(); if ($p) { try { $tcp = Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue; if ($tcp) { $tcp | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue } } } catch {} } } }; Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { ($_.Name -like 'java*') -and ($_.CommandLine -like '*WallpaperChanger*') } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }; Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.Name -eq 'wscript.exe' -and $_.CommandLine -like '*run.vbs*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }" >nul 2>&1

REM Allow operating system time to release file locks
timeout /t 1 /nobreak >nul 2>&1

REM 2. Remove Windows Startup Registry key
echo Removing from Windows Startup Registry...
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v "WallpaperChanger" /f >nul 2>&1

REM 3. Remove Start Menu shortcut
echo Removing Start Menu shortcut...
del /F /Q "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Wallpaper Changer.lnk" >nul 2>&1

REM 4. Remove installed program files (%LOCALAPPDATA%\WallpaperChanger)
set "APP_DIR=%LOCALAPPDATA%\WallpaperChanger"
if exist "%APP_DIR%" (
    if /i not "%~dp0"=="%APP_DIR%\" (
        echo Removing installed application files from %APP_DIR%...
        attrib -r -s -h "%APP_DIR%\*.*" /s /d >nul 2>&1
        del /f /s /q /a "%APP_DIR%\*.*" >nul 2>&1
        rmdir /S /Q "%APP_DIR%" >nul 2>&1
        powershell -NonInteractive -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path -LiteralPath '%APP_DIR%') { Remove-Item -LiteralPath '%APP_DIR%' -Recurse -Force -ErrorAction SilentlyContinue }" >nul 2>&1
    )
)

REM 5. Prompt to remove user configuration and credentials (%USERPROFILE%\.wallpaperchanger)
echo.
echo ---------------------------------------------------
echo Saved settings, Google credentials, and login
echo tokens location:
echo   %USER_DATA_DIR%
echo.
echo WARNING: If you delete this folder, you will need
echo to configure your Google Drive credentials and
echo settings again if you decide to reinstall later.
echo ---------------------------------------------------
echo.

REM Handle non-interactive CLI flags (--all, -y, /y)
if /i "%~1"=="--all" goto do_delete
if /i "%~1"=="-y" goto do_delete
if /i "%~1"=="/y" goto do_delete

REM Prompt user for confirmation
set "DEL_DATA="
set /p "DEL_DATA=Do you want to permanently delete settings and credentials? (y/N): "
if /i "%DEL_DATA%"=="y" goto do_delete
if /i "%DEL_DATA%"=="yes" goto do_delete
if /i "%DEL_DATA%"=="y " goto do_delete
goto do_keep

:do_delete
echo.
echo Removing user settings and credentials...

REM Final safeguard: ensure any running instance listening on port is terminated
if exist "%USER_DATA_DIR%\app.port" (
    for /f "usebackq tokens=*" %%P in ("%USER_DATA_DIR%\app.port") do (
        for /f "tokens=5" %%A in ('netstat -a -n -o ^| findstr /r /c:":%%P .*LISTENING"') do (
            taskkill /F /PID %%A >nul 2>&1
        )
    )
)
powershell -NonInteractive -NoProfile -ExecutionPolicy Bypass -Command "$pf = Join-Path $env:USERPROFILE '.wallpaperchanger\app.port'; if (Test-Path $pf) { $p = (Get-Content $pf -ErrorAction SilentlyContinue).Trim(); if ($p) { try { Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue } } catch {} } }; Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { ($_.Name -like 'java*') -and ($_.CommandLine -like '*WallpaperChanger*') } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }" >nul 2>&1

set "DELETED_ANY=0"

if exist "%USER_DATA_DIR%" (
    set "DELETED_ANY=1"
    echo Cleaning %USER_DATA_DIR%...
    attrib -r -s -h "%USER_DATA_DIR%\*.*" /s /d >nul 2>&1
    attrib -r -s -h "%USER_DATA_DIR%" >nul 2>&1
    del /f /s /q /a "%USER_DATA_DIR%\*.*" >nul 2>&1
    for /d %%D in ("%USER_DATA_DIR%\*") do (
        rmdir /s /q "%%D" >nul 2>&1
    )
    rmdir /s /q "%USER_DATA_DIR%" >nul 2>&1
    powershell -NonInteractive -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path -LiteralPath '%USER_DATA_DIR%') { Start-Sleep -Milliseconds 400; Remove-Item -LiteralPath '%USER_DATA_DIR%' -Recurse -Force -ErrorAction SilentlyContinue }" >nul 2>&1
)

REM Also clean alternate user dir if different
if defined HOMEDRIVE if defined HOMEPATH (
    set "ALT_USER_DIR=%HOMEDRIVE%%HOMEPATH%\.wallpaperchanger"
    if exist "%ALT_USER_DIR%" if /i not "%ALT_USER_DIR%"=="%USER_DATA_DIR%" (
        set "DELETED_ANY=1"
        attrib -r -s -h "%ALT_USER_DIR%\*.*" /s /d >nul 2>&1
        attrib -r -s -h "%ALT_USER_DIR%" >nul 2>&1
        del /f /s /q /a "%ALT_USER_DIR%\*.*" >nul 2>&1
        rmdir /s /q "%ALT_USER_DIR%" >nul 2>&1
        powershell -NonInteractive -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path -LiteralPath '%ALT_USER_DIR%') { Remove-Item -LiteralPath '%ALT_USER_DIR%' -Recurse -Force -ErrorAction SilentlyContinue }" >nul 2>&1
    )
)

REM Also clean temp directory fallback if present
if exist "%TEMP%\.wallpaperchanger" (
    attrib -r -s -h "%TEMP%\.wallpaperchanger\*.*" /s /d >nul 2>&1
    del /f /s /q /a "%TEMP%\.wallpaperchanger\*.*" >nul 2>&1
    rmdir /s /q "%TEMP%\.wallpaperchanger" >nul 2>&1
    powershell -NonInteractive -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path -LiteralPath '$env:TEMP\.wallpaperchanger') { Remove-Item -LiteralPath '$env:TEMP\.wallpaperchanger' -Recurse -Force -ErrorAction SilentlyContinue }" >nul 2>&1
)

if exist "%USER_DATA_DIR%" (
    set "CONFIG_STATUS=FAILED"
    echo [WARNING] Could not completely delete %USER_DATA_DIR%.
    echo The following files or folders are still present:
    dir /b /a "%USER_DATA_DIR%" 2>nul
    echo Please ensure Wallpaper Changer is fully closed and delete the folder manually.
) else (
    set "CONFIG_STATUS=DELETED"
    if "%DELETED_ANY%"=="1" (
        echo [OK] Settings and credentials deleted successfully.
    ) else (
        echo [OK] Settings directory was already clean.
    )
)
goto finish

:do_keep
set "CONFIG_STATUS=KEPT"
echo.
echo [INFO] Kept settings and credentials in %USER_DATA_DIR%
goto finish

:finish
echo.
echo ===================================================
if "%CONFIG_STATUS%"=="FAILED" (
    echo   Wallpaper Changer application removed.
    echo   Note: Some settings files could not be deleted.
) else (
    echo   SUCCESS! Wallpaper Changer has been uninstalled.
)
echo ===================================================
pause
