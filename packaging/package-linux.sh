#!/usr/bin/env bash
# ==============================================================================
# USB Sanitizer — Linux Native Packaging (.deb & .rpm) Script
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
APP_DIR="${PROJECT_ROOT}/DataWiping"
TARGET_DIR="${APP_DIR}/target"
DIST_DIR="${PROJECT_ROOT}/dist"

APP_NAME="usb-sanitizer"
APP_VERSION="${1:-1.0.0}"
VENDOR="Secure Sanitizer Systems"
DESCRIPTION="NIST SP 800-88 Rev. 1 Compliant High-Assurance Data Sanitization Utility"
MAIN_CLASS="com.sanitizer.gui.AppLauncher"

echo "======================================================================"
echo "  Building Linux Native Package (.deb) for ${APP_NAME} v${APP_VERSION}"
echo "======================================================================"

if ! command -v jpackage &> /dev/null; then
    echo "ERROR: 'jpackage' command not found in PATH. Ensure JDK 17+ is installed."
    exit 1
fi

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

ICON_ARG=""
if [ -f "${SCRIPT_DIR}/assets/USBSanitizer.png" ]; then
    ICON_ARG="--icon ${SCRIPT_DIR}/assets/USBSanitizer.png"
fi

# Run jpackage for Linux DEB
echo "Executing jpackage to create .deb..."
jpackage \
    --name "${APP_NAME}" \
    --app-version "${APP_VERSION}" \
    --vendor "${VENDOR}" \
    --description "${DESCRIPTION}" \
    --input "${TARGET_DIR}" \
    --main-jar "$(basename "${JAR_FILE}")" \
    --main-class "${MAIN_CLASS}" \
    --type deb \
    --dest "${DIST_DIR}" \
    --java-options "-Xmx2048m -Dfile.encoding=UTF-8" \
    ${ICON_ARG} \
    --linux-shortcut \
    --linux-menu-group "System;Security;Utility;" \
    --linux-app-category "admin"

echo "======================================================================"
echo " Linux Packaging completed successfully!"
echo " Output artifacts located in: ${DIST_DIR}"
ls -la "${DIST_DIR}"
echo "======================================================================"
