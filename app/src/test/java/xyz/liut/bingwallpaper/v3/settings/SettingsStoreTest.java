package xyz.liut.bingwallpaper.v3.settings;

import org.junit.Assert;
import org.junit.Test;

import xyz.liut.bingwallpaper.BaseTestCase;
import xyz.liut.bingwallpaper.v3.source.BingSourceOptions;
import xyz.liut.bingwallpaper.v3.wallpaper.WallpaperFitMode;

/**
 * v3 设置仓库测试。
 */
public class SettingsStoreTest extends BaseTestCase {

    @Test
    public void bingSourceOptionsDefaultsToNoMarketAndGlobalHost() {
        SettingsStore settingsStore = new SettingsStore(context);

        BingSourceOptions options = settingsStore.bingSourceOptions();

        Assert.assertEquals("", options.getMarket());
        Assert.assertEquals(BingSourceOptions.HOST_GLOBAL, options.getImageHost());
    }

    @Test
    public void bingMarketRoundTripsThroughSettingsStore() {
        SettingsStore settingsStore = new SettingsStore(context);

        settingsStore.bingMarket(" en-WW ");

        Assert.assertEquals("en-WW", settingsStore.bingMarket());
    }

    @Test
    public void chinaMarketUsesChinaImageHost() {
        SettingsStore settingsStore = new SettingsStore(context);

        settingsStore.bingMarket(BingSourceOptions.MARKET_CHINA);

        Assert.assertEquals(BingSourceOptions.HOST_CHINA,
                settingsStore.bingSourceOptions().getImageHost());
    }

    @Test
    public void bingResolutionRoundTripsThroughSettingsStore() {
        SettingsStore settingsStore = new SettingsStore(context);

        settingsStore.bingResolution(BingSourceOptions.RESOLUTION_1920_1080);

        Assert.assertEquals(BingSourceOptions.RESOLUTION_1920_1080, settingsStore.bingResolution());
        Assert.assertEquals(BingSourceOptions.RESOLUTION_1920_1080,
                settingsStore.bingSourceOptions().getResolution());
    }

    @Test
    public void invalidBingResolutionFallsBackToUhd() {
        SettingsStore settingsStore = new SettingsStore(context);

        settingsStore.bingResolution("100x100");

        Assert.assertEquals(BingSourceOptions.RESOLUTION_UHD, settingsStore.bingResolution());
    }

    @Test
    public void wallpaperFitModeRoundTripsThroughSettingsStore() {
        SettingsStore settingsStore = new SettingsStore(context);

        settingsStore.wallpaperFitMode(WallpaperFitMode.CONTAIN);

        Assert.assertEquals(WallpaperFitMode.CONTAIN, settingsStore.wallpaperFitMode());
    }

    @Test
    public void nullWallpaperFitModeFallsBackToSystem() {
        SettingsStore settingsStore = new SettingsStore(context);

        settingsStore.wallpaperFitMode(null);

        Assert.assertEquals(WallpaperFitMode.SYSTEM, settingsStore.wallpaperFitMode());
    }
}
