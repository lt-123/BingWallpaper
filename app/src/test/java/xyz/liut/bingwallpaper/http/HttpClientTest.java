package xyz.liut.bingwallpaper.http;

import android.util.Log;

import org.junit.Test;

import java.io.File;

import xyz.liut.bingwallpaper.BaseTestCase;
import xyz.liut.bingwallpaper.engine.CommonDownloadCallback;
import xyz.liut.bingwallpaper.engine.IWallpaperEngine;

/**
 * Create by liut
 * 2020/11/2
 */
public class HttpClientTest extends BaseTestCase {

    public static final String API = "https://www.bing.com/HPImageArchive.aspx?format=js&n=1";
    public static final String URL = "https://cn.bing.com/th?id=OHR.TorngatsMt_ROW2999822673_1920x1080.jpg&rf=LaDigue_1920x1080.jpg";
    public static final String URL_2 = "https://img.xjh.me/random_img.php?return=302";

    @Test
    public void doRequest() {
        Response<String> resp = HttpClient.getInstance().doRequest(Method.get, API);
        System.out.println(resp);
    }

    @Test
    public void download() {
        IWallpaperEngine.DownloadCallback downloadCallback = new IWallpaperEngine.SimpleDownloadCallback() {
            @Override
            public void onSucceed(File file) {
                Log.d(TAG, "onSucceed() called with: file = [" + file + "]");
            }

            @Override
            public void onMessage(String msg) {
                Log.d(TAG, "onMessage() called with: msg = [" + msg + "]");
            }

            @Override
            public void onFailed(Exception e) {
                e.printStackTrace();
            }
        };

        // 直链
        HttpClient.getInstance().download(URL, "build/test.jpg", new CommonDownloadCallback(downloadCallback));

        // 重定向
        HttpClient.getInstance().download(URL_2, "build/test2.jpg", new CommonDownloadCallback(downloadCallback));
    }
}
