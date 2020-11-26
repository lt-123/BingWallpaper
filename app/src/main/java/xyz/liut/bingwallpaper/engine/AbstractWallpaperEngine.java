package xyz.liut.bingwallpaper.engine;

/**
 * 抽象基类
 * <p>
 * Create by liut on 20-11-16
 */
public abstract class AbstractWallpaperEngine implements IWallpaperEngine {


    @Override
    public long updateInterval() {
        return 0;
    }


    /**
     * @return 创建一个文件名
     */
    protected abstract String createFileName();


}
