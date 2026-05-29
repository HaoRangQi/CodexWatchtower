# Codex 桌宠

一台旧 Android 手机可以摆在旁边，当作 Codex 项目状态桌宠。macOS companion
只读本机 Codex 状态，通过 BLE 广播 compact JSON；Android app 连接后显示高保真
BSOD 蓝屏小机器人、极简状态短句、项目列表和宠物动态。

核心目标不是“显示一个颜色”，而是让用户不用一直盯着 Codex：桌宠会把正在推进、
近期有动静、需要介入或可能卡住的项目排出来。

手机端可以直接隐藏不想看的项目；隐藏列表保存在 Android 本机，不会写回 Mac 或
改变 BLE payload。被隐藏项目不再参与桌宠总状态，底部入口可随时恢复。
向左滑动进入第二屏，可以看 companion 生成的宠物动态：它展示项目是否正在推进、
刚有动静、疑似卡住、blocked 或 Codex 离线。当前动态只来自 SQLite 结构化状态字段，
不读取完整会话正文、`history.jsonl` 或大日志。

视觉上优先照顾旧手机常亮场景：背景尽量使用纯黑，减少标题、方框和长说明，
只点亮蓝色屏幕脸、胸屏、连接状态和必要的项目状态提示，降低 OLED 屏耗电和烧屏风险。

Android 会优先加载本机同步的 Codex BSOD spritesheet。该资源来自本机
`/Applications/Codex.app`，不提交进仓库；缺失时 app 会回退到内置绘制版本。
桌宠动画复用 Codex avatar 原型的 8×9 spritesheet 帧序列，按推进中、刚动过、
需要看一眼、待机和离线状态切换 running / waiting / failed / idle / waving 动作。

## Projects

- `mac-companion/`：Swift command-line BLE peripheral for macOS。
- `android/`：Kotlin + Jetpack Compose Android app。

## Status Semantics

BLE 协议仍使用 `g/y/r` 作为紧凑状态码，Android UI 会把它们翻译成桌宠表情和中文提示：

- `g`：Codex 正在干活，桌宠显示推进中。
- `y`：近期有活动但当前没有新进展，桌宠显示刚动过。
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
  "m": 0,
  "f": [
    ["a1b2c3d4", "正在推进 loading", "4 秒内有新动作", "g", 4, "work"]
  ],
  "n": 0
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

如需使用 Codex 原版 BSOD 桌宠资源，先运行：

```bash
scripts/sync-codex-bsod-asset.sh
```

脚本会从本机 Codex app 的 `app.asar` 提取 `bsod-spritesheet-v4-*.webp` 到
`android/app/src/main/assets/codex_bsod_spritesheet.webp`。该文件被 `.gitignore`
忽略，只用于本机打包。

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
5. Confirm the high-fidelity pet, compact status text, and project list update as Codex activity changes。
6. Swipe left and confirm the second screen shows `宠物动态`。
7. Tap `隐藏` on a project, then use `已隐藏 N 项 · 点此恢复` to restore it。
