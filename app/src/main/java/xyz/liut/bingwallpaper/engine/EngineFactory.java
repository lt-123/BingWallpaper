package xyz.liut.bingwallpaper.engine;

import android.support.annotation.NonNull;

/**
 * Engine 工厂
 * <p>
 * Create by liut
 * 2020/11/2
 */
public class EngineFactory {

    /**
     * 根据名称获取引擎
     *
     * @param name 名称
     * @return 引擎
     */
    public static IWallpaperEngine getEngine(@NonNull String name) {
        switch (name) {
            case BingWallpaperEngine.NAME:
                return new BingWallpaperEngine();
            default:
                return null;

        }
    }

}

