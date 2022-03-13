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
public class CommonDownloadCallback extends FileDownloadCallback.SimpleCallback {

    private final IWallpaperEngine.DownloadCallback downloadCallback;

    public CommonDownloadCallback(@NonNull IWallpaperEngine.DownloadCallback downloadCallback) {
        this.downloadCallback = downloadCallback;
    }

    @Override
    public void onProgress(long totalBytesRead, long totalBytes) {
        if (totalBytes == -1) {
            downloadCallback.onProgressMessage("下载中 " + plainLength(totalBytesRead) + "k");
        } else {
            downloadCallback.onProgressMessage("下载中 " + plainLength(totalBytesRead) + "/" + plainLength(totalBytes) + "k");
        }
    }

    @Override
    public void onCompleted(File file) {
        downloadCallback.onSucceed(file);
    }

    @Override
    public void onError(HttpException e) {
        downloadCallback.onFailed(e);
    }

    @SuppressLint("DefaultLocale")
    private String plainLength(long totalBytesRead) {
        double val = (double) totalBytesRead / 1024;
        return String.format("%.2f", val);
    }

}
