#[cfg(target_os = "android")]
use serde::{Deserialize, Serialize};
use tauri::{plugin::TauriPlugin, Manager, Runtime};

#[cfg(target_os = "android")]
use crate::AppConfig;

#[cfg(mobile)]
use tauri::plugin::PluginHandle;

#[cfg(target_os = "android")]
const PLUGIN_IDENTIFIER: &str = "xyz.liut.wallora.platform";

#[cfg(target_os = "android")]
use wallora_core::config::ScheduleMode;


#[cfg(target_os = "android")]
#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AndroidSchedulePayload {
    pub enabled: bool,
    pub interval_minutes: u32,
    pub notify_on_background_update: bool,
    pub config_json: String,
    /// "Interval" 或 "DailyAt"，供 Kotlin 插件决定 WorkManager 任务间隔
    pub schedule_mode: String,
}

#[cfg(target_os = "android")]
impl From<AppConfig> for AndroidSchedulePayload {
    fn from(config: AppConfig) -> Self {
        let schedule_mode = match config.schedule.mode {
            ScheduleMode::Interval => "Interval",
            ScheduleMode::DailyAt => "DailyAt",
        }
        .to_string();
        Self {
            enabled: config.schedule.enabled,
            interval_minutes: config.schedule.interval_minutes.max(1),
            notify_on_background_update: config.schedule.notify_on_background_update,
            config_json: serde_json::to_string(&config)
                .expect("AppConfig serialization for Android schedule must not fail"),
            schedule_mode,
        }
    }
}


pub struct PlatformWallpaper<R: Runtime> {
    #[cfg(mobile)]
    mobile_plugin_handle: PluginHandle<R>,
    #[cfg(not(mobile))]
    _marker: std::marker::PhantomData<fn() -> R>,
}

impl<R: Runtime> PlatformWallpaper<R> {

    #[cfg(target_os = "android")]
    pub async fn clear_android_wallpaper(&self) -> Result<String, String> {
        let response: AndroidClearWallpaperResponse = self
            .mobile_plugin_handle
            .run_mobile_plugin_async("clearWallpaper", serde_json::json!({}))
            .await
            .map_err(|err| err.to_string())?;
        Ok(response.message)
    }

    #[cfg(target_os = "android")]
    pub async fn configure_android_schedule(
        &self,
        payload: AndroidSchedulePayload,
    ) -> Result<String, String> {
        let response: AndroidScheduleResponse = self
            .mobile_plugin_handle
            .run_mobile_plugin_async("configureSchedule", payload)
            .await
            .map_err(|err| err.to_string())?;
        Ok(response.message)
    }

    #[cfg(target_os = "android")]
    pub async fn cancel_android_schedule(&self) -> Result<String, String> {
        let response: AndroidScheduleResponse = self
            .mobile_plugin_handle
            .run_mobile_plugin_async("cancelSchedule", serde_json::json!({}))
            .await
            .map_err(|err| err.to_string())?;
        Ok(response.message)
    }

    /// 查询是否已豁免电池优化（仅 Android）。
    #[cfg(target_os = "android")]
    pub async fn check_battery_exemption(&self) -> Result<bool, String> {
        #[derive(serde::Deserialize)]
        struct Resp {
            exempted: bool,
        }
        let resp: Resp = self
            .mobile_plugin_handle
            .run_mobile_plugin_async("checkBatteryOptimization", serde_json::json!({}))
            .await
            .map_err(|err| err.to_string())?;
        Ok(resp.exempted)
    }

    /// 打开系统页面引导用户申请电池优化豁免（仅 Android）。
    #[cfg(target_os = "android")]
    pub async fn request_battery_exemption(&self) -> Result<(), String> {
        let _: serde_json::Value = self
            .mobile_plugin_handle
            .run_mobile_plugin_async("requestBatteryExemption", serde_json::json!({}))
            .await
            .map_err(|err| err.to_string())?;
        Ok(())
    }
}

#[cfg(target_os = "android")]
#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AndroidClearWallpaperResponse {
    pub message: String,
}

#[cfg(target_os = "android")]
#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AndroidScheduleResponse {
    pub message: String,
}

pub fn init<R: Runtime>() -> TauriPlugin<R> {
    tauri::plugin::Builder::new("wallpaper-platform")
        .setup(|app, _api| {
            #[cfg(target_os = "android")]
            let handle =
                _api.register_android_plugin(PLUGIN_IDENTIFIER, "WallpaperPlatformPlugin")?;

            app.manage(PlatformWallpaper {
                #[cfg(mobile)]
                mobile_plugin_handle: handle,
                #[cfg(not(mobile))]
                _marker: std::marker::PhantomData::<fn() -> R>,
            });
            Ok(())
        })
        .build()
}

#[cfg(test)]
mod tests {
    #[test]
    fn platform_module_compiles() {
        // 平台模块结构正确，Android 特定类型由 cfg(target_os = "android") 保护。
    }
}
