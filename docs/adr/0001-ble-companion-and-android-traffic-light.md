# ADR 0001：Mac BLE companion 与 Android 红绿灯 v1

## 状态

已接受

## 日期

2026-05-29

## 背景

目标是在不持续盯着多个 Codex 项目的情况下，用手机快速看到 Codex 是否正在干活、近期活跃或疑似空闲/卡住。v1 需要低成本、可本机运行、方便真机验证。

## 决策

采用两端架构：

- Mac 端 Swift companion 只读 Codex 本地 SQLite 状态，并作为 BLE Peripheral 广播 compact JSON。
- Android 端 Kotlin + Jetpack Compose app 作为 BLE Central/GATT Client，扫描 `Codex Watchtower`，连接后读取状态并显示中文像素风红绿灯。
- Mac BLE 常驻运行通过 `.app` bundle 启动，而不是裸 `swift run` 可执行文件。
- Android 除 notify 外，保留 2 秒一次 characteristic read 轮询兜底。

## 依据

实现和真机验证过程中发现：

- macOS 对 CoreBluetooth 的隐私授权依赖 app bundle 身份；裸 SwiftPM executable 即使嵌入 `Info.plist`，仍可能被 TCC 归因到启动它的宿主进程并崩溃。
- 真机 BLE 扫描存在 ROM/状态差异，过滤扫描需要无过滤兜底和重试。
- BLE notify 在不同链路上不宜作为唯一刷新机制，轮询 read 能保证 UI 持续更新。

## 后果

正向影响：

- 安装 Android Studio 不是 v1 必需，命令行工具链即可构建和真机安装。
- Mac companion 不读取敏感正文，仅读取状态库。
- Android 前台 UI 能持续显示连接状态和项目列表。

取舍：

- `.app` bundle 是 Mac BLE 运行的必要分发形态，不能只依赖 `swift run`。
- v1 不是后台常驻 Android service；手机锁屏后系统行为仍可能影响可见性。
- 状态判断是启发式，不是 Codex 内部私有运行态。

## 验证

- `swift run codex-traffic-selftest`
- `swift run codex-traffic --once`
- `./scripts/build-app.sh`
- `./gradlew testDebugUnitTest assembleDebug`
- 真机无线 ADB 验证：Android 发现 BLE 外设、GATT 连接成功、读取 payload 成功、中文 UI 显示 `已连接` 和 `工作中`。
