#!/usr/bin/env bash
# ==============================================================================
# USB Sanitizer — Flatpak Builder & Bundle Packager
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
FLATPAK_DIR="${SCRIPT_DIR}/flatpak"
DIST_DIR="${PROJECT_ROOT}/dist"
BUILD_DIR="${PROJECT_ROOT}/build/flatpak-build"
REPO_DIR="${PROJECT_ROOT}/build/flatpak-repo"

APP_ID="com.sanitizer.USBSanitizer"
APP_VERSION="${1:-1.0.0}"

echo "======================================================================"
echo "  Building Flatpak Package for ${APP_ID} v${APP_VERSION}"
echo "======================================================================"

if ! command -v flatpak-builder &> /dev/null; then
    echo "Notice: 'flatpak-builder' not found on system. Ensure Flatpak SDK is installed."
    echo "Skipping local Flatpak bundle generation. CI runner with flatpak-builder will execute this."
    exit 0
fi

mkdir -p "${DIST_DIR}" "${BUILD_DIR}" "${REPO_DIR}"

cd "${FLATPAK_DIR}"

flatpak-builder --force-clean --repo="${REPO_DIR}" "${BUILD_DIR}" "${APP_ID}.yml"
flatpak build-bundle "${REPO_DIR}" "${DIST_DIR}/${APP_ID}-${APP_VERSION}.flatpak" "${APP_ID}"

echo "======================================================================"
echo " Flatpak Bundle created: ${DIST_DIR}/${APP_ID}-${APP_VERSION}.flatpak"
echo "======================================================================"
