package xyz.liut.bingwallpaper.engine;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import xyz.liut.bingwallpaper.http.HttpClient;
import xyz.liut.bingwallpaper.http.Response;

/**
 * 直链
 * <p>
 * Create by liut on 2020/11/4
 */
public class DirectEngine extends AbstractWallpaperEngine {

    private final SimpleDateFormat format;

    private final String name;
    private final String url;
    private final String path;

    public DirectEngine(@NonNull String name, @NonNull String url, @NonNull String path) {
        this.name = name;
        this.url = url;
        this.path = path;

        format = new SimpleDateFormat("yyyy-MM-dd'.jpg'", Locale.CHINESE);
    }

    @Override
    public String engineName() {
        return name;
    }

    @Override
    public long updateInterval() {
        return 0;
    }

    @Override
    protected String createFileName() {
        return engineName() + File.separator + format.format(new Date());
    }

    @Override
    public void downLoadWallpaper(@NonNull Callback callback) {
        String fileName = path + File.separator + createFileName();
        Response<File> resp = HttpClient.getInstance().download(url, fileName, false);

        if (resp.getError() != null) {
            callback.onFailed(resp.getError());
            return;
        }

        Log.e("liut", "getHeaders: " + resp.getHeaders());
        callback.onSucceed(resp.getBody());
    }


}
