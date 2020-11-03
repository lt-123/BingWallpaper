package xyz.liut.bingwallpaper.engine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 文件名创建工具
 * <p>
 * Create by liut
 * 2020/11/2
 */
public class TimeFileFormat implements IWallpaperEngine.FileNameFormat {

    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE);


    @Override
    public String fileName() {
        return format.format(new Date()) + ".jpg";
    }

}

