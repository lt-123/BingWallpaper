package xyz.liut.bingwallpaper.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.liut.bingwallpaper.Constants;

/**
 * Create by liut
 * 2020/11/2
 */
public final class ToastUtil {

    public static void showToast(@NonNull Context context, @Nullable String msg) {
        SpTool tool = SpTool.getDefault(context);
        if (tool.get(Constants.Default.KEY_SHOW_TOAST, true)) {
            if (msg == null) msg = "[null]";
            Log.d("WALLPAPER", msg);
            if (Thread.currentThread().getName().contentEquals("main")) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            } else {
                String finalMsg = msg;
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, finalMsg, Toast.LENGTH_SHORT).show());
            }
        } else {
            Log.v("ToastUtil", "toast: " + msg);
        }

    }

}
