# 后台定时调度重构设计

## 背景

当前自动更新由前端 `src/main.js` 使用 `setInterval` 实现。这个实现只在 WebView 页面存活时运行：窗口关闭、应用退出、页面被挂起后，定时任务就停止。它也无法适配 Android 后台生命周期。

最初考虑过 Linux 用户级 systemd timer，但该方案需要把当前主程序路径写入 `ExecStart`。这会让定时服务与安装路径强耦合：AppImage 被移动、开发路径与发布路径切换、包管理器安装路径变化，都会导致 service 失效。同时，设置桌面壁纸依赖图形会话环境，systemd user service 与 KDE/GNOME/Wayland 会话上下文之间也容易出现环境不一致。

## 目标

1. Linux 桌面端使用托盘常驻进程承载后台定时任务。
2. Android 使用原生后台任务调度，不依赖 WebView 常驻。
3. 前端只负责编辑配置、显示状态、触发启停，不再直接持有定时器。
4. 手动更新、选中壁纸应用、清除壁纸等现有行为保持可用。

## 非目标

1. 本次不实现 Linux 开机自启。
2. 本次不实现复杂的调度历史、失败重试 UI 或通知中心。
3. 本次不支持 iOS 后台调度。
4. 本次不使用 systemd、cron 或外部 daemon。

## 平台策略

### Linux 桌面

Linux 使用 Tauri 托盘常驻：

- 应用启动时创建托盘图标。
- 用户关闭主窗口时隐藏窗口，不退出进程。
- 托盘菜单提供显示窗口、立即更新、暂停/启用自动更新、退出。
- 后端保存当前配置，并在 Rust 进程内启动/停止定时任务。
- 定时触发时调用与手动更新相同的 Rust 下载和应用壁纸流程。
- 用户显式点击“退出”时才终止进程，定时任务随进程结束。

这种方式避免主程序路径写入外部服务，也能保证设置壁纸时仍处于用户图形会话内。

### Android

Android 使用原生后台调度：

- Rust/Tauri 命令把调度配置转发到现有 Android 平台插件。
- Android 插件把必要配置保存到 `SharedPreferences`。
- Android 插件使用 `WorkManager` 注册或取消周期性任务。
- Worker 触发时读取配置、下载 Bing 图片，并调用 `WallpaperManager` 应用壁纸。

Android 不依赖常驻进程，因为系统会回收后台进程；调度必须交给系统调度器。

### 其他平台

其他桌面平台暂时返回“不支持后台定时调度”。手动更新继续可用。

## API 设计

Rust 新增 Tauri 命令：

- `configure_schedule(config: AppConfig) -> Result<ScheduleStatus, String>`
- `cancel_schedule() -> Result<ScheduleStatus, String>`
- `schedule_status() -> Result<ScheduleStatus, String>`

`ScheduleStatus` 包含：

- `enabled: bool`
- `message: String`

前端在配置变更时调用 `configure_schedule` 或 `cancel_schedule`，并移除 `setInterval`。

## 数据流

1. 用户在前端修改定时开关或间隔。
2. 前端保存配置到 localStorage。
3. 前端调用 Rust 调度命令。
4. Linux：Rust 更新内存中的调度任务。
5. Android：Rust 调用平台插件，平台插件注册或取消 WorkManager。
6. 定时触发后执行和手动更新相同的壁纸更新流程。

## 错误处理

- Linux 托盘创建失败时不阻止主窗口启动，但调度命令返回明确错误。
- Linux 定时任务执行失败时记录错误并保持下一次触发。
- Android WorkManager 注册失败时通过 Tauri 命令返回错误。
- 其他平台调用调度命令时返回“不支持此平台的后台定时调度”。

## 测试策略

1. Rust 单元测试覆盖调度间隔计算、启停状态转换和禁用配置行为。
2. JavaScript 使用语法检查确保移除页面级 timer 后没有语法错误。
3. Android 单元测试覆盖 WorkManager 请求参数、配置序列化和取消逻辑中可纯函数化的部分。
4. 手动验证 Linux 桌面：
   - 启用自动更新后关闭窗口，应用仍在托盘。
   - 到达间隔后触发更新。
   - 托盘“退出”会真正结束进程。

## 迁移影响

现有 localStorage 配置结构保持不变。用户升级后首次打开应用时，前端读取旧配置并调用新的调度命令，使后端调度状态与 UI 配置同步。
