package xyz.liut.bingwallpaper.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public Response<String> doRequest(Method method, String url) {
        Response<String> response = new Response<>();
        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            response.setResponseCode(urlConnection.getResponseCode());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                String resp = reader.readLine();
                response.setBody(resp);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (response.getResponseCode() != 0) {
                response.setResponseCode(-1);
            }
            response.setErrorMessage("网络出错");
        }
        return response;
    }

    public Response<File> download(String url, String fileName) {
        Response<File> response = new Response<>();

        File file = new File(fileName);
        File dir = file.getParentFile();

        if (dir == null) {
            response.setResponseCode(-1);
            response.setErrorMessage("创建文件夹失败");
        } else if (dir.isFile()) {
            boolean ret = dir.delete();
            if (!ret) {
                response.setResponseCode(-1);
                response.setErrorMessage("创建文件夹失败");
            }
        } else if (!dir.exists()) {
            boolean ret = dir.mkdirs();
            if (!ret) {
                response.setResponseCode(-1);
                response.setErrorMessage("创建文件夹失败");
            }
        } else {
            HttpURLConnection urlConnection;
            try {
                urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
                urlConnection.setReadTimeout(READ_TIMEOUT);
                response.setResponseCode(urlConnection.getResponseCode());

                int fileLength = urlConnection.getContentLength();

                try (InputStream is = urlConnection.getInputStream(); OutputStream os = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1)
                        os.write(buffer, 0, len);

                    if (file.length() == fileLength)
                        response.setBody(file);
                    else {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                        response.setErrorMessage("文件下载出错");
                        response.setResponseCode(-1);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (response.getResponseCode() != 0) {
                    response.setResponseCode(-1);
                }
                response.setErrorMessage("下载出错");
            }

        }

        return response;
    }


}
