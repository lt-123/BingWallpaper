package xyz.liut.bingwallpaper.engine;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;

import xyz.liut.bingwallpaper.http.HttpClient;
import xyz.liut.bingwallpaper.http.Response;

/**
 * 直链
 * <p>
 * Create by liut on 2020/11/4
 */
public class DirectEngine implements IWallpaperEngine {

    private static final String NAME = "DirectEngine";

    private final String url;
    private final String path;
    private final IWallpaperEngine.FileNameFormat fileNameFormat;

    public DirectEngine(@NonNull String url, @NonNull String path, @NonNull FileNameFormat fileNameFormat) {
        this.url = url;
        this.path = path;
        this.fileNameFormat = fileNameFormat;
    }

    @Override
    public String engineName() {
        return NAME;
    }

    @Override
    public long updateInterval() {
        return 0;
    }

    @Override
    public void downLoadWallpaper(@NonNull Callback callback) {
        String fileName = path + File.separator + fileNameFormat.fileName();
        Response<File> resp = HttpClient.getInstance().download(url, fileName, false);

        if (resp.getError() != null) {
            callback.onFailed(resp.getError());
            return;
        }

        Log.e("liut", "getHeaders: " + resp.getHeaders());
        callback.onSucceed(resp.getBody());
    }


}
