package xyz.liut.bingwallpaper.engine;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import xyz.liut.bingwallpaper.BaseTestCase;

/**
 * Create by liut on 2020/11/4
 */
public class BingWallpaperEngineTest extends BaseTestCase {

    @Test
    public void setWallpaper() {
        new BingWallpaperEngine("build/wallpaper", BingWallpaperEngine.RESOLUTION_UHD).downLoadWallpaper(new IWallpaperEngine.SimpleDownloadCallback() {
            @Override
            public void onSucceed(File file) {
                System.out.println(file);
                Assert.assertNotEquals(file.length(), 0);
            }

            @Override
            public void onMessage(String msg) {
                System.out.println(msg);
            }

            @Override
            public void onFailed(Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            }
        });
    }

}

