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
public class TimeFileNameFormat implements IWallpaperEngine.FileNameFormat {

    private final SimpleDateFormat format;

    private static final TimeFileNameFormat INSTANCE = new TimeFileNameFormat("wallpaper");

    public static TimeFileNameFormat getInstance() {
        return INSTANCE;
    }

    public TimeFileNameFormat(String template) {
        format = new SimpleDateFormat("'" + template + "-'yyyy-MM-dd'.jpg'", Locale.CHINESE);
    }

    @Override
    public String fileName() {
        return format.format(new Date());
    }

}

