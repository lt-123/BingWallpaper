//! Android JNI 桥接层：Rust 业务逻辑的唯一入口。
//!
//! 设计原则：
//! - Rust 实现所有业务逻辑（Bing 获取、下载重试、DailyAt 时间窗口、通知策略）
//! - Kotlin 仅暴露三个薄平台接口（PlatformApis），供本模块通过 JNI 回调
//! - Worker、QS 瓷砖、手动应用三个入口的业务代码完全统一，无 UI 依赖
//!
//! 调用路径：
//! - Worker/Tile → RustCore.fetchAndApplyLatestWallpaper (JNI) → Rust
//! - 手动应用 → Tauri 命令 → Rust async 下载 → spawn_blocking → JAVA_VM attach → Rust JNI 回调

use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::jboolean;
use jni::JNIEnv;
use std::sync::OnceLock;
use wallora_core::config::ScheduleMode;

use crate::AppConfig;

/// 存储 JavaVM，供 Tauri async 命令路径在 spawn_blocking 中 attach 新线程。
/// 由 Plugin init 中调用 RustCore.initialize() 写入。
pub static JAVA_VM: OnceLock<jni::JavaVM> = OnceLock::new();

/// 存储 Application Context 全局引用，供 Tauri async 命令路径调用 JNI。
/// 由 RustCore.initialize(appContext) 写入，Application Context 生命周期与进程一致。
pub static GLOBAL_APP_CONTEXT: OnceLock<jni::objects::GlobalRef> = OnceLock::new();

/// 壁纸操作结果通知 ID（成功/失败），所有入口统一使用同一 ID 使通知可被替换。
pub const RESULT_NOTIFICATION_ID: i32 = 1002;

/// 缓存 PlatformApis 类的全局引用。
///
/// FindClass 从 attach_current_thread 的 native 线程调用时使用 bootstrap classloader，
/// 无法找到 app 类。在 plugin init（Java 线程）时缓存类引用，后续 JNI 调用直接使用，
/// 绕过 FindClass。
static PLATFORM_APIS_CLASS_REF: OnceLock<jni::objects::GlobalRef> = OnceLock::new();

fn platform_apis_class<'local>() -> Result<jni::objects::JClass<'local>, String> {
    let global = PLATFORM_APIS_CLASS_REF
        .get()
        .ok_or_else(|| "PlatformApis 类引用未初始化（RustCore.initialize 未调用？）".to_string())?;
    // SAFETY: GlobalRef 与 JVM 生命周期一致，长于任何 JNI 函数调用。
    Ok(unsafe { jni::objects::JClass::from_raw(global.as_raw()) })
}

// ---- JNI 导出函数 ----

/// Plugin init 时由 Kotlin 调用，存储 JavaVM 和 Application Context 供异步路径使用。
#[no_mangle]
pub extern "C" fn Java_xyz_liut_wallora_platform_RustCore_initialize(
    mut env: JNIEnv,
    _class: JClass,
    app_context: JObject,
) {
    if JAVA_VM.get().is_none() {
        if let Ok(vm) = env.get_java_vm() {
            JAVA_VM.set(vm).ok();
        }
    }
    if GLOBAL_APP_CONTEXT.get().is_none() {
        if let Ok(global_ref) = env.new_global_ref(&app_context) {
            GLOBAL_APP_CONTEXT.set(global_ref).ok();
        }
    }
    // 在 Java 线程（拥有正确 classloader）时缓存 PlatformApis 类引用，
    // 供后续从 native 线程发起的 JNI 调用使用。
    if PLATFORM_APIS_CLASS_REF.get().is_none() {
        let _ = env.exception_clear();
        match env.find_class("xyz/liut/wallora/platform/PlatformApis") {
            Ok(class) => {
                if let Ok(global) = env.new_global_ref(&class) {
                    PLATFORM_APIS_CLASS_REF.set(global).ok();
                }
            }
            Err(e) => {
                eprintln!("[Wallora] 缓存 PlatformApis 类失败: {e}");
                let _ = env.exception_clear();
            }
        }
    }
}

/// Worker / QS 瓷砖调用：获取最新 Bing 壁纸并应用。
/// 包含 DailyAt 时间窗口检查、下载重试（3 次）、存图、设置壁纸、结果通知。
/// 返回 true = 成功，false = 失败（Worker 据此返回 Result.retry()）。
#[no_mangle]
pub extern "C" fn Java_xyz_liut_wallora_platform_RustCore_fetchAndApplyLatestWallpaper(
    mut env: JNIEnv,
    _class: JClass,
    context: JObject,
    config_json: JString,
) -> jboolean {
    let config_json_str: String = match env.get_string(&config_json) {
        Ok(s) => s.into(),
        Err(e) => {
            eprintln!("[Wallora] JNI get_string error: {e}");
            return 0;
        }
    };

    match fetch_and_apply_latest_impl(&mut env, &context, &config_json_str) {
        Ok(()) => 1,
        Err(e) => {
            eprintln!("[Wallora] fetchAndApplyLatestWallpaper error: {e}");
            let _ = jni_post_notification(&mut env, &context, "壁纸更新失败", &e, RESULT_NOTIFICATION_ID);
            0
        }
    }
}

// ---- 内部实现 ----

