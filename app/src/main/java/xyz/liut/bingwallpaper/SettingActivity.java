package xyz.liut.bingwallpaper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import xyz.liut.bingwallpaper.utils.ComponentUtil;
import xyz.liut.bingwallpaper.utils.ToastUtil;
import xyz.liut.bingwallpaper.utils.WallpaperTool;
import xyz.liut.bingwallpaper.v3.settings.SettingsStore;

/**
 * 主界面 设置页
 */
public class SettingActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";


    private TextView tvTime, tvSave, tvSetLockScreen, tvShowToast, tvShowManual, tvHideMain;

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

        swSave = findViewById(R.id.sw_save);
        swLockScreen = findViewById(R.id.sw_lock_screen);
        swShowToast = findViewById(R.id.sw_show_toast);
        swShowManual = findViewById(R.id.sw_show_manual);
        swHideMain = findViewById(R.id.sw_show_main);

        findViewById(R.id.ll_time).setOnClickListener(this);

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
        } else if (id == R.id.sw_save) {
            // 保存到相册
            reqPermissionAndSave();
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
            SyncWallpaperService.start(this);
            finish();
        } else if (id == R.id.bt_clear) {
            // 清空壁纸
            WallpaperTool.clearWallpaper(this);
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
        if (timedList.size() == 0) {
            tvTime.setText(getString(R.string.no_timed));
        } else {
            tvTime.setText(getString(R.string.every_day) + ": " + TextUtils.join("/", timedList));
        }

        // 配置上屏
        refreshSubText();

        // 设置定时
        timed(timedList);
    }


    private void reqPermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            settingsStore.saveToGallery(swSave.isChecked());
            refreshSubText();
        }
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

    /**
     * 重新设置定时任务
     *
     * @param timedList 定时列表
     */
    private void timed(List<String> timedList) {

        // 清空定时
        JobScheduler scheduler = (JobScheduler) getApplication().getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            scheduler.cancelAll();
        }

        // 重新定时
        for (String timed : timedList) {
            // 时间, 格式: hh:mm
            String[] times = timed.split(":");
            int hour = Integer.parseInt(times[0]);
            int minute = Integer.parseInt(times[1]);

            boolean scheduleResult = AlarmJob.setupTimed(this, hour, minute, 30);
            if (scheduleResult) {
                Log.i(TAG, "定时ok");
            } else {
                Log.e(TAG, "定时不ok");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            settingsStore.saveToGallery(swSave.isChecked());
        } else {
            swSave.setChecked(false);
            ToastUtil.showToast(this, "请授予必要权限");
        }
        refreshSubText();

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
