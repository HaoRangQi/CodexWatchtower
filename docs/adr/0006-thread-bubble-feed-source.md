# ADR 0006：Codex thread 气泡作为第二屏真实数据源

## 状态

已接受

## 日期

2026-05-31

## 背景

第二屏需要显示真实、实时、可读的 Codex 动态。仅靠 SQLite 状态合成可以判断项目是否近期变化，
但用户仍看不到 Codex App 首页气泡里的标题、摘要和最近输出；仅靠 `events.jsonl` 又要求外部 hook
足够完整，覆盖面不稳定。

Codex App 已在本机 thread 记录里暴露气泡信息：`threads.title`、`threads.preview`、`threads.rollout_path`。
这些字段能提供比状态合成更贴近用户看到的“气泡卡片”的内容。

## 决策

Mac companion 的第二屏动态数据源按以下优先级合并：

1. `~/.codex-traffic/events.jsonl` 的 hook 或手动事件。
2. Codex App thread 气泡数据：`~/.codex/state_5.sqlite` 的 `threads.title`、`threads.preview`、`threads.rollout_path`。
3. `rollout_path` 指向的 thread JSONL 最近事件，解析 user、agent、tool、patch 和 task complete 等公开事件。
4. SQLite snapshot 合成动态，作为没有气泡内容时的兜底。

companion 不读取隐藏思维，不读取 `history.jsonl`，不做截图 OCR。`rollout_path` 只尾读有限字节，生成短标题和短正文，
继续通过现有 BLE compact payload 的 `f/n` 字段发送给 Android。

## 依据

- `swift run codex-traffic --once` 已能输出真实 agent 文本到 `f` 动态行。
- Swift selftest 覆盖 rollout agent message 和 preview fallback。
- Android 端无需改协议，已有 parser 继续消费 `f/n`。
- 相比 OCR 或截图解析，thread 结构化记录更稳定、更省电，也更容易裁剪到 480 bytes payload。

## 后果

正向影响：

- 第二屏不再只是状态合成或占位，能显示 Codex App 气泡和最近 agent 输出。
- 不增加 Android 传输复杂度，BLE/HTTP fallback 都沿用同一 payload。
- 数据边界清晰：只读结构化 thread 记录和有限 rollout 尾部。

取舍：

- 这仍不是 Codex 内部私有运行态，也不是完整聊天记录镜像。
- Codex App 本地 schema 如果变化，需要继续做兼容处理。
- payload 仍受 480 bytes 限制，长文本会被裁剪，项目列表可能让位给动态流。

## 验证

- `cd mac-companion && swift run codex-traffic-selftest`
- `cd mac-companion && swift run codex-traffic --once`
- `source scripts/use-android-toolchain.sh && cd android && ./gradlew testDebugUnitTest`
- 真机截图已更新到 `pics/` 与 `docs/images/`。
