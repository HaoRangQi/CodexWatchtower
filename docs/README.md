# 文档索引

本项目文档按职责分层维护：

- `README.md`：面向使用者，记录构建、运行、验收路径。
- `docs/RUNBOOK.md`：运行与调试手册，集中维护命令、事件入口和验收清单。
- `PROTOCOL.md`：BLE 协议契约，任何字段、UUID、语义变化都先改这里。
- `docs/current/ARCHITECTURE.md`：当前架构基线，记录系统边界、数据流和运行约束。
- `docs/GOVERNANCE.md`：文档治理规范，定义触发矩阵、评审门槛和维护节奏。
- `docs/adr/README.md`：ADR 决策目录。
- `docs/adr/TEMPLATE.md`：新增 ADR 模板。
- `docs/adr/`：架构决策记录正文。
- `docs/CHANGELOG.md`：文档变更记录。

## 快速工作流

1. 先判断是否触发 `docs/GOVERNANCE.md` 中的变更触发矩阵。
2. 触发则同步更新对应文档（至少契约文档 + 架构文档）。
3. 若涉及关键取舍，新增或更新 ADR，并维护 `docs/adr/README.md` 索引。
4. 在 `docs/CHANGELOG.md` 记录本次文档更新。
5. 提交前保留验证证据，确保文档中的命令可执行。
