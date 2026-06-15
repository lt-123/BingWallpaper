package xyz.liut.bingwallpaper.v3.wallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import java.io.IOException;
import java.io.InputStream;

import xyz.liut.bingwallpaper.v3.storage.StoredWallpaper;

/**
 * 壁纸设置器。
 * <p>
 * 负责从 {@link StoredWallpaper} 打开图片输入流，并通过系统 {@link WallpaperManager}
 * 将图片设置为桌面壁纸或同时设置为桌面与锁屏壁纸。
 */
public class WallpaperSetter {

    private final Context appContext;
    private final WallpaperManager wallpaperManager;
    private final WallpaperFitMode fitMode;

    /**
     * 创建壁纸设置器。
     *
     * @param context Android 上下文
     */
    public WallpaperSetter(Context context) {
        this(context, WallpaperFitMode.SYSTEM);
    }

    public WallpaperSetter(Context context, WallpaperFitMode fitMode) {
        this.appContext = context.getApplicationContext();
        this.wallpaperManager = WallpaperManager.getInstance(appContext);
        this.fitMode = fitMode == null ? WallpaperFitMode.SYSTEM : fitMode;
    }

    /**
     * 设置壁纸。
     *
     * @param storedWallpaper 已保存壁纸
     * @param lockScreen      是否同时设置锁屏壁纸
     * @throws IOException 打开输入流或设置壁纸失败
     */
    public void set(StoredWallpaper storedWallpaper, boolean lockScreen) throws IOException {
        if (fitMode != WallpaperFitMode.SYSTEM) {
            setTransformedBitmap(storedWallpaper, lockScreen);
            return;
        }
        try (InputStream inputStream = storedWallpaper.openInputStream(appContext)) {
            if (lockScreen) {
                // 同时设置系统桌面和锁屏壁纸，保持用户开启锁屏选项时两个位置一致。
                wallpaperManager.setStream(inputStream, null, true,
                        WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
            } else {
                // 仅设置系统桌面壁纸，不修改用户当前的锁屏壁纸。
                wallpaperManager.setStream(inputStream);
            }
        }
    }

    private void setTransformedBitmap(StoredWallpaper storedWallpaper, boolean lockScreen) throws IOException {
        Bitmap source;
        try (InputStream inputStream = storedWallpaper.openInputStream(appContext)) {
            source = BitmapFactory.decodeStream(inputStream);
        }
        if (source == null) {
            throw new IOException("Unable to decode wallpaper bitmap");
        }

        DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
        int targetWidth = metrics.widthPixels;
        int targetHeight = metrics.heightPixels;
        Bitmap transformed = WallpaperBitmapTransformer.transform(source, targetWidth, targetHeight, fitMode);
        try {
            if (lockScreen) {
                wallpaperManager.setBitmap(transformed, null, true,
                        WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
            } else {
                wallpaperManager.setBitmap(transformed);
            }
        } finally {
            if (transformed != source) {
                transformed.recycle();
            }
            source.recycle();
        }
    }
}
