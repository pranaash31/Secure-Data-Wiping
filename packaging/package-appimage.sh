#!/usr/bin/env bash
# ==============================================================================
# USB Sanitizer — Universal Linux AppImage Packager
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
APP_DIR="${PROJECT_ROOT}/DataWiping"
TARGET_DIR="${APP_DIR}/target"
DIST_DIR="${PROJECT_ROOT}/dist"
APP_DIR_BUILD="${PROJECT_ROOT}/build/AppDir"

APP_NAME="USBSanitizer"
APP_VERSION="${1:-1.0.0}"
ARCH="$(uname -m)"

echo "======================================================================"
echo "  Building Universal Linux AppImage for ${APP_NAME} v${APP_VERSION} (${ARCH})"
echo "======================================================================"

JAR_FILE="${TARGET_DIR}/usb-sanitizer-1.0-SNAPSHOT-all.jar"
if [ ! -f "${JAR_FILE}" ]; then
    JAR_FILE="${TARGET_DIR}/usb-sanitizer-1.0-SNAPSHOT.jar"
fi

if [ ! -f "${JAR_FILE}" ]; then
    echo "Building shaded JAR with Maven..."
    (cd "${APP_DIR}" && mvn clean package -DskipTests=true)
    JAR_FILE="${TARGET_DIR}/usb-sanitizer-1.0-SNAPSHOT-all.jar"
fi

# Clean previous build
rm -rf "${APP_DIR_BUILD}"
mkdir -p "${APP_DIR_BUILD}/usr/bin"
mkdir -p "${APP_DIR_BUILD}/usr/share/applications"
mkdir -p "${APP_DIR_BUILD}/usr/share/icons/hicolor/256x256/apps"
mkdir -p "${APP_DIR_BUILD}/usr/lib/usb-sanitizer"
mkdir -p "${DIST_DIR}"

# Copy JAR
cp "${JAR_FILE}" "${APP_DIR_BUILD}/usr/lib/usb-sanitizer/usb-sanitizer.jar"

# Copy Icon
if [ -f "${SCRIPT_DIR}/assets/USBSanitizer.png" ]; then
    cp "${SCRIPT_DIR}/assets/USBSanitizer.png" "${APP_DIR_BUILD}/usr/share/icons/hicolor/256x256/apps/usb-sanitizer.png"
    cp "${SCRIPT_DIR}/assets/USBSanitizer.png" "${APP_DIR_BUILD}/usb-sanitizer.png"
fi

# Create Desktop Entry
cat << 'EOF' > "${APP_DIR_BUILD}/usr/share/applications/com.sanitizer.USBSanitizer.desktop"
[Desktop Entry]
Type=Application
Name=USB Sanitizer
GenericName=Data Sanitization Tool
Comment=NIST SP 800-88 Rev. 1 Compliant Storage Sanitization Utility
Exec=usb-sanitizer %U
Icon=usb-sanitizer
Categories=System;Security;Utility;
Terminal=false
StartupWMClass=com.sanitizer.gui.AppLauncher
EOF

cp "${APP_DIR_BUILD}/usr/share/applications/com.sanitizer.USBSanitizer.desktop" "${APP_DIR_BUILD}/com.sanitizer.USBSanitizer.desktop"
ln -sf "com.sanitizer.USBSanitizer.desktop" "${APP_DIR_BUILD}/default.desktop"

# Create AppRun entrypoint script
cat << 'EOF' > "${APP_DIR_BUILD}/AppRun"
#!/usr/bin/env bash
SELF_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Check Java runtime
if command -v java &> /dev/null; then
    JAVA_EXEC="java"
elif [ -f "${SELF_DIR}/usr/lib/jvm/bin/java" ]; then
    JAVA_EXEC="${SELF_DIR}/usr/lib/jvm/bin/java"
else
    echo "ERROR: Java 17+ is required to run USB Sanitizer."
    exit 1
fi

exec "${JAVA_EXEC}" -Xmx2048m -Dfile.encoding=UTF-8 -jar "${SELF_DIR}/usr/lib/usb-sanitizer/usb-sanitizer.jar" "$@"
EOF

chmod +x "${APP_DIR_BUILD}/AppRun"

# Check for appimagetool
APPIMAGE_TOOL="appimagetool"
if ! command -v appimagetool &> /dev/null; then
    echo "appimagetool not found in PATH. Downloading standalone appimagetool..."
    mkdir -p "${PROJECT_ROOT}/build/tools"
    APPIMAGE_TOOL="${PROJECT_ROOT}/build/tools/appimagetool"
    if [ ! -f "${APPIMAGE_TOOL}" ]; then
        curl -L -o "${APPIMAGE_TOOL}" "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-${ARCH}.AppImage" || true
        chmod +x "${APPIMAGE_TOOL}" || true
    fi
fi

if command -v "${APPIMAGE_TOOL}" &> /dev/null || [ -x "${APPIMAGE_TOOL}" ]; then
    echo "Generating AppImage..."
    ARCH="${ARCH}" "${APPIMAGE_TOOL}" "${APP_DIR_BUILD}" "${DIST_DIR}/${APP_NAME}-${APP_VERSION}-${ARCH}.AppImage"
    echo "AppImage created: ${DIST_DIR}/${APP_NAME}-${APP_VERSION}-${ARCH}.AppImage"
else
    echo "Notice: appimagetool not executable on host; AppDir structure prepared at ${APP_DIR_BUILD}"
fi

echo "======================================================================"
echo " Universal Linux AppImage Build Completed!"
echo "======================================================================"
