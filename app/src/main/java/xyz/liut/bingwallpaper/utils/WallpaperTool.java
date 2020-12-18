package xyz.liut.bingwallpaper.utils;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 设置壁纸工具
 * <p>
 * Create by liut
 * 2020/11/2
 */
public class WallpaperTool {

    private static final String TAG = "WallpaperTool";

    /**
     * 设置壁纸
     *
     * @param jpgFile 壁纸文件
     */
    public static void setFile2Wallpaper(Context context, File jpgFile, boolean lockScreen) throws Exception {
        final int screenWidth = ScreenUtils.getScreenWidth(context);
        final int screenHeight = ScreenUtils.getScreenHeight(context);
        Log.d(TAG, "screenWidth: " + screenWidth);
        Log.d(TAG, "screenHeight: " + screenHeight);

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(jpgFile.toString(), options);

            int height = options.outHeight;
            int width = options.outWidth;

            Log.d(TAG, "jpg height = " + height);
            Log.d(TAG, "jpg width = " + width);

            final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

            int DesiredMinimumWidth = wallpaperManager.getDesiredMinimumWidth();
            int DesiredMinimumHeight = wallpaperManager.getDesiredMinimumHeight();

            Log.d(TAG, "DesiredMinimumWidth = " + DesiredMinimumWidth);
            Log.d(TAG, "DesiredMinimumHeight = " + DesiredMinimumHeight);

            if (height > width) {
                setWallpaper(wallpaperManager, lockScreen, jpgFile, width, height);
            } else {
                setWallpaper(wallpaperManager, screenWidth, screenHeight, width, height, jpgFile, lockScreen);
            }

        } catch (Exception e) {
            throw new Exception("图片格式错误", e);
        }
    }

    /**
     * 设置壁纸（横向图片）
     * <p>
     * 该函数适用于横向图片， 会把横向图片裁剪， 裁剪结果于屏幕高宽一致
     *
     * @param screenWidth  屏幕宽
     * @param screenHeight 屏幕搞
     * @param width        壁纸文件宽
     * @param height       壁纸文件高
     */
    private static void setWallpaper(WallpaperManager wallpaperManager, int screenWidth, int screenHeight, int width, int height, File jpgFile, boolean lockScreen) throws Exception {

        // 从文件读取图片 按1/整数倍数 缩放
        Bitmap bitmap = AdjustBitmap.decodeSampledBitmapFromFile(jpgFile, screenWidth, screenHeight, width, height);
        Log.d(TAG, "bitmap getHeight: " + bitmap.getHeight());
        Log.d(TAG, "bitmap getWidth: " + bitmap.getWidth());

        // 缩放 按浮点比例
        bitmap = AdjustBitmap.sizeBitmapByHeight(bitmap, screenHeight);
        Log.d(TAG, "bitmap getHeight2: " + bitmap.getHeight());
        Log.d(TAG, "bitmap getWidth2: " + bitmap.getWidth());

        // 裁剪
        bitmap = AdjustBitmap.cropBitmapWidth(bitmap, screenWidth);
        Log.d(TAG, "bitmap getHeight3: " + bitmap.getHeight());
        Log.d(TAG, "bitmap getWidth3: " + bitmap.getWidth());

        // 设置壁纸
        setWallpaper(wallpaperManager, lockScreen, bitmap);

//        wallpaperManager.setWallpaperOffsetSteps(1f, 1f);
//        wallpaperManager.setWallpaperOffsetSteps(0.5f, 0.5f);
//        wallpaperManager.suggestDesiredDimensions(width, height);
//        wallpaperManager.suggestDesiredDimensions(screenWidth, screenHeight);
    }


    /**
     * 设置壁纸
     *
     * @param file   壁纸
     * @param width  壁纸宽
     * @param height 壁纸高
     */
    private static void setWallpaper(WallpaperManager wallpaperManager, boolean lockScreen, File file, int width, int height) throws IOException {
        FileInputStream is = new FileInputStream(file);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && lockScreen) {
            int result = wallpaperManager.setStream(
                    is,
                    new Rect(0, 0, width, height),
                    true,
                    WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM
            );
            Log.d(TAG, "setWallpaper: " + result);
        } else {
            wallpaperManager.setStream(is);
        }
        is.close();
    }

    /**
     * 设置壁纸
     *
     * @param bitmap 壁纸
     */
    private static void setWallpaper(WallpaperManager wallpaperManager, boolean lockScreen, Bitmap bitmap) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && lockScreen) {
            int result = wallpaperManager.setBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), true, WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
            Log.d(TAG, "setWallpaper: " + result);
        } else {
            wallpaperManager.setBitmap(bitmap);
        }
    }


    /**
     * 清空
     */
    @SuppressLint("NewApi")
    public static void clearWallpaper(Context context) {
        try {
            final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            wallpaperManager.clear();
            wallpaperManager.getBuiltInDrawable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
