#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
APP_DIR="${PROJECT_DIR}/dist/Codex Traffic.app"
CONTENTS_DIR="${APP_DIR}/Contents"
MACOS_DIR="${CONTENTS_DIR}/MacOS"
INFO_PLIST="${CONTENTS_DIR}/Info.plist"

cd "${PROJECT_DIR}"

swift build --product codex-traffic

BINARY_PATH="$(swift build --show-bin-path)/codex-traffic"
if [[ ! -x "${BINARY_PATH}" ]]; then
    echo "Built binary not found: ${BINARY_PATH}" >&2
    exit 1
fi

rm -rf "${APP_DIR}"
mkdir -p "${MACOS_DIR}"

cp "${BINARY_PATH}" "${MACOS_DIR}/codex-traffic"
chmod 755 "${MACOS_DIR}/codex-traffic"

cat > "${INFO_PLIST}" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "https://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleExecutable</key>
    <string>codex-traffic</string>
    <key>CFBundleIdentifier</key>
    <string>com.codextraffic.companion</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>Codex Traffic</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>13.0</string>
    <key>NSBluetoothAlwaysUsageDescription</key>
    <string>Codex Traffic uses Bluetooth to broadcast local Codex activity to the Android traffic light app.</string>
    <key>NSBluetoothPeripheralUsageDescription</key>
    <string>Codex Traffic uses Bluetooth to broadcast local Codex activity to the Android traffic light app.</string>
</dict>
</plist>
PLIST

codesign --force --deep --sign - "${APP_DIR}"

echo "${APP_DIR}"
