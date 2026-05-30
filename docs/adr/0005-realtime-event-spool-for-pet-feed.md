# ADR 0005：实时事件 spool 作为第二屏优先数据源

## 状态

已接受

## 日期

2026-05-30

## 背景

第二屏只从 SQLite 结构化状态派生动态时，能看到项目最近是否更新、是否 blocked 或 stale，
但看不到更贴近用户痛点的实时状态：Codex 可能在等权限、等用户输入、任务刚完成、失败，
或网络长时间无响应。Codex 本地 SQLite 当前没有这些细粒度等待状态的稳定表。

用户调研到 `ntfy.sh`、Apprise、Codex notify hook 等方案。这些方案的共同点是事件驱动：
在 Codex 到达某个状态点时推送短消息，而不是继续从日志里猜。

## 决策

Mac companion 新增本机实时事件 spool：

- 默认路径：`~/.codex-traffic/events.jsonl`
- 每行是 compact JSON，包含 `v`、`ts`、`kind`、`cwd`、`title`、`body`
- 支持 `running`、`waiting_input`、`permission_required`、`completed`、`failed`、
  `network_stall`、`message`
- companion 每 2 秒读取最近 15 分钟事件，并优先把事件转为 BLE payload 的 `f/n` 动态行
- companion 同时从 SQLite 结构化状态合成动态，覆盖正在推进、近期更新、目标 blocked、
  agent job stale、Codex 进程不在线等状态
- payload 空间紧张时，编码器优先保留最多 3 条动态，再裁剪项目列表，避免第二屏只剩项目行
- `scripts/codex-traffic-event.sh` 作为通用写入入口，可选通过 `CODEX_TRAFFIC_NTFY_TOPIC`
  同步发到 ntfy
- `scripts/codex-traffic-notify-wrapper.sh` 作为现有 `notify` 的兼容 wrapper，先写事件再转发
  到原有 Codex Computer Use 通知程序
- `scripts/codex-traffic-hook.sh` 作为新版 Codex hooks 的通用 command，从 hook stdin JSON 中
  识别 permission、request user input、stop、session start 等事件并写入同一 spool

## 依据

- 事件 spool 能表达等待输入、等待授权、完成、失败、网络疑似卡住这些 SQLite 无法可靠推断的状态。
- 事件 payload 很短，适合现有 480 bytes BLE characteristic，不需要立即设计新传输通道。
- 本地 JSONL 入口可以被 Codex hook、shell、Hammerspoon、Apprise、ntfy 或自动化系统复用。
- SQLite 合成动态保证即使没有细粒度 hook，第二屏也能显示本机真实状态，而不是静态占位。
- 继续不读取 `history.jsonl`、完整会话正文或大日志，隐私边界清晰。

## 后果

正向影响：

- 第二屏有真实实时事件，不再只是“项目最近更新”的派生卡片。
- Android 端无需改 BLE 连接层，继续消费 `f/n`。
- 用户可以用 ntfy 获取系统级手机推送，同时保留本项目 Android 桌宠的 BLE 近场展示。

取舍：

- v1 仍不能自动覆盖所有 Codex 内部状态；hook 或脚本写入的事件优先级最高，SQLite 合成动态
  是结构化启发式。
- `notify` 旧接口通常只在 turn end 触发，无法天然覆盖“正在运行”的连续流；这类事件需要更细的 hook 或外部监控脚本写入。
- spool 是本机追加文件，需要后续按体积做清理或轮转。

## 验证

- Swift selftest 覆盖 JSONL 事件解析、坏行忽略、实时事件覆盖派生状态。
- `swift run codex-traffic --once` 可直接看到事件进入 `f/n` payload。
- Android parser 既兼容新 `f/n`，也保留旧 payload 派生兜底。
