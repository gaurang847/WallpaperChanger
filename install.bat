@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo   WallpaperChanger - Automatic Windows Installer
echo ===================================================

set JAR_SRC=target\WallpaperChanger-1.0.0.jar
if not exist "%JAR_SRC%" (
    echo Building project with Maven...
    call mvn clean package
)

if not exist "%JAR_SRC%" (
    echo [ERROR] Build failed or JAR not found: %JAR_SRC%
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
