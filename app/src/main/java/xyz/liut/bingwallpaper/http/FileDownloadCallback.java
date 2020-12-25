package xyz.liut.bingwallpaper.http;

import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 下载回调
 */
public interface FileDownloadCallback {

    /**
     * header，如果遇到重定向， 会回调多次
     *
     * @param headers    响应头
     * @param respCode   响应码
     * @param totalBytes 文件大小
     */
    void onHeader(int respCode, long totalBytes, Map<String, List<String>> headers);

    /**
     * 进度
     *
     * @param totalBytesRead 已下载大小
     * @param totalBytes     总大小
     */
    void onProgress(long totalBytesRead, long totalBytes);

    /**
     * 下载完成
     */
    void onCompleted(File file);

    /**
     * 下载出错
     *
     * @param e -
     */
    void onError(HttpException e);

    abstract class SimpleCallback implements FileDownloadCallback {

        private static final String TAG = "FileDownloadCallback";

        @Override
        public void onHeader(int respCode, long totalBytes, Map<String, List<String>> headers) {
            Log.d(TAG, "respCode = [" + respCode + "], totalBytes = [" + totalBytes + "], headers = [" + headers + "]");
        }
    }

}
