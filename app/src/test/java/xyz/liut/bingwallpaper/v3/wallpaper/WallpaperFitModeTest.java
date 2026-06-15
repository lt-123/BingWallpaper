package xyz.liut.bingwallpaper.v3.wallpaper;

import org.junit.Assert;
import org.junit.Test;

/**
 * 壁纸装配模式测试。
 */
public class WallpaperFitModeTest {

    @Test
    public void fromValueRoundTripsKnownValues() {
        Assert.assertEquals(WallpaperFitMode.SYSTEM, WallpaperFitMode.fromValue("system"));
        Assert.assertEquals(WallpaperFitMode.CROP, WallpaperFitMode.fromValue("crop"));
        Assert.assertEquals(WallpaperFitMode.CONTAIN, WallpaperFitMode.fromValue("contain"));
        Assert.assertEquals(WallpaperFitMode.STRETCH, WallpaperFitMode.fromValue("stretch"));
    }

    @Test
    public void fromValueFallsBackToSystem() {
        Assert.assertEquals(WallpaperFitMode.SYSTEM, WallpaperFitMode.fromValue(null));
        Assert.assertEquals(WallpaperFitMode.SYSTEM, WallpaperFitMode.fromValue("bad"));
    }
}
