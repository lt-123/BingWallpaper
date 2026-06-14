package xyz.liut.bingwallpaper.v3.wallpaper;

import android.app.WallpaperManager;
import android.content.Context;

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

    /**
     * 创建壁纸设置器。
     *
     * @param context Android 上下文
     */
    public WallpaperSetter(Context context) {
        this.appContext = context.getApplicationContext();
        this.wallpaperManager = WallpaperManager.getInstance(appContext);
    }

    /**
     * 设置壁纸。
     *
     * @param storedWallpaper 已保存壁纸
     * @param lockScreen      是否同时设置锁屏壁纸
     * @throws IOException 打开输入流或设置壁纸失败
     */
    public void set(StoredWallpaper storedWallpaper, boolean lockScreen) throws IOException {
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
}
