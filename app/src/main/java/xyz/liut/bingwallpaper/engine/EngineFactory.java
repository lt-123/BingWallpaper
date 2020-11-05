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
    private final IWallpaperEngine.FileNameFormat fileNameFormat;

    public static EngineFactory getDefault(Context context) {
        return new EngineFactory(context.getExternalCacheDir().toString(), TimeFileNameFormat.getInstance());
    }

    public EngineFactory(String path, IWallpaperEngine.FileNameFormat fileNameFormat) {
        this.path = path;
        this.fileNameFormat = fileNameFormat;
    }

    /**
     * 根据名称获取引擎
     *
     * @param bean 资源类
     * @return 引擎
     */
    public IWallpaperEngine getEngineBySourceBean(@NonNull SourceBean bean) {
        switch (bean.getName()) {
            case BingWallpaperEngine.NAME:
                return new BingWallpaperEngine(path, fileNameFormat);
            default:
                return new DirectEngine(bean.getUrl(), path, fileNameFormat);
        }
    }


}

