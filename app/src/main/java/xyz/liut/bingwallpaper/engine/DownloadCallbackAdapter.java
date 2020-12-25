package xyz.liut.bingwallpaper.engine;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.io.File;

import xyz.liut.bingwallpaper.http.FileDownloadCallback;
import xyz.liut.bingwallpaper.http.HttpException;

/**
 * 壁纸下载回调，转成Http下载回调
 * <p>
 * Create by liut on 20-12-25
 */
public class DownloadCallbackAdapter extends FileDownloadCallback.SimpleCallback {

    private final IWallpaperEngine.Callback callback;

    public DownloadCallbackAdapter(@NonNull IWallpaperEngine.Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onProgress(long totalBytesRead, long totalBytes) {
        if (totalBytes == -1) {
            callback.onProgressMessage("下载中 " + plainLength(totalBytesRead) + "k");
        } else {
            callback.onProgressMessage("下载中 " + plainLength(totalBytesRead) + "/" + plainLength(totalBytes) + "k");
        }
    }

    @Override
    public void onCompleted(File file) {
        callback.onSucceed(file);
    }

    @Override
    public void onError(HttpException e) {
        callback.onFailed(e);
    }

    @SuppressLint("DefaultLocale")
    private String plainLength(long totalBytesRead) {
        double val = (double) totalBytesRead / 1024;
        return String.format("%.2f", val);
    }

}
