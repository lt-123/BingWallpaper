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

import xyz.liut.bingwallpaper.bean.SourceBean;
import xyz.liut.bingwallpaper.utils.ComponentUtil;
import xyz.liut.bingwallpaper.utils.SpTool;
import xyz.liut.bingwallpaper.utils.ToastUtil;
import xyz.liut.bingwallpaper.utils.WallpaperTool;

/**
 * 主界面 设置页
 */
public class SettingActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";


    private TextView tvSource, tvTime, tvSave, tvSetLockScreen, tvShowToast, tvOnlyWifi, tvShowManual, tvHideMain;

    private Switch swSave, swLockScreen, swShowToast, swOnlyWifi, swShowManual, swHideMain;

    private SpTool spTool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        spTool = SpTool.getDefault(this);

        tvSource = findViewById(R.id.tv_source);
        tvTime = findViewById(R.id.tv_time);
        tvSave = findViewById(R.id.tv_save_path);
        tvSetLockScreen = findViewById(R.id.tv_set_lock_screen);
        tvShowToast = findViewById(R.id.tv_show_toast);
        tvOnlyWifi = findViewById(R.id.tv_only_wifi);
        tvShowManual = findViewById(R.id.tv_show_manual);
        tvHideMain = findViewById(R.id.tv_hide_main);

        swSave = findViewById(R.id.sw_save);
        swLockScreen = findViewById(R.id.sw_lock_screen);
        swShowToast = findViewById(R.id.sw_show_toast);
        swOnlyWifi = findViewById(R.id.sw_only_wifi);
        swShowManual = findViewById(R.id.sw_show_manual);
        swHideMain = findViewById(R.id.sw_show_main);

        findViewById(R.id.ll_source).setOnClickListener(this);
        findViewById(R.id.ll_time).setOnClickListener(this);

        swSave.setOnClickListener(this);
        swLockScreen.setOnClickListener(this);
        swShowManual.setOnClickListener(this);
        swShowToast.setOnClickListener(this);
        swOnlyWifi.setOnClickListener(this);
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
        switch (view.getId()) {
            // 选择源
            case R.id.ll_source:
                startActivity(new Intent(this, SourceListActivity.class));
                break;
            // 设置定时任务
            case R.id.ll_time:
                startActivity(new Intent(this, TimeListActivity.class));
                break;
            // 保存到手机
            case R.id.sw_save:
                reqPermissionAndSave();
                break;
            // 同时设置锁屏
            case R.id.sw_lock_screen:
                savePreference(Constants.Default.KEY_LOCK_SCREEN, swLockScreen.isChecked());
                refreshSubText();
                break;
            case R.id.sw_show_toast:
                savePreference(Constants.Default.KEY_SHOW_TOAST, swShowToast.isChecked());
                refreshSubText();
                break;
            case R.id.sw_only_wifi:
                savePreference(Constants.Default.KEY_ONLY_WIFI, swOnlyWifi.isChecked());
                refreshSubText();
                break;
            // 启用手动
            case R.id.sw_show_manual:
                ComponentUtil.setComponentEnable(this,
                        new ComponentName(this, ManualSetActivity.class),
                        swShowManual.isChecked());
                savePreference(Constants.Default.KEY_SHOW_MANUAL_SYNC, swShowManual.isChecked());
                refreshSubText();
                break;
            // 启用主页
            case R.id.sw_show_main:
                ComponentUtil.setComponentEnable(this,
                        new ComponentName(BuildConfig.APPLICATION_ID, "xyz.liut.bingwallpaper.MainActivity"),
                        !swHideMain.isChecked());
                savePreference(Constants.Default.KEY_HIDE_MAIN, swHideMain.isChecked());
                refreshSubText();
                break;

            // 立即设置
            case R.id.bt_setup_now:
                SyncWallpaperService.start(this);
                finish();
                break;
            // 清空壁纸
            case R.id.bt_clear:
                WallpaperTool.clearWallpaper(this);
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadData() {
        // 壁纸源
        SourceBean bean = SourceManager.getDefaultSource(this);
        tvSource.setText(bean.getName() + "\n" + bean.getDesc());

        // 开关选项
        boolean save = getPreference(Constants.Default.KEY_SAVE, false);
        boolean lockScreen = getPreference(Constants.Default.KEY_LOCK_SCREEN, true);
        boolean showToast = getPreference(Constants.Default.KEY_SHOW_TOAST, true);
        boolean onlyWifi = getPreference(Constants.Default.KEY_ONLY_WIFI, false);
        boolean showManual = getPreference(Constants.Default.KEY_SHOW_MANUAL_SYNC, false);
        boolean hideMain = getPreference(Constants.Default.KEY_HIDE_MAIN, false);
        swSave.setChecked(save);
        swLockScreen.setChecked(lockScreen);
        swShowToast.setChecked(showToast);
        swOnlyWifi.setChecked(onlyWifi);
        swShowManual.setChecked(showManual);
        swHideMain.setChecked(hideMain);

        List<String> timedList = TimedListManager.loadTimedList(this);
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
            savePreference(Constants.Default.KEY_SAVE, swSave.isChecked());
            refreshSubText();
        }
    }

    @SuppressLint("SetTextI18n")
    private void refreshSubText() {
        if (swSave.isChecked()) {
            tvSave.setText("壁纸保存到: \n" + Constants.Config.WALLPAPER_SAVE_PATH);
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

        if (swOnlyWifi.isChecked()) {
            tvOnlyWifi.setText(R.string.only_wifi_desc);
        } else {
            tvOnlyWifi.setText(R.string.only_wifi_desc2);
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
            savePreference(Constants.Default.KEY_SAVE, swSave.isChecked());
        } else {
            swSave.setChecked(false);
            ToastUtil.showToast(this, "请授予必要权限");
        }
        refreshSubText();

    }


    private void savePreference(String key, boolean value) {
        spTool.save(key, value);
    }

    private boolean getPreference(String key, boolean def) {
        return spTool.get(key, def);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting_menu, menu);
        return true;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
