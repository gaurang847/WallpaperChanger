@echo off
REM ===================================================
REM WallpaperChanger - Run Script
REM ===================================================

cd /d "%~dp0"

REM Find the built JAR file
set JAR_FILE=target\WallpaperChanger-1.0.0.jar
if not exist "%JAR_FILE%" (
    echo Building project with Maven...
    call mvn clean package
)

if not exist "%JAR_FILE%" (
    echo Error: Could not find or build %JAR_FILE%.
    echo Please make sure Maven and Java are installed and run: mvn clean package
    pause
    exit /b 1
)

echo Starting Wallpaper Changer...
start javaw -jar "%JAR_FILE%"
exit /b 0
