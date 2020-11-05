package xyz.liut.bingwallpaper;

import android.util.Log;

import org.junit.Test;

import java.io.File;

import xyz.liut.bingwallpaper.engine.DirectEngine;
import xyz.liut.bingwallpaper.engine.IWallpaperEngine;
import xyz.liut.bingwallpaper.engine.TimeFileNameFormat;

/**
 * Create by liut on 20-11-4
 */
public class SourceManagerTest extends BaseTest {

    private static final String TAG = "SourceManagerTest";

    @Test
    public void getSourceList() {
        SourceManager.getSourceList().forEach(sourceBean -> {
            TimeFileNameFormat fileFormat = new TimeFileNameFormat(sourceBean.getName());
            DirectEngine engine = new DirectEngine(sourceBean.getUrl(), "build", fileFormat);
            engine.downLoadWallpaper(new IWallpaperEngine.Callback() {
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


        });

    }
}

