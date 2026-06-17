use serde::{Deserialize, Serialize};
use url::Url;

use crate::wallpaper::WallpaperItem;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum BingMarket {
    China,
    UnitedStates,
    Japan,
    UnitedKingdom,
    Germany,
    France,
}

impl BingMarket {
    pub fn as_query_value(self) -> &'static str {
        match self {
            Self::China => "zh-CN",
            Self::UnitedStates => "en-US",
            Self::Japan => "ja-JP",
            Self::UnitedKingdom => "en-GB",
            Self::Germany => "de-DE",
            Self::France => "fr-FR",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum BingResolution {
    Standard1920x1080,
    Uhd4k,
    Portrait1080x1920,
}

impl BingResolution {
    fn query_pairs(self) -> Vec<(String, String)> {
        match self {
            Self::Standard1920x1080 | Self::Portrait1080x1920 => Vec::new(),
            Self::Uhd4k => vec![
                ("uhd".to_string(), "1".to_string()),
                ("uhdwidth".to_string(), "3840".to_string()),
                ("uhdheight".to_string(), "2160".to_string()),
            ],
        }
    }

    fn image_url(self, image: &BingImage) -> String {
        match self {
            // Portrait is not a distinct Bing API resolution; construct from urlbase
            // by appending the resolution suffix (same pattern as Bing's own thumbnails).
            Self::Portrait1080x1920 => urlbase_url(&image.urlbase, "_1080x1920.jpg"),
            // API response already reflects the requested resolution.
            _ => {
                if image.url.starts_with("http") {
                    image.url.clone()
                } else {
                    format!("https://www.bing.com{}", image.url)
                }
            }
        }
    }
}

/// 将 Bing urlbase（可能为相对路径或绝对 URL）与分辨率后缀拼成完整 URL。
fn urlbase_url(urlbase: &str, suffix: &str) -> String {
    if urlbase.starts_with("http") {
        format!("{}{}", urlbase, suffix)
    } else {
        format!("https://www.bing.com{}{}", urlbase, suffix)
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct BingConfig {
    pub market: BingMarket,
    pub resolution: BingResolution,
    pub count: u8,
}

impl Default for BingConfig {
    fn default() -> Self {
        Self {
            market: BingMarket::UnitedStates,
            resolution: BingResolution::Standard1920x1080,
            count: 1,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SourceRequest {
    pub endpoint: &'static str,
    pub query_pairs: Vec<(String, String)>,
}

impl SourceRequest {
    pub fn to_url(&self) -> Url {
        let mut url = Url::parse(self.endpoint).expect("Bing endpoint must be a valid URL");
        url.query_pairs_mut().extend_pairs(
            self.query_pairs
                .iter()
                .map(|(key, value)| (&**key, &**value)),
        );
        url
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct BingSource {
    config: BingConfig,
}

impl BingSource {
    pub fn new(config: BingConfig) -> Self {
        Self { config }
    }

    pub fn archive_request(&self, index: u8) -> SourceRequest {
        let mut query_pairs = vec![
            ("format".to_string(), "js".to_string()),
            ("idx".to_string(), index.to_string()),
            ("n".to_string(), self.config.count.to_string()),
            (
                "mkt".to_string(),
                self.config.market.as_query_value().to_string(),
            ),
        ];
        query_pairs.extend(self.config.resolution.query_pairs());

        SourceRequest {
            endpoint: "https://www.bing.com/HPImageArchive.aspx",
            query_pairs,
        }
    }
}

#[derive(Debug, Clone, Deserialize)]
pub struct BingWallpaperResponse {
    images: Vec<BingImage>,
}

impl BingWallpaperResponse {
    pub fn into_wallpapers(self, resolution: BingResolution) -> Vec<WallpaperItem> {
        self.images
            .into_iter()
            .map(|image| {
                let image_url = resolution.image_url(&image);
                let thumbnail_url = urlbase_url(&image.urlbase, "_1366x768.jpg");
                let preview_url = urlbase_url(&image.urlbase, "_1920x1080.jpg");
                WallpaperItem {
                    source_id: "bing".to_string(),
                    source_wallpaper_id: image.urlbase,
                    title: image.title,
                    description: image.copyright,
                    published_date: image.startdate,
                    thumbnail_url,
                    preview_url,
                    image_url,
                    detail_url: image.copyrightlink,
                }
            })
            .collect()
    }
}

#[derive(Debug, Clone, Deserialize)]
struct BingImage {
    startdate: String,
    url: String,
    urlbase: String,
    copyright: String,
    copyrightlink: Option<String>,
    title: String,
}
