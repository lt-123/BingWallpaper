package xyz.liut.bingwallpaper.engine;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import xyz.liut.bingwallpaper.http.HttpClient;
import xyz.liut.bingwallpaper.http.Method;
import xyz.liut.bingwallpaper.http.Response;

/**
 * bing 壁纸引擎
 * <p>
 * Create by liut
 * 2020/10/31
 */
public class BingWallpaperEngine extends AbstractWallpaperEngine {

    public static final String NAME = "BingWallpaper";
    private static final String TAG = NAME;

    /**
     * bing
     */
    public static final String BING_URL = "https://cn.bing.com";

    /**
     * API URL
     */
    private static final String BING_WALLPAPER_API = "https://www.bing.com/HPImageArchive.aspx?format=js&n=1";

    private final SimpleDateFormat format;

    private final String path;

    public BingWallpaperEngine(@NonNull String path) {
        this.path = path;
        format = new SimpleDateFormat("yyyy-MM-dd'.jpg'", Locale.CHINESE);
    }

    @Override
    public String engineName() {
        return NAME;
    }

    @Override
    public long updateInterval() {
        return DAY;
    }

    @Override
    protected String createFileName() {
        return engineName() + File.separator + format.format(new Date());
    }

    @Override
    public void downLoadWallpaper(@NonNull Callback callback) {
        Log.d(TAG, "setWallpaper() called with: path = [" + path + "]");
        try {
            // ----------- 获取 URL
            Response<String> response = HttpClient.getInstance().doRequest(Method.get, BING_WALLPAPER_API);
            if (response.getError() != null) {
                callback.onFailed(response.getError());
                return;
            }

            String resp = response.getBody();
            Log.d(TAG, "resp = " + resp);
            String jpgUrl = new JSONObject(resp)
                    .getJSONArray("images")
                    .getJSONObject(0)
                    .getString("url")
                    .replace("1920x1080", "1080x1920");

            callback.onMessage("获取URL成功，开始下载");

            String fileName = path + File.separator + createFileName();
            Response<File> fileResponse = HttpClient.getInstance().download(BING_URL + jpgUrl, fileName, true);

            if (fileResponse.getError() != null) {
                callback.onFailed(fileResponse.getError());
                return;
            }

            Log.d("liut", "getHeaders: " + fileResponse.getHeaders());
            callback.onSucceed(fileResponse.getBody());

        } catch (Exception e) {
            callback.onFailed(e);
        }
    }

}
