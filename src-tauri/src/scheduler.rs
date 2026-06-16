use std::sync::{mpsc, Mutex};
#[cfg(not(any(target_os = "android", target_os = "ios")))]
use std::time::Duration;

use serde::Serialize;

use crate::AppConfig;

#[derive(Debug, Clone, Serialize)]
pub struct ScheduleStatus {
    pub enabled: bool,
    pub message: String,
}

#[derive(Default)]
pub struct SchedulerState {
    stop_tx: Mutex<Option<mpsc::Sender<()>>>,
    status: Mutex<Option<ScheduleStatus>>,
    config: Mutex<Option<AppConfig>>,
}

impl SchedulerState {
    #[cfg(any(test, target_os = "android"))]
    pub fn configure(&self, config: AppConfig) -> Result<ScheduleStatus, String> {
        self.remember_config(config.clone())?;
        self.stop_current();
        if !config.schedule.enabled {
            return self.set_status(false, "Automatic updates disabled".to_string());
        }

        let interval_minutes = normalized_interval_minutes(&config);
        self.set_status(
            true,
            format!("Automatic updates enabled every {interval_minutes} minute(s)"),
        )
    }

    #[cfg(not(any(target_os = "android", target_os = "ios")))]
    pub fn configure_for_desktop(&self, config: AppConfig) -> Result<ScheduleStatus, String> {
        self.remember_config(config.clone())?;
        self.stop_current();
        if !config.schedule.enabled {
            return self.set_status(false, "Automatic updates disabled".to_string());
        }

        let interval_minutes = normalized_interval_minutes(&config);
        let interval = Duration::from_secs(u64::from(interval_minutes) * 60);
        let (stop_tx, stop_rx) = mpsc::channel();
        {
            let mut guard = self
                .stop_tx
                .lock()
                .map_err(|_| "Failed to lock scheduler stop channel".to_string())?;
            *guard = Some(stop_tx);
        }

        std::thread::Builder::new()
            .name("wallpaper-scheduler".to_string())
            .spawn(move || loop {
                match stop_rx.recv_timeout(interval) {
                    Ok(_) | Err(mpsc::RecvTimeoutError::Disconnected) => break,
                    Err(mpsc::RecvTimeoutError::Timeout) => {
                        let config = config.clone();
                        tauri::async_runtime::block_on(async move {
                            if let Err(err) = crate::run_desktop_manual_update(config).await {
                                eprintln!("Scheduled wallpaper update failed: {err}");
                            }
                        });
                    }
                }
            })
            .map_err(|err| format!("Failed to start scheduler thread: {err}"))?;

        self.set_status(
            true,
            format!("Automatic updates enabled every {interval_minutes} minute(s)"),
        )
    }

    pub fn cancel(&self) -> Result<ScheduleStatus, String> {
        self.stop_current();
        self.set_status(false, "Automatic updates disabled".to_string())
    }

    pub fn status(&self) -> Result<ScheduleStatus, String> {
        self.status
            .lock()
            .map_err(|_| "Failed to lock scheduler status".to_string())?
            .clone()
            .ok_or_else(|| "Automatic updates disabled".to_string())
            .or_else(|_| self.set_status(false, "Automatic updates disabled".to_string()))
    }

    #[cfg(not(any(target_os = "android", target_os = "ios")))]
    pub fn current_config(&self) -> Result<Option<AppConfig>, String> {
        self.config
            .lock()
            .map_err(|_| "Failed to lock scheduler config".to_string())
            .map(|guard| guard.clone())
    }

    fn set_status(&self, enabled: bool, message: String) -> Result<ScheduleStatus, String> {
        let status = ScheduleStatus { enabled, message };
        let mut guard = self
            .status
            .lock()
            .map_err(|_| "Failed to lock scheduler status".to_string())?;
        *guard = Some(status.clone());
        Ok(status)
    }

    fn remember_config(&self, config: AppConfig) -> Result<(), String> {
        let mut guard = self
            .config
            .lock()
            .map_err(|_| "Failed to lock scheduler config".to_string())?;
        *guard = Some(config);
        Ok(())
    }

    fn stop_current(&self) {
        if let Ok(mut guard) = self.stop_tx.lock() {
            if let Some(stop_tx) = guard.take() {
                let _ = stop_tx.send(());
            }
        }
    }
}

fn normalized_interval_minutes(config: &AppConfig) -> u32 {
    config.schedule.interval_minutes.max(1)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn disabled_config_reports_schedule_disabled() {
        let scheduler = SchedulerState::default();
        let status = scheduler.configure(AppConfig::default()).unwrap();

        assert!(!status.enabled);
        assert_eq!(status.message, "Automatic updates disabled");
    }

    #[test]
    fn enabled_config_reports_interval_minutes() {
        let scheduler = SchedulerState::default();
        let mut config = AppConfig::default();
        config.schedule.enabled = true;
        config.schedule.interval_minutes = 42;

        let status = scheduler.configure(config).unwrap();

        assert!(status.enabled);
        assert!(status.message.contains("42"));
    }

    #[test]
    fn zero_interval_is_clamped_to_one_minute() {
        let scheduler = SchedulerState::default();
        let mut config = AppConfig::default();
        config.schedule.enabled = true;
        config.schedule.interval_minutes = 0;

        let status = scheduler.configure(config).unwrap();

        assert!(status.enabled);
        assert!(status.message.contains("1"));
    }
}
