package xyz.liut.bingwallpaper.v3.permissions;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Android 13+ 通知运行时权限判断。
 */
public final class NotificationPermissionHelper {

    private NotificationPermissionHelper() {
    }

    public static boolean requiresRuntimePermission(int sdkInt) {
        return sdkInt >= Build.VERSION_CODES.TIRAMISU;
    }

    public static boolean hasPermission(Context context) {
        if (!requiresRuntimePermission(Build.VERSION.SDK_INT)) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
