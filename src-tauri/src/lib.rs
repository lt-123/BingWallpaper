#[cfg(not(target_os = "android"))]
use std::fs;
use std::path::PathBuf;
#[cfg(not(any(target_os = "android", target_os = "ios")))]
use std::process::Command;

#[cfg(target_os = "android")]
use base64::{engine::general_purpose, Engine as _};
use serde::{Deserialize, Serialize};
#[cfg(target_os = "android")]
use tauri::Manager;
use tauri::{AppHandle, Runtime, State};
use wallpaper_core::{
    bing::{BingConfig, BingSource, BingWallpaperResponse},
    config::{FitMode, ScheduleConfig},
    wallpaper::WallpaperItem,
};

mod platform;
mod scheduler;
#[cfg(not(any(target_os = "android", target_os = "ios")))]
mod tray;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    pub bing: BingConfig,
    pub fit_mode: FitMode,
    pub schedule: ScheduleConfig,
    pub save_to_file_system: bool,
    pub platform: PlatformConfig,
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            bing: BingConfig::default(),
            fit_mode: FitMode::Fill,
            schedule: ScheduleConfig {
                enabled: false,
                interval_minutes: 360,
                notify_on_background_update: true,
            },
            save_to_file_system: true,
            platform: PlatformConfig::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlatformConfig {
    pub set_lock_screen: bool,
    pub notify_on_manual_update: bool,
}

