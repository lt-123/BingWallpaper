package xyz.liut.bingwallpaper.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;

/**
 * 设置壁纸工具
 * <p>
 * Create by liut
 * 2020/11/2
 */
public class WallpaperTool {


    /**
     * 设置壁纸
     *
     * @param jpgFile 壁纸文件
     */
    public static boolean setFile2Wallpaper(Context context, File jpgFile) {
        final int screenWidth = ScreenUtils.getScreenWidth(context);
        final int screenHeight = ScreenUtils.getScreenHeight(context);

        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        wallpaperManager.suggestDesiredDimensions(screenWidth, screenHeight);

        // 4.缩放图片。
        Bitmap bitmap = BitmapFactory.decodeFile(jpgFile.getPath());

        // 5.设为壁纸。
        try {
            wallpaperManager.setBitmap(bitmap);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
