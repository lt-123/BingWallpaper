package xyz.liut.bingwallpaper.v3.settings;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import xyz.liut.bingwallpaper.Constants;
import xyz.liut.bingwallpaper.TimedListManager;
import xyz.liut.bingwallpaper.utils.SpTool;
import xyz.liut.bingwallpaper.v3.source.BingSourceOptions;
import xyz.liut.bingwallpaper.v3.wallpaper.WallpaperFitMode;

/**
 * v3 设置存储入口，统一封装 SharedPreferences 读写并复用旧版定时列表存储。
 */
public class SettingsStore {

    private final Context appContext;
    private final SpTool spTool;

    public SettingsStore(Context context) {
        appContext = context.getApplicationContext();
        spTool = SpTool.getDefault(appContext);
    }

    public boolean saveToGallery() {
        return spTool.get(Constants.Default.KEY_SAVE_TO_GALLERY, true);
    }

    public void saveToGallery(boolean saveToGallery) {
        spTool.save(Constants.Default.KEY_SAVE_TO_GALLERY, saveToGallery);
    }

    public boolean setLockScreen() {
        return spTool.get(Constants.Default.KEY_LOCK_SCREEN, false);
    }

    public void setLockScreen(boolean setLockScreen) {
        spTool.save(Constants.Default.KEY_LOCK_SCREEN, setLockScreen);
    }

    public boolean showToast() {
        return spTool.get(Constants.Default.KEY_SHOW_TOAST, true);
    }

    public void showToast(boolean showToast) {
        spTool.save(Constants.Default.KEY_SHOW_TOAST, showToast);
    }

    public boolean showManualEntry() {
        return spTool.get(Constants.Default.KEY_SHOW_MANUAL_SYNC, false);
    }

    public void showManualEntry(boolean showManualEntry) {
        spTool.save(Constants.Default.KEY_SHOW_MANUAL_SYNC, showManualEntry);
    }

    public boolean hideMainIcon() {
        return spTool.get(Constants.Default.KEY_HIDE_MAIN, false);
    }

    public void hideMainIcon(boolean hideMainIcon) {
        spTool.save(Constants.Default.KEY_HIDE_MAIN, hideMainIcon);
    }

    public List<String> timedList() {
        return new ArrayList<>(TimedListManager.loadTimedList(appContext));
    }

    public String bingMarket() {
        return BingSourceOptions.normalizeMarket(
                spTool.get(Constants.Default.KEY_BING_MARKET, BingSourceOptions.MARKET_DEFAULT));
    }

    public void bingMarket(String market) {
        spTool.save(Constants.Default.KEY_BING_MARKET, BingSourceOptions.normalizeMarket(market));
    }

    public String bingResolution() {
        return BingSourceOptions.normalizeResolution(
                spTool.get(Constants.Default.KEY_BING_RESOLUTION, BingSourceOptions.RESOLUTION_UHD));
    }

    public void bingResolution(String resolution) {
        spTool.save(Constants.Default.KEY_BING_RESOLUTION,
                BingSourceOptions.normalizeResolution(resolution));
    }

    public WallpaperFitMode wallpaperFitMode() {
        return WallpaperFitMode.fromValue(
                spTool.get(Constants.Default.KEY_WALLPAPER_FIT_MODE, WallpaperFitMode.SYSTEM.value()));
    }

    public void wallpaperFitMode(WallpaperFitMode mode) {
        WallpaperFitMode fitMode = mode == null ? WallpaperFitMode.SYSTEM : mode;
        spTool.save(Constants.Default.KEY_WALLPAPER_FIT_MODE, fitMode.value());
    }

    public BingSourceOptions bingSourceOptions() {
        return new BingSourceOptions(bingMarket(), bingResolution());
    }
}
