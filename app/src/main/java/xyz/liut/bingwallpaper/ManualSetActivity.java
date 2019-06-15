package xyz.liut.bingwallpaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;

public class ManualSetActivity extends Activity {

    private static final String TAG = "ManualSetActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 如果未忽略则弹窗提醒
            if (!hasIgnoredBatteryOptimization()) {
                new AlertDialog
                        .Builder(this)
                        .setTitle("忽略电池优化")
                        .setMessage("忽略电池优化会使本软件的定时更新功能更加稳定， 并不会增加耗电量， 请在接下来的提示中选择允许。")
                        .setPositiveButton("确定", (dialog, which) -> {
                            ignoreBatteryOptimization();
                            finish();
                        })
                        .show();
            } else finish();
        } else finish();

        startService(new Intent(getApplicationContext(), SyncWallpaperService.class));
    }


    /**
     * 是否已经忽略电池优化
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean hasIgnoredBatteryOptimization() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            boolean hasIgnored = powerManager.isIgnoringBatteryOptimizations(this.getPackageName());
            Log.d(TAG, "ignoreBatteryOptimization() called with hasIgnored = " + hasIgnored);
            return hasIgnored;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * 发起忽略
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void ignoreBatteryOptimization() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
