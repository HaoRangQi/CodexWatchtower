# Codex 桌宠

一台旧 Android 手机可以摆在旁边，当作 Codex 项目状态桌宠。macOS companion
只读本机 Codex 状态，通过 BLE 广播 compact JSON；Android app 连接后显示高保真
双目履带机器人、状态气泡和项目看板。

核心目标不是“显示一个颜色”，而是让用户不用一直盯着 Codex：桌宠会把正在推进、
近期有动静、需要介入或可能卡住的项目排出来。

视觉上优先照顾旧手机常亮场景：背景和面板尽量使用纯黑，只点亮机器人眼睛、
胸屏、连接状态和必要的项目状态提示，降低 OLED 屏耗电和烧屏风险。

## Projects

- `mac-companion/`：Swift command-line BLE peripheral for macOS。
- `android/`：Kotlin + Jetpack Compose Android app。

## Status Semantics

BLE 协议仍使用 `g/y/r` 作为紧凑状态码，Android UI 会把它们翻译成桌宠表情和中文提示：

- `g`：Codex 正在干活，桌宠显示推进中。
- `y`：近期有活动但当前不确定或等待，桌宠显示观察中。
- `r`：空闲、Codex 未运行、blocked，或疑似卡住，桌宠提示需要看一眼。

连接状态单独显示，不混入项目状态语义。

## BLE Contract

- Device name：`Codex Traffic`
- Service UUID：`4F4C0001-6C6F-6164-696E-672D636F6465`
- Status characteristic UUID：`4F4C0002-6C6F-6164-696E-672D636F6465`
- Characteristic properties：`read`、`notify`
- Payload：compact JSON，max 480 bytes。

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

1. On the Mac, run `cd mac-companion && ./scripts/build-app.sh && open "dist/Codex Traffic.app"`。
2. On Android, install and open the app。
3. Grant Bluetooth permissions。
4. Confirm the connection label changes to `已连接`。
5. Confirm the high-fidelity pet, status bubble, and project board update as Codex activity changes。
