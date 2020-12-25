package xyz.liut.bingwallpaper.http;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 一个简易的 http 工具
 * <p>
 * Create by liut
 * 2020/11/2
 */
public final class HttpClient {

    private static final String TAG = "HttpClient";

    /**
     * 连接超时时间
     */
    public static final int CONNECT_TIMEOUT = 3 * 1000;
    /**
     * 读取超时时间
     */
    public static final int READ_TIMEOUT = 8 * 1000;

    /**
     * UA, 必须设置个UA, 不然一些url不允许访问
     * Chrome	86.0.4240.183	WebKit 537.36	Linux x86_64
     */
    public static final String UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36";


    public static final HttpClient tools = new HttpClient();

    public static HttpClient getInstance() {
        return tools;
    }

    /**
     * 请求 url
     *
     * @param method 方法
     * @param url    url
     * @return 结果
     */
    public Response<String> doRequest(Method method, String url) {
        Response<String> response = new Response<>();
        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.setRequestProperty("User-Agent", UA);
            switch (method) {
                case get:
                    urlConnection.setDoOutput(false);
                    break;
                case post:
                    urlConnection.setDoOutput(true);
                    break;
            }
            urlConnection.setDoInput(true);

            response.setResponseCode(urlConnection.getResponseCode());
            response.setHeaders(urlConnection.getHeaderFields());
            try (InputStream is = urlConnection.getInputStream();
                 ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1)
                    os.write(buffer, 0, len);
                response.setBody(os.toString());
            }
        } catch (Exception e) {
            response.setError(new HttpException("网络出错", e));
        }
        return response;
    }

    /**
     * 下载文件
     *
     * @param url      url
     * @param fileName 文件名
     */
    public void download(@NonNull String url, @NonNull String fileName, @NonNull FileDownloadCallback callback) {
        Log.d(TAG, "download() called with: url = [" + url + "], fileName = [" + fileName + "]");

        File file = new File(fileName);
        File dir = file.getParentFile();

        if (dir == null) {
            callback.onError(new HttpException("创建文件夹失败"));
            return;
        } else if (dir.isFile()) {
            boolean ret = dir.delete();
            if (!ret) {
                callback.onError(new HttpException("创建文件夹失败"));
                return;
            }
        } else if (!dir.exists()) {
            boolean ret = dir.mkdirs();
            if (!ret) {
                callback.onError(new HttpException("创建文件夹失败"));
                return;
            }
        }

        doIO(url, file, callback);
    }

    /**
     * 网络->磁盘
     *
     * @param url  url
     * @param file 保存到文件
     */
    private void doIO(@NonNull String url, @NonNull File file, @NonNull FileDownloadCallback callback) {
        HttpURLConnection urlConnection;
        try {
            URL u = new URL(url);
            urlConnection = (HttpURLConnection) u.openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.setRequestProperty("User-Agent", UA);
            // 手动处理重定向，为了解决 文件 length 问题
            urlConnection.setInstanceFollowRedirects(false);

            int respCode = urlConnection.getResponseCode();

            long fileLength;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                fileLength = urlConnection.getContentLengthLong();
            } else {
                fileLength = urlConnection.getContentLength();
            }

            callback.onHeader(respCode, fileLength, urlConnection.getHeaderFields());

            // 重定向
            if (respCode > 300 && respCode < 400) {
                String location = urlConnection.getHeaderField("Location");
                if (location == null) throw new NullPointerException("重定向失败");
                URL locationUrl = new URL(u, location);
                Log.d(TAG, "重定向: " + respCode + "  " + locationUrl);
                doIO(locationUrl.toString(), file, callback);
            }
            // 正常
            else if (respCode >= 200 && respCode < 300) {
                // 文件已存在
                if (file.exists() && file.length() == fileLength) {
                    Log.d(TAG, "download: cached fileLength = " + fileLength);
                    callback.onCompleted(file);
                } else {
                    try (InputStream is = urlConnection.getInputStream();
                         OutputStream os = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        long totalBytesRead = 0L;
                        long progressTime = 0;
                        while ((len = is.read(buffer)) != -1) {
                            // 进度回调间隔：1 秒
                            if (System.currentTimeMillis() - progressTime > 1000) {
                                progressTime = System.currentTimeMillis();
                                callback.onProgress(totalBytesRead, fileLength);
                            }

                            os.write(buffer, 0, len);
                            totalBytesRead += len;
                        }
                        callback.onProgress(totalBytesRead, fileLength);

                        if (fileLength != -1 && file.length() != fileLength) {
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                            callback.onError(new HttpException("文件下载不完整"));
                        } else {
                            callback.onCompleted(file);
                            Log.d(TAG, "download: fileLength = " + fileLength);
                        }
                    }
                }

            }
            // 异常
            else {
                callback.onError(new HttpException("网络请求出错 " + respCode));
            }
        } catch (Exception e) {
            callback.onError(new HttpException(e.getMessage(), e));
        }

    }


}
