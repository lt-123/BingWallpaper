package xyz.liut.bingwallpaper.utils;

import static android.content.Context.POWER_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Create by liut on 20-11-11
 */
public class BatteryUtil {

    private static final String TAG = "BatteryUtil";

    /**
     * 是否已经忽略电池优化
     */
    public static boolean hasIgnoredBatteryOptimization(Context context) {
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
            boolean hasIgnored = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
            Log.d(TAG, "ignoreBatteryOptimization() called with hasIgnored = " + hasIgnored);
            return hasIgnored;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 发起忽略
     */
    public static void ignoreBatteryOptimization(Context context) {
        try {
            @SuppressLint("BatteryLife")
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

}
