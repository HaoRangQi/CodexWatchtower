# 文档索引

本项目文档按职责分层维护：

- `README.md`：面向使用者，记录构建、运行、验收路径。
- `PROTOCOL.md`：BLE 协议契约，任何字段、UUID、语义变化都先改这里。
- `docs/current/ARCHITECTURE.md`：当前架构基线，记录系统边界、数据流和运行约束。
- `docs/adr/`：架构决策记录，记录已经执行且后续维护者需要理解的取舍。

## 治理规则

1. 修改 BLE payload、UUID、颜色语义时，同步更新 `PROTOCOL.md` 和架构基线。
2. 修改 Android 权限、扫描策略、Mac 分发方式时，补充或更新 ADR。
3. 提交前运行对应验证命令，并在提交信息或后续记录中保留证据。
4. 不提交本机工具链、SDK 路径、构建产物和 `.app` bundle。
