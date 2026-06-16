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
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ScheduleConfig {
    pub enabled: bool,
    pub interval_minutes: u32,
    pub notify_on_background_update: bool,
}

impl ScheduleConfig {
    pub fn enabled_every_minutes(interval_minutes: u32, notify_on_background_update: bool) -> Self {
        Self {
            enabled: true,
            interval_minutes,
            notify_on_background_update,
        }
    }
}
