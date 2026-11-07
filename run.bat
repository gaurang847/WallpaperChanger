@echo off
REM ===================================================
REM WallpaperChanger - Run Script
REM ===================================================

cd /d "%~dp0"

REM Find the built JAR file
set JAR_FILE=
if exist "%~dp0WallpaperChanger.jar" set "JAR_FILE=%~dp0WallpaperChanger.jar"
if not defined JAR_FILE if exist "%~dp0WallpaperChanger-1.0.0.jar" set "JAR_FILE=%~dp0WallpaperChanger-1.0.0.jar"
if not defined JAR_FILE if exist "%~dp0target\WallpaperChanger-1.0.0.jar" set "JAR_FILE=%~dp0target\WallpaperChanger-1.0.0.jar"

if not defined JAR_FILE (
    echo Building project with Maven...
    call mvn clean package
    if exist "%~dp0target\WallpaperChanger-1.0.0.jar" set "JAR_FILE=%~dp0target\WallpaperChanger-1.0.0.jar"
)

if not defined JAR_FILE (
    echo Error: Could not find or build WallpaperChanger JAR.
    echo Please make sure Java 21 is installed.
    pause
    exit /b 1
)

echo Starting Wallpaper Changer...
start javaw -jar "%JAR_FILE%"
exit /b 0
