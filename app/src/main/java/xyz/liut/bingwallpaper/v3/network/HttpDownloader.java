package xyz.liut.bingwallpaper.v3.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 基于 {@link HttpURLConnection} 的简单下载器。
 * <p>
 * 负责发起 GET 请求、手动处理重定向、复制响应内容到调用方提供的输出流，
 * 并在下载过程中回调已读取字节数和总字节数。
 */
public class HttpDownloader {

    /**
     * 连接超时时间，单位毫秒。
     */
    public static final int CONNECT_TIMEOUT = 3000;

    /**
     * 读取超时时间，单位毫秒。
     */
    public static final int READ_TIMEOUT = 8000;

    /**
     * 默认 User-Agent，部分图片服务会拒绝没有 UA 的请求。
     */
    public static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36";

    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_REDIRECTS = 5;

    /**
     * 下载进度回调。
     */
    public interface ProgressCallback {
        /**
         * 下载进度变化。
         *
         * @param bytesRead    已读取字节数
         * @param totalBytes   总字节数，未知时为 -1
         */
        void onProgress(long bytesRead, long totalBytes);
    }

    /**
     * 通过 GET 下载二进制内容到指定输出流。
     * <p>
     * 调用方仍然拥有输出流，本方法不会关闭它。
     *
     * @param url      请求地址
     * @param output   接收响应内容的输出流
     * @param callback 进度回调，可为 null
     * @throws IOException 网络错误、重定向异常或非 2xx 响应
     */
    public void download(String url, OutputStream output, ProgressCallback callback) throws IOException {
        HttpURLConnection connection = openConnection(url, 0);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("HTTP request failed: " + responseCode);
            }

            long totalBytes = getContentLength(connection);
            try (InputStream input = connection.getInputStream()) {
                copy(input, output, totalBytes, callback);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 通过 GET 获取文本内容。
     *
     * @param url 请求地址
     * @return UTF-8 文本响应
     * @throws IOException 网络错误、重定向异常或非 2xx 响应
     */
    public String get(String url) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        download(url, output, null);
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private HttpURLConnection openConnection(String url, int redirectCount) throws IOException {
        if (redirectCount > MAX_REDIRECTS) {
            throw new IOException("Too many redirects: " + url);
        }

        URL requestUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = connection.getResponseCode();
        if (responseCode >= 300 && responseCode < 400) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.length() == 0) {
                throw new IOException("Redirect missing Location header: " + responseCode);
            }
            URL redirectUrl = new URL(requestUrl, location);
            return openConnection(redirectUrl.toString(), redirectCount + 1);
        }
        return connection;
    }

    private long getContentLength(HttpURLConnection connection) {
        return connection.getContentLengthLong();
    }

    private void copy(InputStream input, OutputStream output, long totalBytes, ProgressCallback callback)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long bytesRead = 0L;
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            bytesRead += read;
            if (callback != null) {
                callback.onProgress(bytesRead, totalBytes);
            }
        }
    }
}
