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
    long DAY = HOUR * 24;

    /**
     * @return 名称
     */
    String engineName();

    /**
     * @return 壁纸更新间隔, 单位：毫秒
     */
    long updateInterval();

    /**
     * 下载并设置壁纸
     *
     * @param downloadCallback 下载回调
     */
    void downLoadWallpaper(@NonNull DownloadCallback downloadCallback);

    /**
     * 回调
     */
    interface DownloadCallback {

        /**
         * 完成
         */
        void onSucceed(File file);

        /**
         * 普通消息
         */
        void onMessage(String msg);

        /**
         * 进度消息
         */
        void onProgressMessage(String msg);

        /**
         * 出现异常
         */
        void onFailed(Exception e);

    }

    abstract class SimpleDownloadCallback implements DownloadCallback {
        @Override
        public void onProgressMessage(String msg) {
        }
    }

}