fn fetch_and_apply_latest_impl(
    env: &mut JNIEnv,
    context: &JObject,
    config_json: &str,
) -> Result<(), String> {
    let config: AppConfig =
        serde_json::from_str(config_json).map_err(|e| format!("配置解析失败: {e}"))?;

    // DailyAt 模式：当前时间不在任意目标时刻的 ±8 分钟窗口内则跳过
    if config.schedule.mode == ScheduleMode::DailyAt {
        if !is_within_daily_time_window(&config.schedule.daily_times, 8) {
            return Ok(());
        }
    }

    // Worker 线程无 tokio 上下文，创建单线程 runtime 执行异步 HTTP
    let rt = tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()
        .map_err(|e| format!("tokio runtime 创建失败: {e}"))?;

    let wallpapers = rt.block_on(crate::fetch_bing_gallery_impl(config.bing.clone(), 0))?;
    let wallpaper = wallpapers
        .into_iter()
        .next()
        .ok_or_else(|| "Bing 未返回壁纸".to_string())?;

    let bytes = rt.block_on(crate::download_bytes_by_url(&wallpaper.image_url))?;

    apply_bytes_via_jni(
        env,
        context,
        &bytes,
        &wallpaper.download_file_name(),
        &wallpaper.title,
        &config,
        config.schedule.notify_on_background_update,
    )
}

/// 共享的 Android 平台操作：存相册 + 设置壁纸 + 通知。
/// Worker JNI 路径和 Tauri 命令 spawn_blocking 路径均调用此函数。
pub fn apply_bytes_via_jni(
    env: &mut JNIEnv,
    context: &JObject,
    bytes: &[u8],
    file_name: &str,
    title: &str,
    config: &AppConfig,
    show_notification: bool,
) -> Result<(), String> {
    if config.save_to_file_system {
        jni_save_to_gallery(env, context, file_name, bytes)?;
    }
    jni_set_wallpaper(
        env,
        context,
        bytes,
        config.fit_mode,
        config.platform.set_lock_screen,
    )?;
    if show_notification {
        jni_post_notification(env, context, "壁纸已更新", title, RESULT_NOTIFICATION_ID)?;
    }
    Ok(())
}

fn jni_save_to_gallery(
    env: &mut JNIEnv,
    context: &JObject,
    file_name: &str,
    bytes: &[u8],
) -> Result<(), String> {
    let class = platform_apis_class()?;
    let j_file_name = env.new_string(file_name).map_err(|e| e.to_string())?;
    let j_bytes = env.byte_array_from_slice(bytes).map_err(|e| e.to_string())?;
    env.call_static_method(
        class,
        "saveToGallery",
        "(Landroid/content/Context;Ljava/lang/String;[B)V",
        &[
            JValue::from(context),
            JValue::from(&j_file_name),
            JValue::from(&j_bytes),
        ],
    )
    .map_err(|e| format!("JNI saveToGallery 错误: {e}"))?;
    Ok(())
}

fn jni_set_wallpaper(
    env: &mut JNIEnv,
    context: &JObject,
    bytes: &[u8],
    fit_mode: wallora_core::config::FitMode,
    set_lock_screen: bool,
) -> Result<(), String> {
    let class = platform_apis_class()?;
    let j_bytes = env.byte_array_from_slice(bytes).map_err(|e| e.to_string())?;
    let j_fit_mode = env
        .new_string(fit_mode.as_str())
        .map_err(|e| e.to_string())?;
    env.call_static_method(
        class,
        "setWallpaper",
        "(Landroid/content/Context;[BLjava/lang/String;Z)V",
        &[
            JValue::from(context),
            JValue::from(&j_bytes),
            JValue::from(&j_fit_mode),
            JValue::Bool(set_lock_screen as u8),
        ],
    )
    .map_err(|e| format!("JNI setWallpaper 错误: {e}"))?;
    Ok(())
}

pub fn jni_post_notification(
    env: &mut JNIEnv,
    context: &JObject,
    title: &str,
    message: &str,
    notification_id: i32,
) -> Result<(), String> {
    let class = platform_apis_class()?;
    let j_title = env.new_string(title).map_err(|e| e.to_string())?;
    let j_message = env.new_string(message).map_err(|e| e.to_string())?;
    env.call_static_method(
        class,
        "postNotification",
        "(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;I)V",
        &[
            JValue::from(context),
            JValue::from(&j_title),
            JValue::from(&j_message),
            JValue::Int(notification_id),
        ],
    )
    .map_err(|e| format!("JNI postNotification 错误: {e}"))?;
    Ok(())
}

/// 检查当前本地时间是否落在任意 "HH:MM" 时间点的 ±window_minutes 窗口内。
fn is_within_daily_time_window(times: &[String], window_minutes: i64) -> bool {
    use chrono::{Local, Timelike};
    if times.is_empty() {
        return false;
    }
    let now = Local::now();
    let now_minutes = now.hour() as i64 * 60 + now.minute() as i64;
    times.iter().any(|time_str| {
        let mut parts = time_str.splitn(2, ':');
        let h: i64 = parts.next().and_then(|s| s.parse().ok()).unwrap_or(-1);
        let m: i64 = parts.next().and_then(|s| s.parse().ok()).unwrap_or(-1);
        if h < 0 || m < 0 {
            return false;
        }
        (now_minutes - (h * 60 + m)).abs() <= window_minutes
    })
}
