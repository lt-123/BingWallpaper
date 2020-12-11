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
import java.io.InputStream;

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

            Log.d(TAG, "height = " + height);
            Log.d(TAG, "width = " + width);

            final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

            int w = wallpaperManager.getDesiredMinimumWidth();
            int h = wallpaperManager.getDesiredMinimumHeight();

            Log.d(TAG, "w = " + w);
            Log.d(TAG, "h = " + h);


//            wallpaperManager.setWallpaperOffsetSteps(1, 1);
//            wallpaperManager.suggestDesiredDimensions(width, height);

//            // 竖屏壁纸
//            if (height >= width) {
//                setWallpaper(wallpaperManager, lockScreen, jpgFile);
////            wallpaperManager.suggestDesiredDimensions(bitmap.getWidth(), bitmap.getHeight());
//                wallpaperManager.suggestDesiredDimensions(screenWidth, screenHeight);
//            } else {
////            Bitmap wallpaper = cropCenter(bitmap, screenWidth, screenHeight);
//                setWallpaper(wallpaperManager, lockScreen, jpgFile);
////            wallpaperManager.suggestDesiredDimensions(wallpaper.getWidth(), wallpaper.getHeight());
//                wallpaperManager.suggestDesiredDimensions(screenWidth, screenHeight);
//            }

            setWallpaper(wallpaperManager, lockScreen, jpgFile);

//            wallpaperManager.setWallpaperOffsetSteps(1, 1);
//            wallpaperManager.suggestDesiredDimensions(screenWidth, screenHeight);
//            wallpaperManager.suggestDesiredDimensions(width, height);

//            wallpaperManager.forgetLoadedWallpaper();
        } catch (Exception e) {
            throw new Exception("图片格式错误", e);
        }
    }

    private static void setWallpaper(WallpaperManager wallpaperManager, boolean lockScreen, File file) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.toString(), options);

        int height = options.outHeight;
        int width = options.outWidth;

        InputStream is = new FileInputStream(file);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && lockScreen) {
            wallpaperManager.setStream(
                    is,
                    new Rect(0, 0, width, height),
                    true,
                    WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM
            );
        } else {
            wallpaperManager.setStream(is);
        }
        is.close();
    }


    /**
     * 对刚好包含屏幕的图片进行中心裁剪。
     *
     * @param bitmap 宽或高刚好包含屏幕的图片
     * @return 若高的部分多余，裁剪掉上下两边多余部分并返回。
     * 若宽的部分多余，裁减掉左右两边多于部分并返回。
     */
    private static Bitmap cropCenter(Bitmap bitmap, int w2, int h2) {
        int h1 = bitmap.getHeight();
        int w1 = bitmap.getWidth();

        if (w1 > w2) {
            return Bitmap.createBitmap(bitmap, (w1 - w2) / 2, 0, w2, h2);
        } else {
            return Bitmap.createBitmap(bitmap, 0, (h1 - h2) / 2, w2, h2);
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
