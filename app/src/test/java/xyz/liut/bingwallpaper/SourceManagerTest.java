package xyz.liut.bingwallpaper;

import android.util.Log;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import xyz.liut.bingwallpaper.engine.BingWallpaperEngine;
import xyz.liut.bingwallpaper.engine.EngineFactory;
import xyz.liut.bingwallpaper.engine.IWallpaperEngine;

/**
 * Create by liut on 20-11-4
 */
public class SourceManagerTest extends BaseTestCase {

    private static final String TAG = "SourceManagerTest";

    @Test
    public void getSourceList() {
        SourceManager.getSourceList(context).forEach(sourceBean -> {
            if (sourceBean.getName().equals(BingWallpaperEngine.NAME)) return;
            IWallpaperEngine engine = new EngineFactory("build/wallpaper")
                    .getEngineBySourceBean(sourceBean);
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
                public void onFailed(Exception e) {
                    e.printStackTrace();
                    Assert.fail(e.getMessage());
                }
            });


        });

    }
}

