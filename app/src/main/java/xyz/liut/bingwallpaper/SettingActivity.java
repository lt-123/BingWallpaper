package xyz.liut.bingwallpaper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import xyz.liut.bingwallpaper.utils.ComponentUtil;
import xyz.liut.bingwallpaper.utils.ToastUtil;
import xyz.liut.bingwallpaper.utils.WallpaperTool;
import xyz.liut.bingwallpaper.v3.permissions.NotificationPermissionHelper;
import xyz.liut.bingwallpaper.v3.schedule.ScheduleManager;
import xyz.liut.bingwallpaper.v3.settings.SettingsStore;
import xyz.liut.bingwallpaper.v3.source.BingSourceOptions;
import xyz.liut.bingwallpaper.v3.wallpaper.WallpaperFitMode;

/**
 * 主界面 设置页
 */
public class SettingActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    private static final String[] MARKET_VALUES = {
            BingSourceOptions.MARKET_DEFAULT,
            BingSourceOptions.MARKET_CHINA,
            BingSourceOptions.MARKET_GLOBAL,
            BingSourceOptions.MARKET_US,
            BingSourceOptions.MARKET_UK,
            BingSourceOptions.MARKET_JAPAN
    };

    private static final WallpaperFitMode[] FIT_MODE_VALUES = {
            WallpaperFitMode.SYSTEM,
            WallpaperFitMode.CROP,
            WallpaperFitMode.CONTAIN,
            WallpaperFitMode.STRETCH
    };

    private TextView tvTime, tvSave, tvSetLockScreen, tvShowToast, tvShowManual, tvHideMain,
            tvBingMarket, tvBingResolution, tvWallpaperFitMode;

    private Switch swSave, swLockScreen, swShowToast, swShowManual, swHideMain;

    private SettingsStore settingsStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        settingsStore = new SettingsStore(this);

        tvTime = findViewById(R.id.tv_time);
        tvSave = findViewById(R.id.tv_save_path);
        tvSetLockScreen = findViewById(R.id.tv_set_lock_screen);
        tvShowToast = findViewById(R.id.tv_show_toast);
        tvShowManual = findViewById(R.id.tv_show_manual);
        tvHideMain = findViewById(R.id.tv_hide_main);
        tvBingMarket = findViewById(R.id.tv_bing_market);
        tvBingResolution = findViewById(R.id.tv_bing_resolution);
        tvWallpaperFitMode = findViewById(R.id.tv_wallpaper_fit_mode);

        swSave = findViewById(R.id.sw_save);
        swLockScreen = findViewById(R.id.sw_lock_screen);
        swShowToast = findViewById(R.id.sw_show_toast);
        swShowManual = findViewById(R.id.sw_show_manual);
        swHideMain = findViewById(R.id.sw_show_main);

        findViewById(R.id.ll_time).setOnClickListener(this);
        findViewById(R.id.ll_bing_market).setOnClickListener(this);
        findViewById(R.id.ll_bing_resolution).setOnClickListener(this);
        findViewById(R.id.ll_wallpaper_fit_mode).setOnClickListener(this);

        swSave.setOnClickListener(this);
        swLockScreen.setOnClickListener(this);
        swShowManual.setOnClickListener(this);
        swShowToast.setOnClickListener(this);
        swHideMain.setOnClickListener(this);

        findViewById(R.id.bt_setup_now).setOnClickListener(this);
        findViewById(R.id.bt_clear).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadData();
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.ll_time) {
            // 设置定时任务
            startActivity(new Intent(this, TimeListActivity.class));
        } else if (id == R.id.ll_bing_market) {
            showBingMarketDialog();
        } else if (id == R.id.ll_bing_resolution) {
            showBingResolutionDialog();
        } else if (id == R.id.ll_wallpaper_fit_mode) {
            showWallpaperFitModeDialog();
        } else if (id == R.id.sw_save) {
            // 保存到相册
            settingsStore.saveToGallery(swSave.isChecked());
            refreshSubText();
        } else if (id == R.id.sw_lock_screen) {
            // 同时设置锁屏
            settingsStore.setLockScreen(swLockScreen.isChecked());
            refreshSubText();
        } else if (id == R.id.sw_show_toast) {
            // 显示同步提示
            settingsStore.showToast(swShowToast.isChecked());
            refreshSubText();
        } else if (id == R.id.sw_show_manual) {
            // 启用手动
            ComponentUtil.setComponentEnable(this,
                    new ComponentName(this, ManualSetActivity.class),
                    swShowManual.isChecked());
            settingsStore.showManualEntry(swShowManual.isChecked());
            refreshSubText();
        } else if (id == R.id.sw_show_main) {
            // 启用主页
            ComponentUtil.setComponentEnable(this,
                    new ComponentName(BuildConfig.APPLICATION_ID, "xyz.liut.bingwallpaper.MainActivity"),
                    !swHideMain.isChecked());
            settingsStore.hideMainIcon(swHideMain.isChecked());
            refreshSubText();
        } else if (id == R.id.bt_setup_now) {
            // 立即设置
            syncWallpaperWithNotificationPermission();
        } else if (id == R.id.bt_clear) {
            // 清空壁纸
            WallpaperTool.clearWallpaper(this);
        }
    }

    private void syncWallpaperWithNotificationPermission() {
        if (!NotificationPermissionHelper.hasPermission(this)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
            return;
        }
        startSyncWallpaper();
    }

    private void startSyncWallpaper() {
        SyncWallpaperService.start(this);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ToastUtil.showToast(this, "未授予通知权限，同步通知可能不可见");
            }
            startSyncWallpaper();
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadData() {
        // 开关选项
        boolean save = settingsStore.saveToGallery();
        boolean lockScreen = settingsStore.setLockScreen();
        boolean showToast = settingsStore.showToast();
        boolean showManual = settingsStore.showManualEntry();
        boolean hideMain = settingsStore.hideMainIcon();
        swSave.setChecked(save);
        swLockScreen.setChecked(lockScreen);
        swShowToast.setChecked(showToast);
        swShowManual.setChecked(showManual);
        swHideMain.setChecked(hideMain);

        List<String> timedList = settingsStore.timedList();
        if (timedList.isEmpty()) {
            tvTime.setText(getString(R.string.no_timed));
        } else {
            tvTime.setText(getString(R.string.every_day) + ": " + TextUtils.join("/", timedList));
        }

        // 配置上屏
        refreshSubText();
        refreshBingSourceText();

        // 设置定时
        timed(timedList);
    }


    @SuppressLint("SetTextI18n")
    private void refreshSubText() {
        if (swSave.isChecked()) {
            tvSave.setText(R.string.save_to_gallery_desc);
        } else {
            tvSave.setText(R.string.no_save);
        }

        if (swLockScreen.isChecked()) {
            tvSetLockScreen.setText("同时设置锁屏壁纸已开启(仅支持7.0及以上版本)");
        } else {
            tvSetLockScreen.setText("仅设置桌面壁纸");
        }

        if (swShowToast.isChecked()) {
            tvShowToast.setText(R.string.show_toast_desc);
        } else {
            tvShowToast.setText(R.string.show_toast_desc2);
        }

        if (swShowManual.isChecked()) {
            tvShowManual.setText("显示\"手动同步\"桌面图标");
        } else {
            tvShowManual.setText("不显示\"手动同步\"桌面图标");
        }

        if (swHideMain.isChecked()) {
            tvHideMain.setText("隐藏应用桌面图标");
        } else {
            tvHideMain.setText("不隐藏应用桌面图标");
        }
    }

    private void showBingMarketDialog() {
        String[] labels = {
                getString(R.string.bing_market_default),
                getString(R.string.bing_market_china),
                getString(R.string.bing_market_global),
                getString(R.string.bing_market_us),
                getString(R.string.bing_market_uk),
                getString(R.string.bing_market_japan),
                getString(R.string.bing_market_custom)
        };
        int customIndex = labels.length - 1;
        new AlertDialog.Builder(this)
                .setTitle(R.string.bing_market)
                .setSingleChoiceItems(labels, currentMarketSelection(settingsStore.bingMarket()), (dialog, which) -> {
                    dialog.dismiss();
                    if (which == customIndex) {
                        showCustomBingMarketDialog();
                    } else {
                        settingsStore.bingMarket(MARKET_VALUES[which]);
                        refreshBingSourceText();
                    }
                })
                .show();
    }

    private void showCustomBingMarketDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.bing_market_custom_hint);
        input.setText(settingsStore.bingMarket());
        input.setSelection(input.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.bing_market_custom_title)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                    String market = input.getText().toString().trim();
                    if (!BingSourceOptions.isValidMarket(market)) {
                        ToastUtil.showToast(this, getString(R.string.bing_market_invalid));
                        return;
                    }
                    settingsStore.bingMarket(market);
                    refreshBingSourceText();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void refreshBingSourceText() {
        String market = settingsStore.bingMarket();
        tvBingMarket.setText(marketLabel(market));
        tvBingResolution.setText(resolutionLabel(settingsStore.bingResolution()));
        tvWallpaperFitMode.setText(fitModeLabel(settingsStore.wallpaperFitMode()));
    }

    private void showBingResolutionDialog() {
        String[] labels = {
                getString(R.string.bing_resolution_uhd),
                getString(R.string.bing_resolution_1920_1080),
                getString(R.string.bing_resolution_1920_1200),
                getString(R.string.bing_resolution_1366_768),
                getString(R.string.bing_resolution_1080_1920),
                getString(R.string.bing_resolution_768_1366)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.bing_resolution)
                .setSingleChoiceItems(labels, currentResolutionSelection(settingsStore.bingResolution()),
                        (dialog, which) -> {
                            dialog.dismiss();
                            settingsStore.bingResolution(BingSourceOptions.RESOLUTION_VALUES[which]);
                            refreshBingSourceText();
                        })
                .show();
    }

    private void showWallpaperFitModeDialog() {
        String[] labels = {
                getString(R.string.wallpaper_fit_mode_system),
                getString(R.string.wallpaper_fit_mode_crop),
                getString(R.string.wallpaper_fit_mode_contain),
                getString(R.string.wallpaper_fit_mode_stretch)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_fit_mode)
                .setSingleChoiceItems(labels, currentFitModeSelection(settingsStore.wallpaperFitMode()),
                        (dialog, which) -> {
                            dialog.dismiss();
                            settingsStore.wallpaperFitMode(FIT_MODE_VALUES[which]);
                            refreshBingSourceText();
                        })
                .show();
    }

    private int currentMarketSelection(String market) {
        for (int i = 0; i < MARKET_VALUES.length; i++) {
            if (MARKET_VALUES[i].equals(market)) {
                return i;
            }
        }
        return MARKET_VALUES.length;
    }

    private int currentResolutionSelection(String resolution) {
        for (int i = 0; i < BingSourceOptions.RESOLUTION_VALUES.length; i++) {
            if (BingSourceOptions.RESOLUTION_VALUES[i].equals(resolution)) {
                return i;
            }
        }
        return 0;
    }

    private int currentFitModeSelection(WallpaperFitMode fitMode) {
        for (int i = 0; i < FIT_MODE_VALUES.length; i++) {
            if (FIT_MODE_VALUES[i] == fitMode) {
                return i;
            }
        }
        return 0;
    }

    private String marketLabel(String market) {
        if (BingSourceOptions.MARKET_DEFAULT.equals(market)) {
            return getString(R.string.bing_market_default);
        }
        if (BingSourceOptions.MARKET_CHINA.equals(market)) {
            return getString(R.string.bing_market_china);
        }
        if (BingSourceOptions.MARKET_GLOBAL.equals(market)) {
            return getString(R.string.bing_market_global);
        }
        if (BingSourceOptions.MARKET_US.equals(market)) {
            return getString(R.string.bing_market_us);
        }
        if (BingSourceOptions.MARKET_UK.equals(market)) {
            return getString(R.string.bing_market_uk);
        }
        if (BingSourceOptions.MARKET_JAPAN.equals(market)) {
            return getString(R.string.bing_market_japan);
        }
        return getString(R.string.bing_market_custom_value, market);
    }

    private String resolutionLabel(String resolution) {
        return BingSourceOptions.normalizeResolution(resolution);
    }

    private String fitModeLabel(WallpaperFitMode fitMode) {
        if (fitMode == WallpaperFitMode.CROP) {
            return getString(R.string.wallpaper_fit_mode_crop);
        }
        if (fitMode == WallpaperFitMode.CONTAIN) {
            return getString(R.string.wallpaper_fit_mode_contain);
        }
        if (fitMode == WallpaperFitMode.STRETCH) {
            return getString(R.string.wallpaper_fit_mode_stretch);
        }
        return getString(R.string.wallpaper_fit_mode_system);
    }

    /**
     * 重新设置定时任务
     *
     * @param timedList 定时列表
     */
    private void timed(List<String> timedList) {
        ScheduleManager scheduleManager = new ScheduleManager(this);
        boolean scheduleResult = scheduleManager.rescheduleDaily(ScheduleManager.parseTimedJobs(timedList));
        if (scheduleResult) {
            Log.i(TAG, "定时ok");
        } else {
            Log.e(TAG, "定时不ok");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_about) {
            // 打开关于页面
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
