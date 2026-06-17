use wallora_core::bing::{
    BingConfig, BingMarket, BingResolution, BingSource, BingWallpaperResponse,
};
use wallora_core::config::{FitMode, ScheduleConfig};

#[test]
fn bing_source_builds_archive_request_from_config() {
    let source = BingSource::new(BingConfig {
        market: BingMarket::China,
        resolution: BingResolution::Uhd4k,
        count: 8,
    });

    let request = source.archive_request(3);

    assert_eq!(request.endpoint, "https://www.bing.com/HPImageArchive.aspx");
    assert_eq!(
        request.query_pairs,
        vec![
            ("format".to_string(), "js".to_string()),
            ("idx".to_string(), "3".to_string()),
            ("n".to_string(), "8".to_string()),
            ("mkt".to_string(), "zh-CN".to_string()),
            ("uhd".to_string(), "1".to_string()),
            ("uhdwidth".to_string(), "3840".to_string()),
            ("uhdheight".to_string(), "2160".to_string()),
        ]
    );
    assert_eq!(
        request.to_url().as_str(),
        "https://www.bing.com/HPImageArchive.aspx?format=js&idx=3&n=8&mkt=zh-CN&uhd=1&uhdwidth=3840&uhdheight=2160"
    );
}

#[test]
fn schedule_config_accepts_enabled_interval_with_notification_preference() {
    let schedule = ScheduleConfig::enabled_every_minutes(180, false);

    assert!(schedule.enabled);
    assert_eq!(schedule.interval_minutes, 180);
    assert!(!schedule.notify_on_background_update);
}

#[test]
fn fit_mode_css_values_match_wallpaper_assembly_modes() {
    assert_eq!(FitMode::Fill.css_object_fit(), "cover");
    assert_eq!(FitMode::Fit.css_object_fit(), "contain");
    assert_eq!(FitMode::Stretch.css_object_fit(), "fill");
    assert_eq!(FitMode::Center.css_object_fit(), "none");
}

#[test]
fn bing_response_maps_gallery_items_to_absolute_wallpapers() {
    let payload = r#"{
        "images": [
            {
                "startdate": "20260615",
                "fullstartdate": "202606150000",
                "enddate": "20260616",
                "url": "/th?id=OHR.Example_EN-US1234567890_UHD.jpg",
                "urlbase": "/th?id=OHR.Example_EN-US1234567890",
                "copyright": "Example place (Example photographer)",
                "copyrightlink": "https://www.bing.com/search?q=example",
                "title": "Example place",
                "quiz": "/search?q=Bing+homepage+quiz",
                "wp": true,
                "hsh": "abc",
                "drk": 1,
                "top": 1,
                "bot": 1,
                "hs": []
            }
        ],
        "tooltips": {}
    }"#;

    let response: BingWallpaperResponse = serde_json::from_str(payload).unwrap();
    let wallpapers = response.into_wallpapers(BingResolution::Uhd4k);

    assert_eq!(wallpapers.len(), 1);
    assert_eq!(wallpapers[0].source_id, "bing");
    assert_eq!(wallpapers[0].title, "Example place");
    assert_eq!(wallpapers[0].published_date, "20260615");
    assert_eq!(
        wallpapers[0].thumbnail_url,
        "https://www.bing.com/th?id=OHR.Example_EN-US1234567890_1366x768.jpg"
    );
    assert_eq!(
        wallpapers[0].preview_url,
        "https://www.bing.com/th?id=OHR.Example_EN-US1234567890_1920x1080.jpg"
    );
    assert_eq!(
        wallpapers[0].image_url,
        "https://www.bing.com/th?id=OHR.Example_EN-US1234567890_UHD.jpg"
    );
    assert_eq!(
        wallpapers[0].download_file_name(),
        "bing-20260615-Example_place.jpg"
    );
}
