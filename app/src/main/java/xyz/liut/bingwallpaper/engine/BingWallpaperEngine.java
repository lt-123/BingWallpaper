package xyz.liut.bingwallpaper.engine;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.File;

import xyz.liut.bingwallpaper.http.HttpClient;
import xyz.liut.bingwallpaper.http.Method;
import xyz.liut.bingwallpaper.http.Response;

/**
 * bing 壁纸引擎
 * <p>
 * Create by liut
 * 2020/10/31
 */
public class BingWallpaperEngine implements IWallpaperEngine {

    public static final String NAME = "BingWallpaperEngine";
    private static final String TAG = NAME;

    /**
     * bing
     */
    public static final String BING_URL = "https://cn.bing.com";

    /**
     * API URL
     */
    private static final String BING_WALLPAPER_API = "https://www.bing.com/HPImageArchive.aspx?format=js&n=1";

    private final String path;
    private final IWallpaperEngine.FileNameFormat fileNameFormat;

    public BingWallpaperEngine(@NonNull String path, @NonNull FileNameFormat fileNameFormat) {
        this.path = path;
        this.fileNameFormat = fileNameFormat;
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
            String jpgUrl = new JSONObject(resp)
                    .getJSONArray("images")
                    .getJSONObject(0)
                    .getString("url")
                    .replace("1920x1080", "1080x1920");

            callback.onMessage("获取URL成功，开始下载");

            String fileName = path + File.separator + fileNameFormat.fileName();
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
