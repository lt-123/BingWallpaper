package xyz.liut.bingwallpaper.engine;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import xyz.liut.bingwallpaper.BaseTest;

/**
 * Create by liut on 2020/11/4
 */
public class BingWallpaperEngineTest extends BaseTest {

    @Test
    public void setWallpaper() {
        new BingWallpaperEngine("build", TimeFileNameFormat.getInstance()).downLoadWallpaper(new IWallpaperEngine.Callback() {
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
            public void onFailed(String msg) {
                System.out.println(msg);
                Assert.fail(msg);
            }
        });
    }

}

