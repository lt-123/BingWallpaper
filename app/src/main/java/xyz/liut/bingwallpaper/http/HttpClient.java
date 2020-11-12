package xyz.liut.bingwallpaper.http;

import android.util.Log;

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
     * @param url             url
     * @param fileName        文件名
     * @param checkFileLength 是否校验文件尺寸 重定向连接请关闭
     */
    public Response<File> download(String url, String fileName, boolean checkFileLength) {
        Log.d(TAG, "download() called with: url = [" + url + "], fileName = [" + fileName + "]");

        Response<File> response = new Response<>();

        File file = new File(fileName);
        File dir = file.getParentFile();

        if (dir == null) {
            response.setError(new HttpException("创建文件夹失败"));
        } else if (dir.isFile()) {
            boolean ret = dir.delete();
            if (!ret) {
                response.setError(new HttpException("创建文件夹失败"));
            }
        } else if (!dir.exists()) {
            boolean ret = dir.mkdirs();
            if (!ret) {
                response.setError(new HttpException("创建文件夹失败"));
            }
        } else {
            HttpURLConnection urlConnection;
            try {
                urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
                urlConnection.setReadTimeout(READ_TIMEOUT);
                response.setResponseCode(urlConnection.getResponseCode());
                response.setHeaders(urlConnection.getHeaderFields());

                int fileLength = urlConnection.getContentLength();

                try (InputStream is = urlConnection.getInputStream();
                     OutputStream os = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1)
                        os.write(buffer, 0, len);

                    if (checkFileLength && file.length() != fileLength) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                        response.setError(new HttpException("文件下载不完整: " + url));
                    } else {
                        response.setBody(file);
                        Log.d(TAG, "download: fileLength = " + fileLength);
                    }
                }
            } catch (Exception e) {
                response.setError(new HttpException("下载出错: " + url, e));
            }

        }

        return response;
    }


}
