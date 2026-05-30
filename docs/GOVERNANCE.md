# 文档治理规范

## 目标

- 保证文档与当前实现一致，避免“代码已变，文档失效”。
- 明确每类信息的唯一权威来源（single source of truth）。
- 将文档更新纳入交付定义（Definition of Done），不是收尾可选项。

## 文档分层与权威来源

1. `README.md`：
   面向使用者和调试者，覆盖运行方式、构建路径、手动验收。
2. `PROTOCOL.md`：
   BLE 与 payload 协议契约唯一权威来源。
3. `docs/current/ARCHITECTURE.md`：
   当前架构基线，记录系统边界、数据流、运行约束、关键假设。
4. `docs/adr/*.md`：
   关键决策的背景、取舍与后果。`docs/adr/README.md` 为决策目录。
5. `docs/CHANGELOG.md`：
   文档层变更记录，追踪“谁在什么时候更新了什么”。

## 变更触发矩阵

1. 协议字段、UUID、状态码、截断策略变化：
   必改 `PROTOCOL.md`，并同步 `README.md` 与 `ARCHITECTURE.md`。
2. Mac/Android 连接策略、权限、fallback 策略变化：
   必改 `ARCHITECTURE.md`，必要时新增 ADR。
3. UI 信息架构或状态语义变化：
   必改 `README.md` 与 `ARCHITECTURE.md`，涉及取舍时补 ADR。
4. 脚本参数、调试命令、运行入口变化：
   必改 `README.md`，涉及长期约束时补 `ARCHITECTURE.md`。

## ADR 治理规则

1. 文件命名：
   `NNNN-<kebab-case-title>.md`，例如 `0006-http-fallback-discovery.md`。
2. 状态字段：
   `已接受`、`已废弃`、`已替代` 三选一，状态变化必须写明替代项。
3. 内容最小结构：
   状态、日期、背景、决策、依据、后果、验证。
4. 变更方式：
   原 ADR 只追加状态与链接，不覆盖历史语义。

## 合并前文档检查（PR Checklist）

1. 本次代码变更是否触发“变更触发矩阵”中的任一条。
2. 触发则必须同步对应文档，不允许“下个 PR 再补”。
3. 所有新命令都经过本地验证，并在文档中给出可执行示例。
4. 引用路径必须为当前仓库真实路径，不使用过期目录名。
5. 在 `docs/CHANGELOG.md` 追加一条记录，说明本次文档更新范围。

## 维护节奏

1. 每次功能合并时执行文档检查。
2. 每周至少一次快速巡检以下文件：
   `README.md`、`PROTOCOL.md`、`ARCHITECTURE.md`、最新 ADR。
3. 发现文档与实现不一致时，优先修正文档；若实现偏离契约则先修实现或回滚。