impl Default for PlatformConfig {
    fn default() -> Self {
        Self {
            set_lock_screen: false,
            notify_on_manual_update: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UpdateResult {
    pub wallpaper: WallpaperItem,
    pub saved_path: Option<PathBuf>,
    pub saved_uri: Option<String>,
    pub applied: bool,
    pub message: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PlatformCapabilities {
    pub can_clear_wallpaper: bool,
}

/// 向前端返回默认应用配置，用于首次运行或 localStorage 无缓存时的初始化。
#[tauri::command]
fn default_config() -> AppConfig {
    AppConfig::default()
}

/// 返回当前平台支持的功能集合，前端根据此结果显示或隐藏对应按钮。
/// 目前仅 Android 支持"清除系统壁纸"功能。
#[tauri::command]
fn platform_capabilities() -> PlatformCapabilities {
    PlatformCapabilities {
        can_clear_wallpaper: cfg!(target_os = "android"),
    }
}

/// 拉取 Bing 壁纸存档列表。
///
/// `page` 对应 Bing API 的 `idx` 偏移参数（u8），每次返回至多 `config.count` 条。
/// Bing 实际历史深度约 15 条，正常使用不会触及 u8 上限（255）。
#[tauri::command]
async fn fetch_bing_gallery(config: BingConfig, page: u8) -> Result<Vec<WallpaperItem>, String> {
    let request = BingSource::new(config.clone()).archive_request(page);
    let response = reqwest::get(request.to_url())
        .await
        .map_err(|err| format!("Failed to request Bing gallery: {err}"))?
        .error_for_status()
        .map_err(|err| format!("Bing returned an error: {err}"))?
        .json::<BingWallpaperResponse>()
        .await
        .map_err(|err| format!("Failed to parse Bing gallery: {err}"))?;

    Ok(response.into_wallpapers(config.resolution))
}

/// 手动触发壁纸更新的 IPC 命令。
///
/// 注意：前端当前已不直接调用此命令（JS 侧的 manualUpdate 函数已被移除，
/// 用户通过"应用选中壁纸"流程代替）。系统托盘的"立即更新"使用
/// `run_desktop_manual_update`，同样不经过此命令。
/// 此命令应考虑从 invoke_handler 中移除，以缩小 WebView 可调用的 IPC 接口范围。
#[tauri::command]
async fn manual_update<R: Runtime>(
    app: AppHandle<R>,
    config: AppConfig,
) -> Result<UpdateResult, String> {
    run_manual_update(app, config).await
}

pub(crate) async fn run_manual_update<R: Runtime>(
    app: AppHandle<R>,
    config: AppConfig,
) -> Result<UpdateResult, String> {
    let wallpapers = fetch_bing_gallery(config.bing.clone(), 0).await?;
    let wallpaper = wallpapers
        .into_iter()
        .next()
        .ok_or_else(|| "Bing did not return any wallpapers".to_string())?;
    apply_wallpaper(app, wallpaper, config).await
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
pub(crate) async fn run_desktop_manual_update(config: AppConfig) -> Result<UpdateResult, String> {
    let wallpapers = fetch_bing_gallery(config.bing.clone(), 0).await?;
    let wallpaper = wallpapers
        .into_iter()
        .next()
        .ok_or_else(|| "Bing did not return any wallpapers".to_string())?;
    let bytes = download_wallpaper_bytes(&wallpaper).await?;
    apply_downloaded_wallpaper_desktop(wallpaper, config, bytes)
}

#[tauri::command]
async fn configure_schedule<R: Runtime>(
    app: AppHandle<R>,
    config: AppConfig,
    scheduler: State<'_, scheduler::SchedulerState>,
) -> Result<scheduler::ScheduleStatus, String> {
    configure_platform_schedule(app, config, scheduler).await
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
async fn configure_platform_schedule<R: Runtime>(
    _app: AppHandle<R>,
    config: AppConfig,
    scheduler: State<'_, scheduler::SchedulerState>,
) -> Result<scheduler::ScheduleStatus, String> {
    scheduler.configure_for_desktop(config)
}

#[cfg(target_os = "android")]
async fn configure_platform_schedule<R: Runtime>(
    app: AppHandle<R>,
    config: AppConfig,
    scheduler: State<'_, scheduler::SchedulerState>,
) -> Result<scheduler::ScheduleStatus, String> {
    let mut status = scheduler.configure(config.clone())?;
    let native_message = app
        .state::<platform::PlatformWallpaper<R>>()
        .configure_android_schedule(platform::AndroidSchedulePayload::from(config))
        .await?;
    status.message = native_message;
    Ok(status)
}

#[cfg(target_os = "ios")]
async fn configure_platform_schedule<R: Runtime>(
    _app: AppHandle<R>,
    _config: AppConfig,
    _scheduler: State<'_, scheduler::SchedulerState>,
) -> Result<scheduler::ScheduleStatus, String> {
    Err("Background scheduling is not available on iOS yet".to_string())
}

#[tauri::command]
async fn cancel_schedule<R: Runtime>(
    app: AppHandle<R>,
    scheduler: State<'_, scheduler::SchedulerState>,
) -> Result<scheduler::ScheduleStatus, String> {
    cancel_platform_schedule(app, scheduler).await
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
async fn cancel_platform_schedule<R: Runtime>(
    _app: AppHandle<R>,
    scheduler: State<'_, scheduler::SchedulerState>,
) -> Result<scheduler::ScheduleStatus, String> {
    scheduler.cancel()
}

#[cfg(target_os = "android")]
async fn cancel_platform_schedule<R: Runtime>(
    app: AppHandle<R>,
    scheduler: State<'_, scheduler::SchedulerState>,
) -> Result<scheduler::ScheduleStatus, String> {
    app.state::<platform::PlatformWallpaper<R>>()
        .cancel_android_schedule()
        .await?;
    scheduler.cancel()
}

#[cfg(target_os = "ios")]
async fn cancel_platform_schedule<R: Runtime>(
    _app: AppHandle<R>,
    _scheduler: State<'_, scheduler::SchedulerState>,
) -> Result<scheduler::ScheduleStatus, String> {
    Err("Background scheduling is not available on iOS yet".to_string())
}

#[tauri::command]
fn schedule_status(
    scheduler: State<'_, scheduler::SchedulerState>,
) -> Result<scheduler::ScheduleStatus, String> {
    scheduler.status()
}

#[tauri::command]
async fn apply_wallpaper<R: Runtime>(
    app: AppHandle<R>,
    wallpaper: WallpaperItem,
    config: AppConfig,
) -> Result<UpdateResult, String> {
    let bytes = download_wallpaper_bytes(&wallpaper).await?;

    apply_downloaded_wallpaper(app, wallpaper, config, bytes).await
}

async fn download_wallpaper_bytes(wallpaper: &WallpaperItem) -> Result<Vec<u8>, String> {
    reqwest::get(&wallpaper.image_url)
        .await
        .map_err(|err| format!("Failed to download wallpaper: {err}"))?
        .error_for_status()
        .map_err(|err| format!("Wallpaper download returned an error: {err}"))?
        .bytes()
        .await
        .map(|bytes| bytes.to_vec())
        .map_err(|err| format!("Failed to read wallpaper bytes: {err}"))
}

#[cfg(target_os = "android")]
async fn apply_downloaded_wallpaper<R: Runtime>(
    app: AppHandle<R>,
    wallpaper: WallpaperItem,
    config: AppConfig,
    bytes: Vec<u8>,
) -> Result<UpdateResult, String> {
    let response = app
        .state::<platform::PlatformWallpaper<R>>()
        .save_and_apply_android(platform::AndroidSaveWallpaperPayload {
            file_name: wallpaper.download_file_name(),
            mime_type: "image/jpeg".to_string(),
            image_base64: general_purpose::STANDARD.encode(bytes),
            fit_mode: config.fit_mode,
            set_lock_screen: config.platform.set_lock_screen,
            save_to_gallery: config.save_to_file_system,
            show_toast: config.platform.notify_on_manual_update,
        })
        .await?;

    Ok(UpdateResult {
        wallpaper,
        saved_path: None,
        saved_uri: (!response.uri.is_empty()).then_some(response.uri),
        applied: response.applied_home_screen || response.applied_lock_screen,
        message: "Wallpaper updated".to_string(),
    })
}

#[cfg(not(target_os = "android"))]
async fn apply_downloaded_wallpaper<R: Runtime>(
    _app: AppHandle<R>,
    wallpaper: WallpaperItem,
    config: AppConfig,
    bytes: Vec<u8>,
) -> Result<UpdateResult, String> {
    apply_downloaded_wallpaper_desktop(wallpaper, config, bytes)
}

#[cfg(not(target_os = "android"))]
fn apply_downloaded_wallpaper_desktop(
    wallpaper: WallpaperItem,
    config: AppConfig,
    bytes: Vec<u8>,
) -> Result<UpdateResult, String> {
    let saved_path = if config.save_to_file_system {
        let directory = wallpaper_directory()?;
        fs::create_dir_all(&directory)
            .map_err(|err| format!("Failed to create wallpaper directory: {err}"))?;
        let path = directory.join(wallpaper.download_file_name());
        fs::write(&path, bytes.as_slice())
            .map_err(|err| format!("Failed to save wallpaper file: {err}"))?;
        Some(path)
    } else {
        None
    };

    let temporary_path;
    let path_for_system = if let Some(path) = saved_path.clone() {
        path
    } else {
        temporary_path = std::env::temp_dir().join(wallpaper.download_file_name());
        fs::write(&temporary_path, bytes.as_slice())
            .map_err(|err| format!("Failed to prepare temporary wallpaper file: {err}"))?;
        temporary_path
    };
    set_system_wallpaper(&path_for_system, config.fit_mode)?;

    Ok(UpdateResult {
        wallpaper,
        saved_path,
        saved_uri: None,
        applied: true,
        message: "Wallpaper updated".to_string(),
    })
}

#[tauri::command]
async fn clear_system_wallpaper<R: Runtime>(app: AppHandle<R>) -> Result<String, String> {
    clear_platform_wallpaper(app).await
}

/// 返回壁纸文件的保存目录（桌面端）。
/// 优先使用系统图片目录（~/Pictures/Wallora），回退到下载目录。
///
/// 注意：目录名从"Wallpaper Client"更名为"Wallora"后，
/// 旧版本已保存的壁纸文件不会自动迁移，需用户手动移动或清理。
#[cfg(not(target_os = "android"))]
fn wallpaper_directory() -> Result<PathBuf, String> {
    dirs::picture_dir()
        .or_else(dirs::download_dir)
        .map(|dir| dir.join("Wallora"))
        .ok_or_else(|| "Could not locate a Pictures or Downloads directory".to_string())
}

/// 设置桌面系统壁纸。
/// KDE 环境优先使用 `plasma-apply-wallpaperimage` 命令以获得更好的兼容性；
/// 其他桌面环境使用 `wallpaper` crate 的通用实现。
/// `_fit_mode` 暂未被桌面路径使用（KDE 自行管理铺展模式），保留参数为未来扩展预留。
#[cfg(not(any(target_os = "android", target_os = "ios")))]
fn set_system_wallpaper(path: &PathBuf, _fit_mode: FitMode) -> Result<(), String> {
    if is_kde_desktop() && command_exists("plasma-apply-wallpaperimage") {
        let status = Command::new("plasma-apply-wallpaperimage")
            .arg(path)
            .status()
            .map_err(|err| format!("Failed to run plasma-apply-wallpaperimage: {err}"))?;
        if status.success() {
            return Ok(());
        }
    }

    wallpaper::set_from_path(
        path.to_str()
            .ok_or_else(|| "Wallpaper path is not valid UTF-8".to_string())?,
    )
    .map_err(|err| format!("Failed to set system wallpaper: {err}"))
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
fn is_kde_desktop() -> bool {
    std::env::var("XDG_CURRENT_DESKTOP")
        .map(|desktop| desktop.split(':').any(|part| part.eq_ignore_ascii_case("KDE")))
        .unwrap_or(false)
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
fn command_exists(command: &str) -> bool {
    std::env::var_os("PATH")
        .and_then(|paths| {
            std::env::split_paths(&paths)
                .map(|path| path.join(command))
                .find(|path| path.is_file())
        })
        .is_some()
}

#[cfg(target_os = "ios")]
fn set_system_wallpaper(_path: &PathBuf, _fit_mode: FitMode) -> Result<(), String> {
    Err("Mobile wallpaper setting requires a native platform adapter".to_string())
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
async fn clear_platform_wallpaper<R: Runtime>(_app: AppHandle<R>) -> Result<String, String> {
    Err("Clearing system wallpaper is platform-specific and is not available yet".to_string())
}

#[cfg(target_os = "android")]
async fn clear_platform_wallpaper<R: Runtime>(app: AppHandle<R>) -> Result<String, String> {
    app.state::<platform::PlatformWallpaper<R>>()
        .clear_android_wallpaper()
        .await
}

#[cfg(target_os = "ios")]
async fn clear_platform_wallpaper<R: Runtime>(_app: AppHandle<R>) -> Result<String, String> {
    Err("iOS wallpaper clearing requires a native platform adapter".to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .manage(scheduler::SchedulerState::default())
        .plugin(platform::init())
        .plugin(tauri_plugin_opener::init())
        .setup(|app| {
            #[cfg(not(any(target_os = "android", target_os = "ios")))]
            tray::setup_tray(app)?;
            Ok(())
        })
        .on_window_event(|window, event| {
            #[cfg(not(any(target_os = "android", target_os = "ios")))]
            tray::handle_window_event(window, event);
        })
        .invoke_handler(tauri::generate_handler![
            default_config,
            platform_capabilities,
            fetch_bing_gallery,
            manual_update,
            apply_wallpaper,
            clear_system_wallpaper,
            configure_schedule,
            cancel_schedule,
            schedule_status
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
