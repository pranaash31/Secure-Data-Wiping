#!/usr/bin/env bash
# ==============================================================================
# USB Sanitizer — macOS Native Packaging (.dmg & .app) with Notarization
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
APP_DIR="${PROJECT_ROOT}/DataWiping"
TARGET_DIR="${APP_DIR}/target"
DIST_DIR="${PROJECT_ROOT}/dist"

APP_NAME="USBSanitizer"
APP_VERSION="${1:-1.0.0}"
VENDOR="Secure Sanitizer Systems"
DESCRIPTION="NIST SP 800-88 Rev. 1 Compliant High-Assurance Data Sanitization Utility"
MAIN_CLASS="com.sanitizer.gui.AppLauncher"
ENTITLEMENTS="${SCRIPT_DIR}/mac/entitlements.plist"

echo "======================================================================"
echo "  Building macOS Native Package for ${APP_NAME} v${APP_VERSION}"
echo "======================================================================"

# Ensure jpackage is available
if ! command -v jpackage &> /dev/null; then
    echo "ERROR: 'jpackage' command not found in PATH. Ensure JDK 17+ is installed."
    exit 1
fi

# Locate the shaded Uber-JAR
JAR_FILE="${TARGET_DIR}/usb-sanitizer-1.0-SNAPSHOT-all.jar"
if [ ! -f "${JAR_FILE}" ]; then
    JAR_FILE="${TARGET_DIR}/usb-sanitizer-1.0-SNAPSHOT.jar"
fi

if [ ! -f "${JAR_FILE}" ]; then
    echo "Jar not found. Building with Maven..."
    (cd "${APP_DIR}" && mvn clean package -DskipTests=true)
    JAR_FILE="${TARGET_DIR}/usb-sanitizer-1.0-SNAPSHOT-all.jar"
fi

echo "Using JAR: ${JAR_FILE}"
mkdir -p "${DIST_DIR}"

# Icon configuration
ICON_ARG=""
if [ -f "${SCRIPT_DIR}/assets/USBSanitizer.icns" ]; then
    ICON_ARG="--icon ${SCRIPT_DIR}/assets/USBSanitizer.icns"
fi

# Code Signing Flags (if Developer ID is provided via ENV or keychain)
SIGN_ARGS=()
if [ -n "${SIGNING_IDENTITY:-}" ]; then
    echo "Enabling Apple Developer ID Code Signing with identity: ${SIGNING_IDENTITY}"
    SIGN_ARGS+=(
        "--mac-sign"
        "--mac-signing-key-user-name" "${SIGNING_IDENTITY}"
    )
    if [ -f "${ENTITLEMENTS}" ]; then
        SIGN_ARGS+=("--mac-entitlements" "${ENTITLEMENTS}")
    fi
else
    echo "Notice: SIGNING_IDENTITY not set. Proceeding with unsigned / ad-hoc build."
fi

# Run jpackage to generate DMG installer
echo "Executing jpackage to create .dmg..."
jpackage \
    --name "${APP_NAME}" \
    --app-version "${APP_VERSION}" \
    --vendor "${VENDOR}" \
    --description "${DESCRIPTION}" \
    --input "${TARGET_DIR}" \
    --main-jar "$(basename "${JAR_FILE}")" \
    --main-class "${MAIN_CLASS}" \
    --type dmg \
    --dest "${DIST_DIR}" \
    --java-options "-Xmx2048m -Dfile.encoding=UTF-8" \
    ${ICON_ARG} \
    --mac-package-name "${APP_NAME}" \
    --mac-package-identifier "com.sanitizer.usbsanitizer" \
    "${SIGN_ARGS[@]}"

DMG_PATH="${DIST_DIR}/${APP_NAME}-${APP_VERSION}.dmg"

# Apple Notarization Hook (using xcrun notarytool)
if [ -n "${APPLE_ID:-}" ] && [ -n "${APPLE_APP_SPECIFIC_PASSWORD:-}" ] && [ -n "${APPLE_TEAM_ID:-}" ] && [ -f "${DMG_PATH}" ]; then
    echo "======================================================================"
    echo "  Submitting DMG for Apple Notarization (xcrun notarytool)..."
    echo "======================================================================"
    xcrun notarytool submit "${DMG_PATH}" \
        --apple-id "${APPLE_ID}" \
        --password "${APPLE_APP_SPECIFIC_PASSWORD}" \
        --team-id "${APPLE_TEAM_ID}" \
        --wait

    echo "Stapling notarization ticket to DMG..."
    xcrun stapler staple "${DMG_PATH}"
    echo "Apple Notarization & Stapling Completed Successfully!"
fi

echo "======================================================================"
echo " Packaging completed successfully!"
echo " Output artifacts located in: ${DIST_DIR}"
ls -la "${DIST_DIR}"
echo "======================================================================"
