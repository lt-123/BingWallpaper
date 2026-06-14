package xyz.liut.bingwallpaper;

/**
 * 常量值
 * <p>
 * Create by liut on 20-11-4
 */
public interface Constants {

    interface Default {
        /**
         * 源列表 KEY
         */
        String KEY_SOURCE_LIST = "KEY_SOURCE_LIST";

        /**
         * 默认源 KEY
         */
        String KEY_DEFAULT_SOURCE = "KEY_DEFAULT_SOURCE";

        /**
         * 定时 时间列表
         */
        String KEY_TIMED_LIST = "KEY_TIMED_LIST";

        /**
         * 保存壁纸 KEY
         */
        String KEY_SAVE = "KEY_SAVE";

        /** 保存到系统相册 KEY。 */
        String KEY_SAVE_TO_GALLERY = "KEY_SAVE_TO_GALLERY";

        /**
         * 同时设置锁屏 KEY
         */
        String KEY_LOCK_SCREEN = "KEY_LOCK_SCREEN";

        /**
         * 显示土司 KEY
         */
        String KEY_SHOW_TOAST = "KEY_SHOW_TOAST";

        /**
         * 仅wifi KEY
         */
        String KEY_ONLY_WIFI = "KEY_ONLY_WIFI";

        /**
         * 显示手动 KEY
         */
        String KEY_SHOW_MANUAL_SYNC = "KEY_SHOW_MANUAL_SYNC";

        /**
         * 显示主图标 KEY
         */
        String KEY_HIDE_MAIN = "KEY_SHOW_MAIN";
    }


    interface Config {

        /**
         * 使用 MediaStore 保存到公共图片库时的相对目录。
         */
        String MEDIASTORE_RELATIVE_PATH = "Pictures/BingWallpaper";

        /**
         * 通知通道ID和名称
         */
        String CHANNEL_ONE_ID = "Default";
        String CHANNEL_ONE_NAME = "Default";
    }

}
