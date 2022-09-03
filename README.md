# Amarok-Hider

## Amarok 是什么？
Amarok 是一个轻量级的安卓的文件应用隐藏器，旨在保护您的隐私。  

> 很多时候，我们使用一些复杂的加密器加密个人文件。  
> 它们加密速度及慢，加密后大型文件更是占用大量储存空间，尽管确实很安全。  
> 但这些文件却实则**并非需要严格的保护，只是不希望在不经意间错误地被他人看见、发送给错误的人。**  
> 我们只是不想让他人**随便看到**并**心生好奇**。

而 Amarok 就是这样一个文件应用隐藏工具。  

## 功能
- 隐藏文件，将文件名字混淆并去除后缀  
- 隐藏应用，使应用在桌面上不可见 **(目前只支持Root隐藏)**  

**<u>请注意：Amarok 并非加密程序，而只是隐藏程序！请勿使用 Amarok 保护重要文件！</u>**

## 特点 & Todos
- [x] 一键隐藏&一键取消隐藏：文件应用，一键隐藏。
- [x] 高速大文件隐藏：只混淆文件名，文件大小并不影响隐藏速度。`FileApi` 直接调用，上千文件瞬间隐藏。
- [x] 提供快速隐藏：提供控制中心开关，随时开关隐藏状态。
- [x] 全新的 Material3 UI设计。
- [ ] 免Root应用隐藏：使用DSM进行隐藏。
- [ ] 自身图标隐藏：隐藏Amarok的启动器图标。


## 截图

![New UI](https://raw.githubusercontent.com/deltazefiro/ImageHost/master/Amarok-screenshot.jpg)


## 感谢

- [heruoxin/Icebox-SDK](https://github.com/heruoxin/IceBox-SDK), Unknown License
- [Sheep-y/Base85](https://github.com/Sheep-y/Base85/), [Apache v2](https://github.com/Sheep-y/Base85/blob/master/LICENSE) License
- [Icongeek26](https://www.flaticon.com/authors/icongeek26) & [Freepik](), For the icons


## 免责声明

<u>**Amaork 目前处于早期开发阶段，切勿用于重要文件或应用的隐藏。**</u>  
<u>**您使用 Amaork 导致的一切损失和后果均由您自行承担。**</u>

## 更新记录
### Amarok v0.7.3a3
- Breakings
    - 加入 Microsoft AppCenter 进行自动错误统计与分析。
    - 升级 Android targetSDK 到 32
- New Features
    - 加入 更多设置 界面
- Optimize
    - 优化 主界面布局
    - 优化 应用隐藏时对su命令的调用次数，减少Toast弹出数
    - 优化 应用隐藏速度
    - 优化 中文翻译
- Fix
    - 修复 设置隐藏应用 中包名太长超出显示区域
    - 修复 ShellUtil 在Logcat中刷屏
    - 修复 黑暗模式下图标暗色问题
    - 修复 文件隐藏器在隐藏过长文件名时崩溃

## Amarok v0.7.1
- Breakings
    - 优化配置储存结构 上一版本的配置文件将不可使用！
    - 增加 Android 12 支持，且停止支持 Android 9 以下的设备
- New Features
    - 增加对 Root 工作模式的支持
    - 增加自动权限请求
    - 增加中文支持
- Optimize
    - 使用 Material 3 重构UI，更加精美
    - 使用 Base64 代替 Ascii85 加密文件名称
    - 不再用 昼夜切换 描述隐藏状态，而是直接称呼为 Hidden 与 Visible
- Fix
    - 修复 QuickSetting 闪退
- Remove
    - 取消对 Icebox冰箱 工作模式的支持
    - 停用切换隐藏状态时切换昼夜主题