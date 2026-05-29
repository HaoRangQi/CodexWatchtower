# ADR 0004：项目信号 HUD 与动态兜底

## 状态

已接受

## 日期

2026-05-29

## 背景

第二屏在真机上曾出现“没有动态”的反馈。排查后确认：Android 主链路已经能收到
`p/m` 项目状态，源码版 Mac companion 也能生成 `f/n` 动态字段，但正在 BLE 广播的是
旧的 `dist/Codex Traffic.app` 进程，手机连到旧 payload 时第二屏没有动态数据。

同时，“极客风格”不是黑底终端或 JSON 文本，而是偏视觉画风：需要像仪表盘、雷达、
信号面板这类监控感表达。

## 决策

- Android parser 兼容旧 payload：如果没有 `f/n` 字段，就从 `p` 项目行派生动态卡片。
- 新 companion 继续发送真实 `f/n` 字段；Android 优先使用 `f/n`，只在缺失时兜底。
- 第二屏保留 Codex avatar overlay 方向：固定原型比例内容框、左上通知托盘、右下小桌宠、纯黑背景和轻网格。
- 第二屏小桌宠复用 Codex avatar 原型帧表，按结构化动态优先级播放 running / waiting / failed / idle，动态行用呼吸状态点表达刷新感。
- 第三屏命名为 `项目信号`，用 HUD 画风展示雷达环、扫描线、信号柱、项目轨道和中文摘要。
- 第三屏不使用终端 JSON 文本作为用户界面。

## 后果

正向影响：

- 即使 Mac 端广播进程暂时是旧版本，第二屏也不会空白。
- 新旧 payload 的兼容逻辑集中在 Android parser，不污染 UI 层。
- 第三屏有独立视觉方向，和第二屏宠物 overlay 区分开。

取舍：

- 旧 payload 派生动态只能表达项目级状态，无法包含未来真实宠物事件流的额外细节。
- HUD 第三屏目前是第一版视觉壳，后续可以继续加入更细的项目健康指标。

## 验证

- Android 单元测试覆盖旧 payload 派生动态。
- Android 编译覆盖第三屏 HUD 组件。
- Swift selftest 覆盖 companion payload 编码。
- 真机日志验证 Android 持续收到 451-454 bytes 的新 payload；实机第二屏截图确认显示 `正在推进 loading`、`正在推进 shimmyUI`。
