# ==============================================================================
# USB Sanitizer — Windows Native Packaging (.msi & .exe) Script (PowerShell)
# ==============================================================================
param (
    [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$AppDir = Join-Path $ProjectRoot "DataWiping"
$TargetDir = Join-Path $AppDir "target"
$DistDir = Join-Path $ProjectRoot "dist"

$AppName = "USBSanitizer"
$Vendor = "Secure Sanitizer Systems"
$Description = "NIST SP 800-88 Rev. 1 Compliant High-Assurance Data Sanitization Utility"
$MainClass = "com.sanitizer.gui.AppLauncher"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  Building Windows Native Package (.msi) for $AppName v$AppVersion" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    Write-Error "'jpackage' command not found in PATH. Ensure JDK 17+ is installed."
    exit 1
}

$JarFile = Join-Path $TargetDir "usb-sanitizer-1.0-SNAPSHOT-all.jar"
if (-not (Test-Path $JarFile)) {
    $JarFile = Join-Path $TargetDir "usb-sanitizer-1.0-SNAPSHOT.jar"
}

if (-not (Test-Path $JarFile)) {
    Write-Host "Jar not found. Building with Maven..."
    Push-Location $AppDir
    mvn clean package -DskipTests=true
    Pop-Location
    $JarFile = Join-Path $TargetDir "usb-sanitizer-1.0-SNAPSHOT-all.jar"
}

if (-not (Test-Path $DistDir)) {
    New-Item -ItemType Directory -Force -Path $DistDir | Out-Null
}

$IconArgs = @()
$IconPath = Join-Path $ScriptDir "assets\USBSanitizer.ico"
if (Test-Path $IconPath) {
    $IconArgs = @("--icon", $IconPath)
}

$MainJarName = Split-Path -Leaf $JarFile

Write-Host "Executing jpackage to create .msi..." -ForegroundColor Green
$JPackageArgs = @(
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", $Vendor,
    "--description", $Description,
    "--input", $TargetDir,
    "--main-jar", $MainJarName,
    "--main-class", $MainClass,
    "--type", "msi",
    "--dest", $DistDir,
    "--java-options", "-Xmx2048m -Dfile.encoding=UTF-8",
    "--win-dir-chooser",
    "--win-menu",
    "--win-shortcut"
) + $IconArgs

& jpackage @JPackageArgs

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Windows Packaging completed successfully!" -ForegroundColor Cyan
Write-Host " Output artifacts located in: $DistDir" -ForegroundColor Cyan
Get-ChildItem $DistDir
Write-Host "======================================================================" -ForegroundColor Cyan
