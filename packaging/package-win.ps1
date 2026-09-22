# ==============================================================================
# USB Sanitizer — Windows Native Packaging (.msi & .exe) with Authenticode Signing
# ==============================================================================
param (
    [string]$AppVersion = "1.0.0",
    [string]$CertificatePath = $env:WIN_CERTIFICATE_PATH,
    [string]$CertificatePassword = $env:WIN_CERTIFICATE_PASSWORD,
    [string]$TimestampServer = "http://timestamp.digicert.com"
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

$MsiPath = Join-Path $DistDir "$AppName-$AppVersion.msi"

# Authenticode Signing with signtool.exe
if ($CertificatePath -and (Test-Path $CertificatePath) -and (Test-Path $MsiPath)) {
    Write-Host "======================================================================" -ForegroundColor Green
    Write-Host "  Signing MSI Installer with Authenticode Certificate ($CertificatePath)..." -ForegroundColor Green
    Write-Host "======================================================================" -ForegroundColor Green

    $SignArgs = @(
        "sign",
        "/f", $CertificatePath,
        "/fd", "SHA256",
        "/tr", $TimestampServer,
        "/td", "SHA256",
        "/d", $Description
    )
    if ($CertificatePassword) {
        $SignArgs += @("/p", $CertificatePassword)
    }
    $SignArgs += $MsiPath

    & signtool @SignArgs
    Write-Host "MSI signing completed successfully!" -ForegroundColor Green
} else {
    Write-Host "Notice: Certificate not provided or signtool not found. Proceeding with unsigned MSI." -ForegroundColor Yellow
}

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host " Windows Packaging completed successfully!" -ForegroundColor Cyan
Write-Host " Output artifacts located in: $DistDir" -ForegroundColor Cyan
Get-ChildItem $DistDir
Write-Host "======================================================================" -ForegroundColor Cyan
