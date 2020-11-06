package xyz.liut.bingwallpaper;

import android.os.Environment;

import java.io.File;

/**
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
         * 保存壁纸 KEY
         */
        String KEY_SAVE = "KEY_SAVE";

        /**
         * 同时设置锁屏 KEY
         */
        String KEY_LOCK_SCREEN = "KEY_LOCK_SCREEN";

        /**
         * 显示手动 KEY
         */
        String KEY_SHOW_MANUAL_SYNC = "KEY_SHOW_MANUAL_SYNC";

        /**
         * 显示主图标 KEY
         */
        String KEY_SHOW_MAIN = "KEY_SHOW_MAIN";
    }


    interface Config {

        /**
         * 文件保存路径
         */
        String WALLPAPER_SAVE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "wallpaper" + File.separator;
    }

}

