#[cfg(not(any(target_os = "android", target_os = "ios")))]
use tauri::{
    menu::{Menu, MenuItem},
    tray::TrayIconBuilder,
    App, Manager, Runtime, WindowEvent,
};

#[cfg(not(any(target_os = "android", target_os = "ios")))]
const SHOW_ID: &str = "show";
#[cfg(not(any(target_os = "android", target_os = "ios")))]
const UPDATE_NOW_ID: &str = "update_now";
#[cfg(not(any(target_os = "android", target_os = "ios")))]
const TOGGLE_SCHEDULE_ID: &str = "toggle_schedule";
#[cfg(not(any(target_os = "android", target_os = "ios")))]
const QUIT_ID: &str = "quit";

#[cfg(not(any(target_os = "android", target_os = "ios")))]
pub fn setup_tray<R: Runtime>(app: &mut App<R>) -> tauri::Result<()> {
    let show = MenuItem::with_id(app, SHOW_ID, "显示窗口", true, None::<&str>)?;
    let update_now = MenuItem::with_id(app, UPDATE_NOW_ID, "立即更新", true, None::<&str>)?;
    let toggle_schedule =
        MenuItem::with_id(app, TOGGLE_SCHEDULE_ID, "暂停/启用自动更新", true, None::<&str>)?;
    let quit = MenuItem::with_id(app, QUIT_ID, "退出", true, None::<&str>)?;
    let menu = Menu::with_items(app, &[&show, &update_now, &toggle_schedule, &quit])?;

    let mut tray = TrayIconBuilder::new()
        .menu(&menu)
        .tooltip("Wallpaper Client")
        .on_menu_event(|app, event| match event.id().as_ref() {
            SHOW_ID => show_main_window(app),
            UPDATE_NOW_ID => update_now_from_tray(app),
            TOGGLE_SCHEDULE_ID => toggle_schedule_from_tray(app),
            QUIT_ID => app.exit(0),
            _ => {}
        });

    if let Some(icon) = app.default_window_icon().cloned() {
        tray = tray.icon(icon);
    }
    tray.build(app)?;
    Ok(())
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
pub fn handle_window_event<R: Runtime>(window: &tauri::Window<R>, event: &WindowEvent) {
    if let WindowEvent::CloseRequested { api, .. } = event {
        api.prevent_close();
        let _ = window.hide();
    }
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
fn show_main_window<R: Runtime>(app: &tauri::AppHandle<R>) {
    if let Some(window) = app.get_webview_window("main") {
        let _ = window.show();
        let _ = window.set_focus();
    }
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
fn update_now_from_tray<R: Runtime>(app: &tauri::AppHandle<R>) {
    let scheduler = app.state::<crate::scheduler::SchedulerState>();
    match scheduler.current_config() {
        Ok(Some(config)) => {
            tauri::async_runtime::spawn(async move {
                if let Err(err) = crate::run_desktop_manual_update(config).await {
                    eprintln!("Tray wallpaper update failed: {err}");
                }
            });
        }
        Ok(None) => eprintln!("Tray wallpaper update skipped: no schedule config is available"),
        Err(err) => eprintln!("Tray wallpaper update skipped: {err}"),
    }
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
fn toggle_schedule_from_tray<R: Runtime>(app: &tauri::AppHandle<R>) {
    let scheduler = app.state::<crate::scheduler::SchedulerState>();
    let enabled = scheduler.status().map(|status| status.enabled).unwrap_or(false);
    if enabled {
        if let Err(err) = scheduler.cancel() {
            eprintln!("Failed to cancel schedule from tray: {err}");
        }
        return;
    }

    match scheduler.current_config() {
        Ok(Some(mut config)) => {
            config.schedule.enabled = true;
            if let Err(err) = scheduler.configure_for_desktop(config) {
                eprintln!("Failed to enable schedule from tray: {err}");
            }
        }
        Ok(None) => eprintln!("Schedule cannot be enabled from tray before settings are saved"),
        Err(err) => eprintln!("Failed to read schedule config from tray: {err}"),
    }
}
