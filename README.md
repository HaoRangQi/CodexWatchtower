# Codex 守望台 / Codex Watchtower

把一台旧 Android 手机变成 Codex 项目状态守望台。  
Mac companion 读取本机 Codex 结构化状态并广播，Android 展示桌宠、动态流和项目信号 HUD，帮助你在多项目并行时快速判断是否需要介入。

## 30 秒启动

1. 启动 Mac companion（BLE 正式运行）：
   `cd mac-companion && ./scripts/build-app.sh && open "dist/Codex Watchtower.app"`
2. 手机安装调试包：
   `source scripts/use-android-toolchain.sh && cd android && ./gradlew installDebug`
3. 手机打开 app，授予蓝牙权限，确认右上角显示 `已连接`。

## 三屏预览

| 第一屏：桌宠总览 | 第二屏：宠物动态 | 第三屏：项目信号 HUD |
| --- | --- | --- |
| <img src="pics/screen-1-main.png" alt="第一屏：桌宠总览" width="32%"> | <img src="pics/screen-2-feed.png" alt="第二屏：宠物动态" width="32%"> | <img src="pics/screen-3-geek.png" alt="第三屏：项目信号 HUD" width="32%"> |

## 文档入口

- 文档导航：`docs/README.md`
- 运行与调试手册：`docs/RUNBOOK.md`
- 协议契约：`PROTOCOL.md`
- 架构基线：`docs/current/ARCHITECTURE.md`
- 文档治理：`docs/GOVERNANCE.md`
- ADR 索引：`docs/adr/README.md`

## 致谢

致谢：vibe by codex。

## 二期

二期：待施工。
