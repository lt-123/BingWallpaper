package xyz.liut.bingwallpaper.engine;

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

    /**
     * bing
     */
    private static final String BING_URL = "https://cn.bing.com";

    /**
     * API URL
     */
    private static final String BING_WALLPAPER_API = "https://www.bing.com/HPImageArchive.aspx?format=js&n=1";


    @Override
    public String engineName() {
        return NAME;
    }

    @Override
    public long updateInterval() {
        return DAY;
    }

    @Override
    public void setWallpaper(String path, FileNameFormat fileNameFormat, Callback callback) {
        try {
            String fileName = path + File.separator + fileNameFormat.fileName();
            // ----------- 获取 URL
            Response<String> response = HttpClient.getInstance().doRequest(Method.get, BING_WALLPAPER_API);
            if (response.isSuccessful()) {
                String resp = response.getBody();
                String jpgUrl = new JSONObject(resp)
                        .getJSONArray("images")
                        .getJSONObject(0)
                        .getString("url")
                        .replace("1920x1080", "1080x1920");

                callback.onMessage("获取URL成功，开始下载");

                // 下载文件
                Response<File> fileResp = HttpClient.getInstance().download(BING_URL + jpgUrl, fileName);
                if (fileResp.isSuccessful()) {
                    callback.onSucceed(fileResp.getBody());
                } else {
                    callback.onFailed("下载壁纸失败");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            callback.onFailed("下载失败：" + e.getMessage());
        }
    }

}
