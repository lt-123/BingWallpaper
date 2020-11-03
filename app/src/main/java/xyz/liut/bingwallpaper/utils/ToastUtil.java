package xyz.liut.bingwallpaper.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

/**
 * Create by liut
 * 2020/11/2
 */
public final class ToastUtil {

    public static void showToast(@NonNull Context context, @NonNull String msg) {
        Log.d("WALLPAPER", msg);
        if (Thread.currentThread().getName().contentEquals("main")) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        } else {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
        }
    }

}
