package xyz.liut.bingwallpaper.engine;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import xyz.liut.bingwallpaper.bean.SourceBean;

/**
 * Engine 工厂
 * <p>
 * Create by liut
 * 2020/11/2
 */
public class EngineFactory {

    private static final String TAG = "EngineFactory";

    private final String path;

    public static EngineFactory getDefault(Context context) {
        return new EngineFactory(context.getExternalCacheDir().toString());
    }

    public EngineFactory(String path) {
        this.path = path;
    }

    /**
     * 根据名称获取引擎
     *
     * @param bean 资源类
     * @return 引擎
     */
    public IWallpaperEngine getEngineBySourceBean(@NonNull SourceBean bean) {
        Log.d(TAG, "getEngineBySourceBean() called with: bean = [" + bean.getName() + "]");
        switch (bean.getName()) {
            case BingWallpaperEngine.NAME:
            case BingWallpaperEngine.NAME_UHD:
                return new BingWallpaperEngine(path, BingWallpaperEngine.RESOLUTION_UHD);
            case BingWallpaperEngine.NAME_1080_1920:
                return new BingWallpaperEngine(path, BingWallpaperEngine.RESOLUTION_1080_1920);
            default:
                return new DirectEngine(bean.getName(), bean.getUrl(), path);
        }
    }


}

