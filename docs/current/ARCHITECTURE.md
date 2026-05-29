# 当前架构基线

## 系统边界

Codex 红绿灯 v1 由两个运行时组成：

- macOS companion：Swift CLI 代码，通过 `.app` bundle 身份运行，作为 BLE Peripheral 广播状态。
- Android app：Kotlin + Jetpack Compose，作为 BLE Central/GATT Client 扫描、连接、读取状态并显示红绿灯。

Android 工具链是共享本机依赖，放在 `/Users/macos/Downloads/AndroidToolchain`，不属于本仓库。

## 数据来源

Mac 端只读 Codex 本地 SQLite 状态：

- `~/.codex/state_5.sqlite`
- `~/.codex/goals_1.sqlite`

不读取会话正文、`history.jsonl` 或大日志。状态判断是启发式，不等同 Codex 私有内部运行态。

## BLE 契约

BLE 契约以 `PROTOCOL.md` 为准。当前 v1 使用：

- 设备名：`Codex Traffic`
- Service UUID：`4F4C0001-6C6F-6164-696E-672D636F6465`
- Status characteristic UUID：`4F4C0002-6C6F-6164-696E-672D636F6465`
- Characteristic：`read + notify`
- Payload：compact JSON，最多 480 bytes

Android 连接后请求 MTU 517；失败时继续使用普通 read。为兼容真机 BLE notify 差异，Android 端保留 2 秒一次 read 轮询兜底。

## 状态语义

- 绿：Codex 正在干活。
- 黄：最近有活动但当前不确定或等待。
- 红：空闲、Codex 未运行、blocked，或疑似卡住。

总灯优先级：任一项目绿则绿；否则任一项目黄则黄；否则红。

## Android 行为

Android UI 用户可见文案使用中文。连接状态单独显示，不混入红黄绿状态语义。

扫描策略：

1. 先按 Service UUID 过滤扫描。
2. 5 秒未命中则切无过滤扫描，在回调中按设备名或 Service UUID 校验。
3. 无过滤扫描 10 秒未命中则重启扫描循环。
4. GATT 断开或失败后自动回到扫描。

## Mac 运行方式

BLE 广播必须通过 `mac-companion/dist/Codex Traffic.app` 启动，因为 macOS 蓝牙隐私权限绑定 app bundle 身份。`swift run codex-traffic --once` 只用于调试 payload，不作为 BLE 常驻运行方式。

## 验证基线

当前 v1 验证命令：

```bash
cd mac-companion
swift run codex-traffic-selftest
swift run codex-traffic --once
./scripts/build-app.sh

cd ../android
source ../scripts/use-android-toolchain.sh
./gradlew testDebugUnitTest assembleDebug
```

真机验收使用 `ONEPLUS A6013` 无线 ADB 验证过：发现 `Codex Traffic`、GATT connected、发现 characteristic、持续收到 payload，UI 显示中文 `已连接`、`工作中`、项目列表。
