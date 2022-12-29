# Amarok-Hider

![poster](https://raw.githubusercontent.com/deltazefiro/ImageHost/master/amarok-169-poster.png)

![version](https://img.shields.io/visual-studio-app-center/releases/version/deltazefiro/Amarok/2e57e3f726f6bdf0b9bd5e3791bd2c5d1ab1dbe2)
![tag](https://img.shields.io/github/v/tag/deltazefiro/Amarok-Hider)
![commit-freq](https://img.shields.io/github/commit-activity/m/deltazefiro/Amarok-Hider)
## Amarok 是什么？
Amarok 是一个轻量级的安卓的文件应用隐藏器，旨在保护您的隐私。  

> 很多时候，我们使用一些复杂的加密器加密个人文件。  
> 它们加密速度慢，加密大型文件更是令人头疼，尽管确实很安全。  
> 但我们的文件其实**根本不需要严格的保护，只是不希望不小心被别人看见，或手滑发给错误的人。**  
> 我们只需要一个工具，不让它们被**随便看到**，使别人**心生好奇**。

而 Amarok 就是这样一个隐藏文件、应用的工具。  

## 功能
- 隐藏文件，将混淆文件名、文件头  
- 免Root隐藏应用，使应用在桌面上不可见  

**<u>请注意：Amarok 并非加密程序，而只是隐藏程序！请勿使用 Amarok 保护重要文件！</u>**

## 特点 & Todos
- 一键隐藏&一键取消隐藏：文件应用，一键隐藏。
- 高速大文件隐藏：混淆文件名与文件头，轻松隐藏视频、图像等大文件。
- 提供快速隐藏：提供控制中心开关，随时开关隐藏状态。
- 全新的 Material3 UI设计。
- 免Root应用隐藏：Root、Shizuku、DSM等多种隐藏模式。
- ~~自身图标隐藏：隐藏Amarok的启动器图标。~~


## 使用文档
[![](https://img.shields.io/badge/Amarok%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3-%E7%82%B9%E5%87%BB%E6%9F%A5%E7%9C%8B-brightgreen)](https://deltazefiro.github.io/Amarok-doc/)  


## 下载
[![](https://img.shields.io/github/v/release/deltazefiro/Amarok-Hider?include_prereleases&label=Github%20Release)](https://github.com/deltazefiro/Amarok-Hider/releases)  
[![](https://img.shields.io/visual-studio-app-center/releases/version/deltazefiro/Amarok/2e57e3f726f6bdf0b9bd5e3791bd2c5d1ab1dbe2?color=blue&label=AppCenter)](https://install.appcenter.ms/users/deltazefiro/apps/amarok/distribution_groups/public)  
[![](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/deltazero.amarok.foss)](https://apt.izzysoft.de/fdroid/index/apk/deltazero.amarok.foss)  



## 截图

![Screenshot](https://raw.githubusercontent.com/deltazefiro/ImageHost/master/amarok-169-1.png)
![Screenshot](https://raw.githubusercontent.com/deltazefiro/ImageHost/master/amarok-169-2.png)
![Screenshot](https://raw.githubusercontent.com/deltazefiro/ImageHost/master/amarok-169-3.png)
![Screenshot](https://raw.githubusercontent.com/deltazefiro/ImageHost/master/amarok-169-4.png)


## 感谢

- [heruoxin/Icebox-SDK](https://github.com/heruoxin/IceBox-SDK), Unknown License
- [Sheep-y/Base85](https://github.com/Sheep-y/Base85/), [Apache v2](https://github.com/Sheep-y/Base85/blob/master/LICENSE) License
- [Icongeek26](https://www.flaticon.com/authors/icongeek26) & [Freepik](), For the icons


## 免责声明

<u>**请您切勿将 Amaork 用于重要文件或应用的隐藏。**</u>  
<u>**您使用 Amaork 导致的一切损失和后果均由您自行承担。**</u>

## 更新记录

### Amarok v0.8.1b1

- Breakings
    - **此版本非稳定版本，未经过测试，请谨慎使用**
    - **新增 `-foss` 后缀版本，用于 F-droid 分发。此版本无自动更新等网络功能。**
- New Features
    - **新增 混淆文件头功能** (#3)
    - **新增 应用隐藏模式 选择界面**
    - 新增 隐藏后的文件以 `.` 开头 (#4)
    - 新增 每次启动时检测应用隐藏器权限
- Optimize
    - 重构 应用隐藏器选择逻辑
    - 优化 文件隐藏速度
    - 优化 中英文翻译
    - 升级 AppCenter & Shizuku API 版本
- Fix
    - 移除 已停止使用的 `android.software.leanback` 权限 (#5)

### Amarok v0.7.4a5

- Breakings
    - **这是 Amarok 的最后一个 `alpha` 版本  欢迎进入 `beta` 阶段！** :tada: :tada: 
    - **加入 AppCenter 应用内升级功能，未来将会自动更新**
    - **Amarok 的图标由 月亮 更换为 爪印形**
- New Features
    - **支持 DSM 免 root 隐藏** ***[尚未测试]***
    - **支持同时隐藏多个目录**
- Optimize
    - 优化 `SharedPreferance` 储存方式
    - 优化 对于打开非本地文件是的反馈提示
    - 优化 中英文翻译
- Fix
    - 修复 暗色模式下状态栏颜色与 `AppBar` 不一致
    - 修复 亮色模式下状态栏文字颜色为白色
    - 修复 选择非本地储存中文件后崩溃 **(在 HarmonyOS 下隐藏文件崩溃的原因)**

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

### Amarok v0.7.1
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