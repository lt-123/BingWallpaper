package xyz.liut.bingwallpaper.engine;

import android.content.Context;

import androidx.annotation.NonNull;

import xyz.liut.bingwallpaper.bean.SourceBean;

/**
 * Engine 工厂
 * <p>
 * Create by liut
 * 2020/11/2
 */
public class EngineFactory {

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
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public IWallpaperEngine getEngineBySourceBean(@NonNull SourceBean bean) {
        switch (bean.getName()) {
            case BingWallpaperEngine.NAME:
                return new BingWallpaperEngine(path);
            default:
                return new DirectEngine(bean.getName(), bean.getUrl(), path);
        }
    }


}

