# 醒醒闹钟 · WakeCircuit

**WakeCircuit** 是一款使用 Kotlin 与 Jetpack Compose 开发的原生 Android 任务闹钟。它将按时响铃与连续唤醒任务结合：用户需要完成随机微积分题、拍照物品核验和英语跟读，才能走到应用内的停止铃声步骤。项目面向 Android 8.0 及以上设备，当前以 Android 15 为主要编译与测试目标。

> **适用定位**：这是一个强调任务驱动唤醒流程的 Android 项目。它不会、也不应绕过 Android 的系统静音、强行停止、卸载或特殊权限控制；这些系统级行为始终由设备与用户管理。

| 项目项 | 内容 |

| --- | --- |
| 应用包名 | `com.wakecircuit.alarm` |
| 最低系统版本 | Android 8.0（API 26） |
| 编译与目标 SDK | Android 15（API 35） |
| 主要语言 | Kotlin 2.0.21 |
| UI 框架 | Jetpack Compose 与 Material 3 |
| 构建工具 | Android Gradle Plugin 8.8.2、Gradle 8.11.1、JDK 17 |
| 当前版本 | `1.0.0` |
| 文档维护 | Manus AI · 2026-08-15 |

## 核心功能

WakeCircuit 将一次闹钟触发拆分为三个连续阶段。闹钟触发后，铃声由前台服务维持播放，并进入全屏任务界面；每个阶段完成后才会进入下一阶段。完成全部任务后，用户可在完成界面停止铃声。

| 阶段 | 实现方式 | 目的 |
| --- | --- | --- |
| 1. 数学唤醒 | 随机生成微积分问题并校验答案 | 通过计算提升即时注意力。 |
| 2. 物品核验 | 调用后置相机拍照，再通过兼容 Chat Completions 图片输入的视觉服务校验目标物品 | 促使用户离开床边并与真实环境交互。 |
| 3. 英语跟读 | 调用 Android 语音识别，按文本相似度判断随机英文句子的跟读结果 | 通过发声与语言复述进一步确认清醒状态。 |

应用同时提供闹钟时间选择、启用开关、铃声选择和联网识图设置。闹钟会在触发后为次日重新注册；设备重启或应用更新后，接收器会尝试恢复已启用的闹钟。

## 任务流程

```text
设置时间并启用闹钟
        ↓
到点触发：通知 + 前台播放服务 + 全屏任务页
        ↓
随机数学题 → 拍照识别指定物品 → 英语跟读
        ↓
完成全部阶段 → 点击停止铃声
```

在正常使用流程中，请在闹钟触发前完成联网识图配置，并在首次使用时按系统提示授予必要权限。精确闹钟和全屏通知均受到 Android 与设备厂商策略约束；权限被关闭时，闹钟展示或准时性可能受影响。[1] [2]

## 项目结构

```text
WakeCircuit/
├── app/
│   ├── src/main/java/com/wakecircuit/alarm/
│   │   ├── MainActivity.kt          # 主界面、时间与设置
│   │   ├── AlarmActivity.kt         # 任务式全屏闹钟流程
│   │   ├── AlarmStore.kt            # 本地配置存储与调度
│   │   ├── AlarmComponents.kt       # 接收器、前台铃声服务、重启恢复
│   │   ├── ChallengeEngine.kt       # 数学题与英语跟读题库、判定逻辑
│   │   ├── VisionVerifier.kt        # 照片压缩、方向修正与视觉服务调用
│   │   └── WakeTheme.kt             # Compose 主题
│   └── src/main/AndroidManifest.xml # 权限与组件声明
├── gradle/wrapper/                  # Gradle Wrapper
├── build.gradle.kts                 # 根项目构建配置
└── README.md
```

## 快速开始

### 前置条件

请安装 **Android Studio**（推荐使用当前稳定版），并准备 Android SDK Platform 35、Build Tools 与 **JDK 17**。项目已提交 Gradle Wrapper，因此无需预先全局安装 Gradle。真机测试时，建议使用具有相机、麦克风、网络连接与通知能力的 Android 设备。

### 获取并构建

```bash
git clone https://github.com/xhslam/WakeCircuit.git
cd WakeCircuit

# macOS / Linux
./gradlew assembleDebug

# Windows PowerShell
./gradlew.bat assembleDebug
```

首次构建会下载 Android 和 Kotlin 依赖。成功后，调试安装包默认位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

也可以连接已开启 USB 调试的设备后直接安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 本地 SDK 配置

Android Studio 通常会自动生成 `local.properties`。若从命令行构建，请在项目根目录创建该文件并填入本机 SDK 路径；该文件已在 `.gitignore` 中排除，不能提交到仓库。

```properties
sdk.dir=C:\\Users\\<用户名>\\AppData\\Local\\Android\\Sdk
```

## 使用说明

首次打开应用后，先选择响铃时间与铃声。启用闹钟时，请根据系统页面确认精确闹钟、通知和全屏通知权限；随后打开 **配置联网识图**，填写服务地址、访问密钥以及能够处理图片输入的模型名称。配置完成后再启用闹钟，以免流程在拍照核验阶段因缺少网络服务配置而无法完成。

| 操作 | 说明 |
| --- | --- |
| 设置时间 | 通过时间选择器保存每日触发时间。 |
| 选择铃声 | 调起系统铃声选择器，保存所选 URI。 |
| 启用闹钟 | 注册精确的下一次触发时间；触发后自动安排次日。 |
| 配置联网识图 | 填入兼容 Chat Completions 图片输入的服务基础地址、API 密钥与模型名。应用会将请求发送至 `/chat/completions`。 |
| 完成唤醒任务 | 按顺序完成数学、拍照和英语跟读，然后在完成页停止铃声。 |

