#!/usr/bin/env bash
# ==============================================================================
# USB Sanitizer — Cross-Platform Packaging Orchestrator
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

VERSION="${1:-1.0.0}"

echo "======================================================================"
echo " USB Sanitizer Packaging Orchestrator"
echo " Target Version: ${VERSION}"
echo "======================================================================"

OS_TYPE="$(uname -s)"

case "${OS_TYPE}" in
    Darwin*)
        echo "Detected Host OS: macOS"
        chmod +x "${SCRIPT_DIR}/package-mac.sh"
        "${SCRIPT_DIR}/package-mac.sh" "${VERSION}"
        ;;
    Linux*)
        echo "Detected Host OS: Linux"
        chmod +x "${SCRIPT_DIR}/package-linux.sh"
        "${SCRIPT_DIR}/package-linux.sh" "${VERSION}"
        ;;
    MINGW*|MSYS*|CYGWIN*)
        echo "Detected Host OS: Windows (MinGW/Cygwin/MSYS)"
        "${SCRIPT_DIR}/package-win.bat" "${VERSION}"
        ;;
    *)
        echo "Unsupported OS: ${OS_TYPE}"
        exit 1
        ;;
esac
