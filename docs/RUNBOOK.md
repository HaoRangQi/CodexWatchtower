# 运行与调试手册

## 工具链准备

本仓库默认使用共享 Android 命令行工具链：

```text
/Users/macos/Downloads/AndroidToolchain
```

进入开发前先执行：

```bash
source scripts/use-android-toolchain.sh
```

如需同步 Codex 原版 BSOD 桌宠资源：

```bash
scripts/sync-codex-bsod-asset.sh
```

该脚本会把资源写入 `android/app/src/main/assets/codex_bsod_spritesheet.webp`（本机文件，不提交）。

## 启动 Mac companion

### 正式 BLE 运行方式（推荐）

```bash
cd mac-companion
./scripts/build-app.sh
open "dist/Codex Watchtower.app"
```

说明：macOS 蓝牙权限绑定 app bundle 身份，正式广播建议始终使用 `.app` 启动。

### 命令行调试方式

```bash
cd mac-companion
swift run codex-traffic-selftest
swift run codex-traffic --once
swift run codex-traffic --no-ble --http-port 8765
```

- `--once`：只输出一次 payload 并退出。
- `--no-ble`：只开 HTTP fallback，避免终端进程触发蓝牙隐私崩溃。

## 实时事件入口

第二屏动态的数据源优先级：

1. `~/.codex-traffic/events.jsonl` 中的 Hook 或手动实时事件。
2. Codex App thread 气泡记录：`~/.codex/state_5.sqlite` 的 `threads.title` / `preview` / `rollout_path`，以及 `rollout_path` 指向的最近 thread JSONL 事件。
3. SQLite 状态兜底：线程更新时间、agent job、goal blocked、stale 或 Codex 离线。

实时事件文件：

```text
~/.codex-traffic/events.jsonl
```

写入事件示例：

```bash
scripts/codex-traffic-event.sh running "Codex 正在运行" "loading 有新动作"
scripts/codex-traffic-event.sh waiting_input "等待你回复" "Codex 需要用户输入"
scripts/codex-traffic-event.sh permission_required "等待授权" "需要批准命令或权限"
scripts/codex-traffic-event.sh network_stall "网络可能卡住" "长时间没有新 token 或状态变化"
scripts/codex-traffic-event.sh completed "任务完成" "回到项目确认结果"
scripts/codex-traffic-event.sh failed "任务失败" "需要检查错误"
```

可选 ntfy 转发：

```bash
CODEX_TRAFFIC_NTFY_TOPIC="your-topic" \
  scripts/codex-traffic-event.sh completed "Codex 任务完成" "回到项目确认结果"
```

如需对接现有 Codex `notify`：

```toml
notify = ["/Users/macos/Downloads/Projects/loading/scripts/codex-traffic-notify-wrapper.sh", "turn-ended"]
```

如需对接新版 hooks：

```text
/Users/macos/Downloads/Projects/loading/scripts/codex-traffic-hook.sh
```

## 构建并安装 Android

```bash
source scripts/use-android-toolchain.sh
cd android
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew installDebug
```

调试包路径：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## 手动验收清单

1. Mac 端启动 companion，手机端能发现并连接 `Codex Watchtower`。
2. 手机右上角连接状态显示 `已连接`。
3. 第一屏能看到桌宠状态和项目列表持续刷新。
4. 左滑第二屏能看到动态卡片，优先显示 Codex thread 气泡和最近 agent 输出，不再只是 SQLite 合成动态。
5. 再左滑第三屏能看到 `项目信号` HUD。
6. 手机端隐藏/恢复项目后，列表和状态汇总按预期变化。

## 关联文档

- 协议字段：`PROTOCOL.md`
- 架构说明：`docs/current/ARCHITECTURE.md`
- ADR 决策：`docs/adr/README.md`
