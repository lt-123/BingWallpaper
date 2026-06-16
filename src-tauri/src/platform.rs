use serde::{Deserialize, Serialize};
use tauri::{plugin::TauriPlugin, Manager, Runtime};

use wallpaper_core::config::FitMode;

#[cfg(mobile)]
use tauri::plugin::PluginHandle;

#[cfg(target_os = "android")]
const PLUGIN_IDENTIFIER: &str = "com.liut.wallpaper.platform";

#[derive(Debug, Clone, PartialEq, Eq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AndroidSaveWallpaperPayload {
    pub file_name: String,
    pub mime_type: String,
    pub image_base64: String,
    pub fit_mode: FitMode,
    pub set_lock_screen: bool,
    pub save_to_gallery: bool,
    pub show_toast: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AndroidSaveWallpaperResponse {
    pub uri: String,
    pub applied_home_screen: bool,
    pub applied_lock_screen: bool,
}

pub struct PlatformWallpaper<R: Runtime> {
    #[cfg(mobile)]
    mobile_plugin_handle: PluginHandle<R>,
    #[cfg(not(mobile))]
    _marker: std::marker::PhantomData<fn() -> R>,
}

impl<R: Runtime> PlatformWallpaper<R> {
    #[cfg(target_os = "android")]
    pub async fn save_and_apply_android(
        &self,
        payload: AndroidSaveWallpaperPayload,
    ) -> Result<AndroidSaveWallpaperResponse, String> {
        self.mobile_plugin_handle
            .run_mobile_plugin_async("saveAndApplyWallpaper", payload)
            .await
            .map_err(|err| err.to_string())
    }

    #[cfg(target_os = "android")]
    pub async fn clear_android_wallpaper(&self) -> Result<String, String> {
        let response: AndroidClearWallpaperResponse = self
            .mobile_plugin_handle
            .run_mobile_plugin_async("clearWallpaper", serde_json::json!({}))
            .await
            .map_err(|err| err.to_string())?;
        Ok(response.message)
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AndroidClearWallpaperResponse {
    pub message: String,
}

pub fn init<R: Runtime>() -> TauriPlugin<R> {
    tauri::plugin::Builder::new("wallpaper-platform")
        .setup(|app, api| {
            #[cfg(target_os = "android")]
            let handle =
                api.register_android_plugin(PLUGIN_IDENTIFIER, "WallpaperPlatformPlugin")?;

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
    use super::*;

    #[test]
    fn android_wallpaper_payload_serializes_for_kotlin_plugin() {
        let payload = AndroidSaveWallpaperPayload {
            file_name: "bing-20260616-sample.jpg".to_string(),
            mime_type: "image/jpeg".to_string(),
            image_base64: "AQID".to_string(),
            fit_mode: FitMode::Fit,
            set_lock_screen: true,
            save_to_gallery: true,
            show_toast: true,
        };

        let json = serde_json::to_value(payload).unwrap();

        assert_eq!(json["fileName"], "bing-20260616-sample.jpg");
        assert_eq!(json["mimeType"], "image/jpeg");
        assert_eq!(json["imageBase64"], "AQID");
        assert_eq!(json["fitMode"], "Fit");
        assert_eq!(json["setLockScreen"], true);
        assert_eq!(json["saveToGallery"], true);
        assert_eq!(json["showToast"], true);
    }
}
