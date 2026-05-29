# Codex Traffic Light

Pixel-style Android traffic light for monitoring local Codex activity from a
phone. A macOS companion reads local Codex state and broadcasts a compact BLE
status payload. The Android app connects to that BLE service and renders a
green/yellow/red project overview.

## Projects

- `mac-companion/`: Swift command-line BLE peripheral for macOS.
- `android/`: Kotlin + Jetpack Compose Android app.

## Status Semantics

- Green: Codex is doing work.
- Yellow: recent activity exists, but the current state is uncertain or waiting.
- Red: idle, Codex is not running, blocked, or a running job looks stale.

## BLE Contract

- Device name: `Codex Traffic`
- Service UUID: `4F4C0001-6C6F-6164-696E-672D636F6465`
- Status characteristic UUID: `4F4C0002-6C6F-6164-696E-672D636F6465`
- Characteristic properties: `read`, `notify`
- Payload: compact JSON, max 480 bytes.

```json
{
  "v": 1,
  "t": 1780039000,
  "o": "g",
  "p": [
    ["a1b2c3d4", "loading", "g", 4, "work"]
  ],
  "m": 0
}
```

## Build Notes

This workspace uses a shared command-line Android toolchain:

```text
/Users/macos/Downloads/AndroidToolchain
```

It contains JDK 17, Android SDK command-line tools, Android platform/build tools,
platform-tools, and Gradle 9.1.0. To use it in a shell:

```bash
source scripts/use-android-toolchain.sh
```

## Run the macOS Companion

```bash
cd mac-companion
swift run codex-traffic-selftest
swift run codex-traffic --once
./scripts/build-app.sh
open "dist/Codex Traffic.app"
```

`--once` prints one compact JSON payload and exits. Use the `.app` bundle for
BLE advertising because macOS Bluetooth privacy permission is granted to the app
bundle identity. The companion updates every 2 seconds by default.

## Run the Android App

From this project:

```bash
source scripts/use-android-toolchain.sh
cd android
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew installDebug
```

The app scans for the `Codex Traffic` BLE service, connects, requests MTU 517,
reads the status characteristic, and subscribes to notifications.

The debug APK is generated at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Manual Acceptance

1. On the Mac, run `cd mac-companion && ./scripts/build-app.sh && open "dist/Codex Traffic.app"`.
2. On Android, install and open the app.
3. Grant Bluetooth permissions.
4. Confirm the connection label changes to `CONNECTED`.
5. Confirm the top traffic light and project list update as Codex activity
   changes.