## 权限与隐私

WakeCircuit 仅在相应功能被使用时请求或依赖下列权限。不同 Android 版本和设备厂商的授权界面会有所不同。对于 Android 14 及更高版本，精确闹钟与全屏通知属于系统重点管理能力，请在真机上逐项验证。[1] [2]

| 权限 | 使用位置 | 目的 |
| --- | --- | --- |
| `USE_EXACT_ALARM` | 闹钟调度 | 以用户设置的时间精确触发闹钟。 |
| `USE_FULL_SCREEN_INTENT` | 触发通知 | 在合适的锁屏或熄屏场景显示全屏闹钟界面。 |
| `POST_NOTIFICATIONS` | 活动闹钟通知 | 显示响铃中的前台通知。 |
| `CAMERA` | 物品核验 | 拍摄待识别物品。 |
| `RECORD_AUDIO` | 英语跟读 | 向系统语音识别提供麦克风输入。 |
| `INTERNET` | 物品核验 | 调用用户配置的视觉服务。 |
| `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 响铃期间 | 在前台服务中持续播放铃声。 |
| `WAKE_LOCK` | 触发与响铃 | 减少闹钟活动期间被过早休眠的风险。 |
| `RECEIVE_BOOT_COMPLETED` | 重启后 | 恢复已启用的闹钟安排。 |

### 联网识图的数据处理

拍照核验是唯一会将图像发送到网络的功能。应用会在本地修正照片方向、将长边按需缩小至不超过 1280 像素，然后以兼容 Chat Completions 图片输入的请求格式提交给你指定的服务。视觉配置中的访问密钥和服务信息通过 AndroidX Security 的加密共享偏好设置保存。

请只使用你信任、已了解其数据政策并支持 HTTPS 的服务端点。项目不会代管、收集或上传你的服务密钥；但图像和请求内容会按你选择的服务商规则处理。若不希望照片离开设备，请不要启用联网识图，并在开发阶段替换或实现本地核验方案。

## 手动验收建议

为了避免只在桌面构建成功而未发现真机问题，建议在至少一台实体设备上按以下路径测试。

| 场景 | 预期结果 |
| --- | --- |
| 首次打开并授权 | 系统会按需请求通知、相机和麦克风等权限；用户可以在系统设置中撤销。 |
| 设置 1–2 分钟后的闹钟 | 闹钟到点触发通知与任务流程；不同 ROM 的后台限制可能影响实际呈现。 |
| 熄屏或锁屏触发 | 在已允许全屏通知的设备上进入闹钟任务页；未允许时至少应检查通知呈现。 |
| 完成数学题 | 正确答案进入拍照任务，不正确答案保留在当前步骤并提示重试。 |
| 完成拍照任务 | 配置有效且目标物品识别成功后进入英语跟读。 |
| 完成英语跟读 | 识别文本相似度达标后显示完成页，并可停止铃声。 |
| 重启设备 | 已启用闹钟会在系统允许的条件下重新注册。 |

## 常见问题

### 闹钟未按时触发或无法启用

首先检查应用的 **闹钟和提醒** 特殊访问权限，以及电池优化、后台自启动和通知设置。Android 对精确闹钟的能力及其默认授权状态随版本、安装方式与应用类别而变化；在 Android 14 及更高版本尤其应以系统设置的实际状态为准。[1]

### 熄屏时没有弹出全屏界面

确认应用允许发送全屏通知。Android 14 及更高版本会限制全屏通知的使用，用户也可以在“特殊应用访问”中撤销该许可。[2] 即使权限已开启，不同厂商的锁屏、后台和省电策略也可能改变界面展示时机。

### 拍照任务一直失败

请确认相机权限、网络连接、服务基础地址、API 密钥和模型名均正确。该服务必须兼容 Chat Completions 的图片输入，并能返回可解析的 JSON 结果。照片中的目标物品应清晰、占据足够画面并符合当次任务提示。

### 英语跟读无法通过

确认麦克风权限已授予并在安静环境中重试。系统语音识别的可用语言、离线模型和结果质量因设备及安装的语音服务而不同。

## 开发说明

项目使用 Kotlin DSL 管理 Gradle 构建，界面采用 Compose，调度与配置保存在应用内。当前仓库未包含自动化测试套件；在提交涉及闹钟、权限、相机或语音流程的修改前，请至少执行 `assembleDebug` 并按“手动验收建议”完成真机验证。

建议的提交前检查如下：

```bash
./gradlew assembleDebug
git status
```

请勿提交 `local.properties`、构建产物、日志、API 密钥、真实照片或其他个人数据。仓库已通过 `.gitignore` 排除常见的本机构建与 IDE 文件。

## 贡献与许可证

欢迎以 Issue 或 Pull Request 的方式改进可用性、任务类型、无障碍支持和设备兼容性。提交前请说明测试设备、Android 版本以及对锁屏、后台和权限流程的影响。

当前仓库尚未附带许可证文件。若计划公开分发、复用或接受外部贡献，请由项目维护者明确添加合适的许可证后再进行。

## 参考资料

[1] [Android Developers：Android 14 中精确闹钟默认被拒绝](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)

[2] [Android Open Source Project：全屏通知限制](https://source.android.com/docs/core/permissions/fsi-limits)
