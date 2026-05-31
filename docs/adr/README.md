# ADR 索引

## 说明

- ADR（Architecture Decision Record）用于记录“为什么这么做”，不是重复实现细节。
- 新增 ADR 前先检查是否已有同主题记录，避免重复。
- 新 ADR 请基于 `docs/adr/TEMPLATE.md` 编写。

## 目录

- `0001-ble-companion-and-android-traffic-light.md`
  主题：Mac BLE companion 与 Android v1 双端形态。
- `0002-high-fidelity-pet-monitor-ui.md`
  主题：从基础红绿灯升级到高保真桌宠监控 UI。
- `0003-structured-pet-feed-screen.md`
  主题：第二屏结构化动态流与 UI 方向。
- `0004-hud-signal-matrix-and-feed-fallback.md`
  主题：项目信号 HUD 与旧 payload 动态兜底。
- `0005-realtime-event-spool-for-pet-feed.md`
  主题：实时事件 spool 与第二屏优先数据源。
- `0006-thread-bubble-feed-source.md`
  主题：Codex thread 气泡作为第二屏真实数据源。

## 新增流程

1. 复制 `docs/adr/TEMPLATE.md` 为下一编号文件。
2. 填写背景、决策、取舍和验证证据。
3. 更新本索引和 `docs/CHANGELOG.md`。
4. 若替代旧 ADR，在旧文件中更新状态为“已替代”，并链接新 ADR。
