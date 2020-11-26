package xyz.liut.bingwallpaper.utils;

import android.content.Context;

/**
 * 屏幕工具
 * <p>
 * Create by liut
 * 2019-06-16 11:22
 */
public class ScreenUtils {

    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

}
