# BingWallpaper v3 Design

## 目标

v3 是一次以 Android 10+ 分区存储为核心的全量重构。应用只内置 Bing 每日壁纸源，保留多时间点自动同步、立即更新、保存到相册、同时设置锁屏、显示 Toast、手动入口显示、主图标显示、忽略电池优化、清空壁纸和关于页。

v3 不申请外部存储权限，不使用 `requestLegacyExternalStorage`，不兼容 Android 9 及以下，`minSdk` 提升到 29。

## 明确删除的功能

- 删除源列表入口。
- 删除添加源入口。
- 删除自定义直链源功能。
- 删除仅 Wi-Fi 自动更新开关和相关判断。
- 删除依赖外部公共文件路径直接读写的保存逻辑。

代码层面仍预留壁纸源抽象，后续新增其它源时不需要改同步主流程。

## 架构

v3 使用原生 Java，不新增三方运行时依赖。核心代码按职责拆分，避免继续把下载、保存、设置壁纸、重试和定时全部耦合在 Service 中。

### source

`WallpaperSource` 是壁纸源接口，负责返回可下载的壁纸信息。接口应包含中文 Javadoc，说明实现类需要提供图片 URL、文件名、标题或描述。

`BingWallpaperSource` 是 v3 唯一内置源。它请求 Bing API，解析今日图片 URL，并生成稳定文件名。JSON 解析继续使用原生 `org.json` 或 Android 平台能力，不引入 Gson、Moshi 等依赖。

### network

`HttpDownloader` 负责 HTTP 请求和下载流复制。它只依赖 `HttpURLConnection`，支持设置 User-Agent、超时、重定向和基础错误信息。

下载层不决定文件保存位置，只把网络输入流交给调用方或写到调用方提供的输出流。

### storage

`WallpaperStore` 负责保存图片，并返回统一的保存结果。

保存到相册开启时：
- 使用 `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`。
- `RELATIVE_PATH` 为 `Pictures/BingWallpaper`。
- 写入前设置 `IS_PENDING=1`。
- 下载成功后设置 `IS_PENDING=0`。
- 返回可用于设置壁纸的 `Uri`。

保存到相册关闭时：
- 写入 `context.getCacheDir()/wallpapers/`。
- 设置壁纸完成后删除缓存文件。
- 返回缓存文件引用或可打开的内部 `Uri` 包装对象。

任何路径都不需要 `WRITE_EXTERNAL_STORAGE` 或 `READ_MEDIA_IMAGES`。

### wallpaper

`WallpaperSetter` 负责设置壁纸。它从 `ContentResolver.openInputStream(uri)` 或 `FileInputStream` 获取图片流，并调用 `WallpaperManager.setStream(...)`。

当“同时设置锁屏”开启时，使用 `WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK`。关闭时只设置系统桌面壁纸。

如果需要裁剪或缩放，逻辑应集中在 `WallpaperSetter` 或独立图片工具中，并添加中文注释说明为什么裁剪。

### sync

`WallpaperSyncUseCase` 是主流程编排：

1. 从 `SettingsStore` 读取配置。
2. 使用 `WallpaperSource` 获取今日壁纸信息。
3. 使用下载器下载图片。
4. 使用 `WallpaperStore` 保存到相册或缓存。
5. 使用 `WallpaperSetter` 设置壁纸。
6. 成功后清理临时缓存并重新安排定时任务。
7. 失败时返回明确错误，由 Service 展示通知或 Toast。

重试策略在 use case 或 Service 边界统一管理，最多重试 3 次。失败后安排 30 分钟后的重试任务。

### schedule

`ScheduleManager` 封装 `JobScheduler`。

它负责：
- 保存和读取多个 `HH:mm` 时间点。
- 取消旧任务。
- 为每个时间点计算下一次触发时间。
- 安排 JobScheduler 任务。
- 安排失败重试任务。

下一次触发时间计算应拆成纯 Java 方法，方便单元测试。

### settings

`SettingsStore` 封装 SharedPreferences。

v3 配置项：
- 保存到相册。
- 同时设置锁屏。
- 显示 Toast。
- 显示手动设置入口。
- 隐藏主图标。
- 多时间点定时列表。

不再保留仅 Wi-Fi 配置。

## UI 行为

主设置页保留并简化为：

- 立即更新壁纸。
- 保存到相册。
- 同时设置锁屏。
- 显示 Toast。
- 手动设置入口显示。
- 主图标显示。
- 定时同步入口。
- 忽略电池优化。
- 清空壁纸。
- 关于。

定时列表页保留多时间点添加、清空和忽略电池优化入口。

快速设置磁贴继续触发一次同步。

## Manifest 与构建

- `minSdk = 29`。
- `compileSdk` 和 `targetSdk` 保持现代版本。
- 删除 `WRITE_EXTERNAL_STORAGE`。
- 删除 `android:requestLegacyExternalStorage`。
- 保留 `INTERNET`、`ACCESS_NETWORK_STATE`、`SET_WALLPAPER`、`SET_WALLPAPER_HINTS`、`RECEIVE_BOOT_COMPLETED`、`FOREGROUND_SERVICE`、`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`。
- 继续使用原生 Java。
- 严禁新增三方运行时依赖；测试依赖沿用现有 JUnit/Robolectric 体系。

## 注释规范

v3 新增或重构的核心类、接口和复杂方法必须添加中文 Javadoc 或中文代码注释。

必须添加注释的位置：
- 每个核心接口的职责说明。
- 每个核心实现类的职责说明。
- `MediaStore` 写入流程，尤其是 `IS_PENDING` 和 `RELATIVE_PATH`。
- 定时任务下一次触发时间计算。
- 同步主流程和重试策略。
- 设置系统壁纸和锁屏壁纸的分支。

不要求给简单 getter、setter、常量逐行添加注释。

## 测试策略

- `BingWallpaperSource` 使用固定 JSON 样例测试解析，不依赖真实网络。
- `SettingsStore` 测试配置读写和默认值。
- `ScheduleManager` 测试下一次触发时间计算。
- `WallpaperSyncUseCase` 使用假 `WallpaperSource`、假下载器、假 store、假 setter 测试成功、下载失败、设置失败和缓存清理。
- 编译验证使用 `./gradlew assembleDebug assembleRelease --warning-mode all`。
- 完整 `build` 可能受旧网络型单元测试影响，v3 中应删除或改写这类依赖真实网络的测试。

## 成功标准

- debug 和 release 均可编译打包。
- Manifest 不包含外部存储权限和 legacy storage 配置。
- 保存到相册开启时，图片写入系统 `Pictures/BingWallpaper`。
- 保存到相册关闭时，不向公共图片库写入，设置完成后清理缓存。
- 定时同步多时间点可用。
- 快速设置磁贴可触发一次同步。
- 代码没有新增三方运行时依赖。
- 新增/重构的核心代码包含中文 Javadoc 或必要中文注释。
