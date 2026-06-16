use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct WallpaperItem {
    pub source_id: String,
    pub source_wallpaper_id: String,
    pub title: String,
    pub description: String,
    pub published_date: String,
    pub image_url: String,
    pub detail_url: Option<String>,
}

impl WallpaperItem {
    pub fn download_file_name(&self) -> String {
        let title = self
            .title
            .chars()
            .map(|ch| {
                if ch.is_ascii_alphanumeric() || ch == '-' {
                    ch
                } else {
                    '_'
                }
            })
            .collect::<String>()
            .trim_matches('_')
            .to_string();

        format!("{}-{}-{}.jpg", self.source_id, self.published_date, title)
    }
}
