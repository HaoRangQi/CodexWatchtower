# ADR 0003：第二屏使用结构化宠物动态

## 状态

已接受

## 日期

2026-05-29

## 背景

用户希望第一屏先保持当前桌宠监控，向左滑动进入第二屏，第二屏展示“Codex 宠物输出什么就展示什么”。Codex app 的桌宠原型有活动通知和状态卡片，但当前 companion 的安全边界是只读 SQLite 结构化状态，不读取 `history.jsonl`、完整会话正文或大日志。

如果 v1 直接同步原始会话输出，需要重新确认隐私边界、数据源稳定性和 BLE 分片策略。当前 BLE characteristic 仍限制 compact JSON 约 480 bytes，适合传短状态卡片，不适合传完整输出流。

## 决策

第二屏先实现为 `宠物动态`：

- Android 主 UI 使用 `HorizontalPager`，第一屏保留桌宠监控，向左滑进入第二屏。
- Mac companion 从现有 `ProjectStatus` 生成结构化动态，例如正在推进、刚有动静、暂时安静、可能卡住、blocked、Codex 不在线。
- BLE payload 增加可选字段 `f` 和 `n`：
  - `f`：动态行 `[projectId, title, body, light, ageSeconds, reason]`
  - `n`：因 480 bytes 限制被截断的动态数量
- Android 解析 `f/n`，第二屏按关注优先级和最近活动排序展示动态。
- Android 兼容旧 payload：没有 `f/n` 时从 `p` 项目行派生等价动态，避免第二屏空白。
- 手机端隐藏项目时，同步过滤第二屏动态，避免隐藏项目在动态页继续出现。
- 仍不读取会话正文、`history.jsonl` 或大日志。

## 后果

正向影响：

- 第二屏已经有可用的动态内容，能表达项目推进、等待、阻塞和卡住风险。
- 保持当前隐私边界和 BLE 简单契约，不需要新增 characteristic 或分片协议。
- Android 端可以继续兼容旧 payload：没有 `f/n` 时第二屏仍显示派生动态。

取舍：

- 这不是 Codex app 宠物原始通知流的完整镜像。
- 480 bytes 会导致动态数量很少，项目多时优先保留更靠前的项目动态。
- 后续如果要展示真实对话输出，需要单独设计数据源、权限提示和更大的传输通道。

## 验证

- Swift selftest 覆盖 `f/n` 编码和动态截断。
- Android parser 单元测试覆盖动态解析。
- Android ViewModel 测试覆盖隐藏项目时过滤动态。
- Android Compose 真机测试覆盖左滑进入第二屏并显示动态文本。
