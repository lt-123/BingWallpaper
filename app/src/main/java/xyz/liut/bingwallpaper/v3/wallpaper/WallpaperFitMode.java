package xyz.liut.bingwallpaper.v3.wallpaper;

/**
 * 壁纸图片与屏幕尺寸不匹配时的装配模式。
 */
public enum WallpaperFitMode {
    SYSTEM("system"),
    CROP("crop"),
    CONTAIN("contain"),
    STRETCH("stretch");

    private final String value;

    WallpaperFitMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static WallpaperFitMode fromValue(String value) {
        if (value == null) {
            return SYSTEM;
        }
        for (WallpaperFitMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        return SYSTEM;
    }
}
