package xyz.liut.bingwallpaper.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * 组件工具
 * <p>
 * Create by liut on 20-11-6
 */
public class ComponentUtil {


    /**
     * 启用或禁用组件
     *
     * @param componentName 组件
     * @param enable        enable
     */
    public static void setComponentEnable(Context context, ComponentName componentName, boolean enable) {
        PackageManager packageManager = context.getPackageManager();

        // 启用
        if (enable) {
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }

        // 停用
        else {
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        }

    }


}
