package xyz.liut.bingwallpaper.engine;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * 壁纸下载
 * <p>
 * Create by liut
 * 2020/10/31
 */
public interface IWallpaperEngine {

    long HOUR = 1000 * 60 * 60;
    long DAY = 1000 * 60 * 60 * 24;

    String engineName();

    /**
     * @return 壁纸更新间隔, 单位：毫秒
     */
    long updateInterval();


    /**
     * 下载并设置壁纸
     */
    void downLoadWallpaper(@NonNull Callback callback);

    /**
     * 回调
     */
    interface Callback {

        void onSucceed(File file);

        void onMessage(String msg);

        void onFailed(Exception e);

    }

    /**
     * 提供文件名
     */
    interface FileNameFormat {
        String fileName();
    }

}
