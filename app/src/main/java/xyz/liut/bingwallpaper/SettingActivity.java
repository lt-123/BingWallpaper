package xyz.liut.bingwallpaper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

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


    private TextView tvSource, tvTime, tvSave, tvSetLockScreen, tvShowManual, tvShowMain;

    private Switch swSave, swLockScreen, swShowManual, swShowMain;

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
        tvShowManual = findViewById(R.id.tv_show_manual);
        tvShowMain = findViewById(R.id.tv_show_main);

        swSave = findViewById(R.id.sw_save);
        swLockScreen = findViewById(R.id.sw_lock_screen);
        swShowManual = findViewById(R.id.sw_show_manual);
        swShowMain = findViewById(R.id.sw_show_main);

        findViewById(R.id.ll_source).setOnClickListener(this);
        findViewById(R.id.ll_time).setOnClickListener(this);

        swSave.setOnClickListener(this);
        swLockScreen.setOnClickListener(this);
        swShowManual.setOnClickListener(this);
        swShowMain.setOnClickListener(this);

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
                        swShowMain.isChecked());
                savePreference(Constants.Default.KEY_SHOW_MAIN, swShowMain.isChecked());
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
        boolean showManual = getPreference(Constants.Default.KEY_SHOW_MANUAL_SYNC, false);
        boolean showMain = getPreference(Constants.Default.KEY_SHOW_MAIN, true);
        swSave.setChecked(save);
        swLockScreen.setChecked(lockScreen);
        swShowManual.setChecked(showManual);
        swShowMain.setChecked(showMain);

        refreshSubText();
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
            tvSave.setText("壁纸保存到: " + Constants.Config.WALLPAPER_SAVE_PATH);
        } else {
            tvSave.setText(R.string.no_save);
        }

        if (swLockScreen.isChecked()) {
            tvSetLockScreen.setText("同时设置锁屏壁纸已开启(仅支持7.0及以上版本): \n" + Constants.Config.WALLPAPER_SAVE_PATH);
        } else {
            tvSetLockScreen.setText("同时设置锁屏壁纸已开启");
        }

        if (swShowManual.isChecked()) {
            tvShowManual.setText("显示\"手动同步\"桌面图标");
        } else {
            tvShowManual.setText("不显示\"手动同步\"桌面图标");
        }

        if (swShowMain.isChecked()) {
            tvShowMain.setText("显示本应用桌面图标");
        } else {
            tvShowMain.setText("不显示本应用桌面图标");
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
