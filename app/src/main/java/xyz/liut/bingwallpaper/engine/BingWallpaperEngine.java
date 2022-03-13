package xyz.liut.bingwallpaper.engine;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private static final String TAG = "BingWallpaper";

    public static final String RESOLUTION_UHD = "UHD";
    public static final String RESOLUTION_1080_1920 = "1080x1920";

    public static final String NAME = TAG;
    public static final String NAME_UHD = TAG + "_" + RESOLUTION_UHD + "裁剪";
    public static final String NAME_1080_1920 = TAG + "_" + RESOLUTION_1080_1920;

    /**
     * bing
     */
    public static final String BING_URL = "https://cn.bing.com";

    /**
     * API URL
     */
    private static final String BING_WALLPAPER_API = "https://www.bing.com/HPImageArchive.aspx?format=js&n=1";

    private final String name;
    private final String path;
    private final String resolutions;

    public BingWallpaperEngine(@NonNull String path, @Nullable String resolutions) {
        Log.i(TAG, "BingWallpaperEngine() called with: path = [" + path + "], resolutions = [" + resolutions + "]");
        this.path = path;
        if (RESOLUTION_UHD.equals(resolutions)) {
            this.resolutions = RESOLUTION_UHD;
            this.name = NAME_UHD;
        } else if (RESOLUTION_1080_1920.equals(resolutions) || TextUtils.isEmpty(resolutions)) {
            this.resolutions = RESOLUTION_1080_1920;
            this.name = NAME_1080_1920;
        } else {
            this.resolutions = resolutions;
            this.name = TAG + "_" + resolutions;
        }
    }

    @Override
    public String engineName() {
        return name;
    }

    @Override
    public long updateInterval() {
        return DAY;
    }

    @Override
    public void downLoadWallpaper(@NonNull DownloadCallback downloadCallback) {
        Log.d(TAG, "setWallpaper() called with: path = [" + path + "]");
        try {
            // ----------- 获取 URL
            Response<String> response = HttpClient.getInstance().doRequest(Method.get, BING_WALLPAPER_API);
            if (response.getError() != null) {
                downloadCallback.onFailed(response.getError());
                return;
            }

            String resp = response.getBody();
            Log.d(TAG, "resp = " + resp);

            JSONObject jo = new JSONObject(resp)
                    .getJSONArray("images")
                    .getJSONObject(0);

            String copyright = jo.getString("copyright");
            downloadCallback.onMessage(copyright);

            String urlbase = jo.getString("urlbase");
            String jpgUrl = BING_URL + urlbase + "_" + resolutions + ".jpg";

            String fileName = path + File.separator + urlbase.split("=")[1] + "_" + resolutions + ".jpg";

            HttpClient.getInstance().download(jpgUrl, fileName, new CommonDownloadCallback(downloadCallback));
        } catch (Exception e) {
            downloadCallback.onFailed(e);
        }
    }

}
