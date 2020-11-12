package xyz.liut.bingwallpaper.engine;

import android.util.Log;

import org.junit.Test;

import java.io.File;

import xyz.liut.bingwallpaper.BaseTest;

/**
 * 直链
 * <p>
 * Create by liut on 2020/11/4
 */
public class DirectEngineTest extends BaseTest {

    private static final String TAG = "DirectEngineTest";

    @Test
    public void downLoadWallpaper() {
        new DirectEngine("https://api.ixiaowai.cn/gqapi/gqapi.php", "build", TimeFileNameFormat.getInstance())
                .downLoadWallpaper(new IWallpaperEngine.Callback() {
                    @Override
                    public void onSucceed(File file) {
                        Log.d(TAG, "onSucceed() called with: file = [" + file + "]");
                    }

                    @Override
                    public void onMessage(String msg) {
                        Log.d(TAG, "onMessage() called with: msg = [" + msg + "]");
                    }

                    @Override
                    public void onFailed(String msg) {
                        Log.d(TAG, "onFailed() called with: msg = [" + msg + "]");
                    }
                });
    }

}
