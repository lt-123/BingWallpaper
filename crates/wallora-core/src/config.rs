use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum FitMode {
    Fill,
    Fit,
    Stretch,
    Center,
}

impl FitMode {
    pub fn css_object_fit(self) -> &'static str {
        match self {
            Self::Fill => "cover",
            Self::Fit => "contain",
            Self::Stretch => "fill",
            Self::Center => "none",
        }
    }

    pub fn as_str(self) -> &'static str {
        match self {
            Self::Fill => "Fill",
            Self::Fit => "Fit",
            Self::Stretch => "Stretch",
            Self::Center => "Center",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
pub enum ScheduleMode {
    /// 按固定时间间隔触发
    #[default]
    Interval,
    /// 每天在指定时刻触发（daily_times 列表中的 "HH:MM" 时间点）
    DailyAt,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ScheduleConfig {
    pub enabled: bool,
    pub mode: ScheduleMode,
    pub interval_minutes: u32,
    /// 每日触发时刻列表，格式 "HH:MM"，仅 mode == DailyAt 时生效
    pub daily_times: Vec<String>,
    pub notify_on_background_update: bool,
}

impl Default for ScheduleConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            mode: ScheduleMode::default(),
            interval_minutes: 360,
            daily_times: Vec::new(),
            notify_on_background_update: true,
        }
    }
}

impl ScheduleConfig {
    pub fn enabled_every_minutes(interval_minutes: u32, notify_on_background_update: bool) -> Self {
        Self {
            enabled: true,
            interval_minutes,
            ..Default::default()
        }
        .with_notify(notify_on_background_update)
    }

    fn with_notify(mut self, notify: bool) -> Self {
        self.notify_on_background_update = notify;
        self
    }
}
