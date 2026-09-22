@echo off
REM ==============================================================================
REM USB Sanitizer — Windows Native Packaging (.msi & .exe) Script (Batch)
REM ==============================================================================
setlocal EnableDelayedExpansion

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set APP_DIR=%PROJECT_ROOT%\DataWiping
set TARGET_DIR=%APP_DIR%\target
set DIST_DIR=%PROJECT_ROOT%\dist

set APP_NAME=USBSanitizer
set APP_VERSION=1.0.0
if not "%~1"=="" set APP_VERSION=%~1
set VENDOR=Secure Sanitizer Systems
set DESCRIPTION=NIST SP 800-88 Rev. 1 Compliant High-Assurance Data Sanitization Utility
set MAIN_CLASS=com.sanitizer.gui.AppLauncher

echo ======================================================================
echo   Building Windows Native Package (.msi) for %APP_NAME% v%APP_VERSION%
echo ======================================================================

where jpackage >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: 'jpackage' command not found in PATH. Ensure JDK 17+ is installed.
    exit /b 1
)

set JAR_FILE=%TARGET_DIR%\usb-sanitizer-1.0-SNAPSHOT-all.jar
if not exist "%JAR_FILE%" (
    set JAR_FILE=%TARGET_DIR%\usb-sanitizer-1.0-SNAPSHOT.jar
)

if not exist "%JAR_FILE%" (
    echo Jar not found. Building with Maven...
    cd /d "%APP_DIR%" && call mvn clean package -DskipTests=true
    set JAR_FILE=%TARGET_DIR%\usb-sanitizer-1.0-SNAPSHOT-all.jar
)

if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

set ICON_ARG=
if exist "%SCRIPT_DIR%assets\USBSanitizer.ico" (
    set ICON_ARG=--icon "%SCRIPT_DIR%assets\USBSanitizer.ico"
)

echo Executing jpackage to create .msi...
jpackage ^
    --name "%APP_NAME%" ^
    --app-version "%APP_VERSION%" ^
    --vendor "%VENDOR%" ^
    --description "%DESCRIPTION%" ^
    --input "%TARGET_DIR%" ^
    --main-jar "usb-sanitizer-1.0-SNAPSHOT-all.jar" ^
    --main-class "%MAIN_CLASS%" ^
    --type msi ^
    --dest "%DIST_DIR%" ^
    --java-options "-Xmx2048m -Dfile.encoding=UTF-8" ^
    %ICON_ARG% ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut

echo ======================================================================
echo  Windows Packaging completed successfully!
echo  Output artifacts located in: %DIST_DIR%
echo ======================================================================
