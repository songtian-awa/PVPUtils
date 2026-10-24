---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: ffc8a205f489a8facd45506b4bbd66cd_12d517accedf11f19fe7525400de85a5
    ReservedCode1: oD4LXmlXZYZDFQfKhJJ2FO9EBgluFY0KucaEyQQOEJpmLpDGGQkJXSHd3DVMzr36El73ibU7D4Gw678YLT/J4oUbyyQX+Z0f3P47qyk+u8s9+Bqs9r/IAdXnkP8s6XZqYPedSHBnosXknxS8RVjL4a1XsB3QG8leX54MukbDkgiCcwX0fyq/jNVm7ZI=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: ffc8a205f489a8facd45506b4bbd66cd_12d517accedf11f19fe7525400de85a5
    ReservedCode2: oD4LXmlXZYZDFQfKhJJ2FO9EBgluFY0KucaEyQQOEJpmLpDGGQkJXSHd3DVMzr36El73ibU7D4Gw678YLT/J4oUbyyQX+Z0f3P47qyk+u8s9+Bqs9r/IAdXnkP8s6XZqYPedSHBnosXknxS8RVjL4a1XsB3QG8leX54MukbDkgiCcwX0fyq/jNVm7ZI=
---



# PVPUtils 插件系统整合说明

## 背景

PVPUtils 本地自定义版此前一直停留在自定义功能阶段，官方仓库中的插件系统（JS 脚本插件加载、插件管理页、插件命令）尚未接入。本次把官方插件系统完整同步到本地，让插件能力成为客户端可用的正式功能。

## 完成内容

### 依赖补齐

插件脚本运行依赖 Mozilla Rhino 脚本引擎，原先工程未声明该依赖，本次在工程依赖清单中补上 Rhino，并随最终产物一起打包，保证插件脚本在运行环境中可直接使用，无需额外安装。

### 渲染适配

官方插件系统基于独立的 Skija 渲染接口编写，本地渲染体系使用另一套 Skia 渲染实现，接口不互通。本次没有强行引入整套官方渲染框架，而是做了等价适配：

- 插件 UI 依赖的文本测量、图标、渐变、圆角、阴影等绘制能力，统一收口到本地字体与渲染管线，对外保持原方法签名，插件无需感知差异。
- 模糊背景与玻璃面板效果复用了本地已有的液体玻璃渲染能力。
- 插件独立页面从官方屏幕基类切换到本地屏幕基类，绘制逻辑原样迁移，页面交互（鼠标、键盘、滚轮）保持不变。

适配的目标是：插件开发者的写法、插件对外行为都不受影响。

### 入口注册

插件系统接入了两处用户可触达的入口：

- 命令体系：新增插件管理命令，可在游戏内通过聊天栏命令完成插件列表、启用、禁用、重载等操作。
- 设置界面：设置页新增"插件"标签页，与既有战斗、视觉、工具等页面并列，可图形化管理插件。

客户端启动时初始化插件管理器，每帧驱动插件 HUD 与脚本 tick，退出时统一清理插件运行时。

### 产物确认

最终模组包内已包含插件管理、运行时、脚本桥接、屏幕、命令、页面等全部插件相关类，Rhino 依赖随包内置，版本号维持 v1.8-beta.9 不变。

### 更新日志

游戏内更新日志与随包发布文档均已补充"官方插件系统"条目，同步移除此前关于插件能力未就绪的说明。

## 使用方式

- 在游戏内执行插件管理命令（如列出插件、加载插件目录、启用/禁用指定插件）。
- 打开设置界面，进入"插件"标签页，可查看已加载插件、刷新目录、按需启停。
- 将符合官方格式的 JS 脚本插件放入插件目录后刷新即可加载。

## 注意事项

- 插件脚本运行在受控脚本环境中，存在执行时间与资源限制，异常脚本会被隔离并标记失败，不影响客户端主体运行。
- 插件 API 与官方 PVPUtils 保持一致，已有官方插件脚本无需修改即可加载。
