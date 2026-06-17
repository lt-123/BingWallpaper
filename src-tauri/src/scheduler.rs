use std::sync::{mpsc, Mutex};
#[cfg(not(any(target_os = "android", target_os = "ios")))]
use std::time::Duration;

use serde::Serialize;

use crate::AppConfig;
use wallora_core::config::ScheduleMode;

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

        let msg = schedule_enabled_message(&config);
        self.set_status(true, msg)
    }

    #[cfg(not(any(target_os = "android", target_os = "ios")))]
    pub fn configure_for_desktop(&self, config: AppConfig) -> Result<ScheduleStatus, String> {
        self.remember_config(config.clone())?;
        self.stop_current();
        if !config.schedule.enabled {
            return self.set_status(false, "Automatic updates disabled".to_string());
        }

        let (stop_tx, stop_rx) = mpsc::channel();
        {
            let mut guard = self
                .stop_tx
                .lock()
                .map_err(|_| "Failed to lock scheduler stop channel".to_string())?;
            *guard = Some(stop_tx);
        }

        let msg = schedule_enabled_message(&config);

        std::thread::Builder::new()
            .name("wallpaper-scheduler".to_string())
            .spawn(move || loop {
                let delay = next_trigger_delay(&config);
                match stop_rx.recv_timeout(delay) {
                    Ok(_) | Err(mpsc::RecvTimeoutError::Disconnected) => break,
                    Err(mpsc::RecvTimeoutError::Timeout) => {
                        let config = config.clone();
                        tauri::async_runtime::block_on(async move {
                            if let Err(err) = crate::run_desktop_scheduled_update(config).await {
                                eprintln!("Scheduled wallpaper update failed: {err}");
                            }
                        });
                    }
                }
            })
            .map_err(|err| format!("Failed to start scheduler thread: {err}"))?;

        self.set_status(true, msg)
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

fn schedule_enabled_message(config: &AppConfig) -> String {
    match config.schedule.mode {
        ScheduleMode::Interval => {
            let mins = config.schedule.interval_minutes.max(1);
            format!("Automatic updates enabled every {mins} minute(s)")
        }
        ScheduleMode::DailyAt => {
            if config.schedule.daily_times.is_empty() {
                "Automatic updates enabled (no times configured)".to_string()
            } else {
                format!("Automatic updates enabled at: {}", config.schedule.daily_times.join(", "))
            }
        }
    }
}

/// 计算到下一次触发的等待时长。
/// Interval 模式：固定等待 interval_minutes。
/// DailyAt 模式：使用本地时间计算到最近下一个时间点的差值；
///   若列表为空则回退到每小时检查一次。
#[cfg(not(any(target_os = "android", target_os = "ios")))]
fn next_trigger_delay(config: &AppConfig) -> Duration {
    match config.schedule.mode {
        ScheduleMode::Interval => {
            let mins = config.schedule.interval_minutes.max(1);
            Duration::from_secs(u64::from(mins) * 60)
        }
        ScheduleMode::DailyAt => duration_to_next_daily_trigger(&config.schedule.daily_times),
    }
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
fn duration_to_next_daily_trigger(daily_times: &[String]) -> Duration {
    use chrono::{Local, NaiveTime, Timelike};

    if daily_times.is_empty() {
        return Duration::from_secs(3600);
    }

    let now = Local::now();
    let now_secs = now.time().num_seconds_from_midnight() as i64;
    let day_secs = 86_400i64;

    let min_delay = daily_times
        .iter()
        .filter_map(|t| NaiveTime::parse_from_str(t, "%H:%M").ok())
        .map(|t| {
            let target_secs = t.num_seconds_from_midnight() as i64;
            let diff = target_secs - now_secs;
            // 若时间点已过则等到明天
            if diff > 0 { diff } else { diff + day_secs }
        })
        .min()
        .unwrap_or(day_secs);

    Duration::from_secs(min_delay as u64)
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
    fn enabled_interval_config_reports_interval_minutes() {
        let scheduler = SchedulerState::default();
        let mut config = AppConfig::default();
        config.schedule.enabled = true;
        config.schedule.mode = ScheduleMode::Interval;
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
        config.schedule.mode = ScheduleMode::Interval;
        config.schedule.interval_minutes = 0;

        let status = scheduler.configure(config).unwrap();

        assert!(status.enabled);
        assert!(status.message.contains("1"));
    }

    #[test]
    fn daily_at_mode_lists_times_in_message() {
        let scheduler = SchedulerState::default();
        let mut config = AppConfig::default();
        config.schedule.enabled = true;
        config.schedule.mode = ScheduleMode::DailyAt;
        config.schedule.daily_times = vec!["08:00".to_string(), "18:00".to_string()];

        let status = scheduler.configure(config).unwrap();

        assert!(status.enabled);
        assert!(status.message.contains("08:00"));
        assert!(status.message.contains("18:00"));
    }

    #[test]
    fn daily_at_mode_with_empty_times_still_enables() {
        let scheduler = SchedulerState::default();
        let mut config = AppConfig::default();
        config.schedule.enabled = true;
        config.schedule.mode = ScheduleMode::DailyAt;
        config.schedule.daily_times = vec![];

        let status = scheduler.configure(config).unwrap();

        assert!(status.enabled);
    }
}
