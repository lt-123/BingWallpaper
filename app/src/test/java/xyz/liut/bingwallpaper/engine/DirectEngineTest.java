package xyz.liut.bingwallpaper.engine;

import android.util.Log;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import xyz.liut.bingwallpaper.BaseTestCase;

/**
 * 直链
 * <p>
 * Create by liut on 2020/11/4
 */
public class DirectEngineTest extends BaseTestCase {

    private static final String TAG = "DirectEngineTest";

    @Test
    public void downLoadWallpaper() {
        new DirectEngine("DirectEngine", "https://api.ixiaowai.cn/gqapi/gqapi.php", "build/wallpaper")
                .downLoadWallpaper(new IWallpaperEngine.SimpleDownloadCallback() {
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
                        Assert.fail(e.getMessage());
                    }
                });
    }

}

