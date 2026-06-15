package xyz.liut.bingwallpaper.v3.wallpaper;

import android.graphics.Bitmap;
import android.graphics.Color;

import org.junit.Assert;
import org.junit.Test;

import xyz.liut.bingwallpaper.BaseTestCase;

/**
 * 壁纸图片装配测试。
 */
public class WallpaperBitmapTransformerTest extends BaseTestCase {

    @Test
    public void cropFillsTargetAndCropsCenter() {
        Bitmap source = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888);
        fillColumn(source, 0, Color.RED);
        fillColumn(source, 1, Color.GREEN);
        fillColumn(source, 2, Color.BLUE);
        fillColumn(source, 3, Color.YELLOW);

        Bitmap result = WallpaperBitmapTransformer.transform(source, 2, 2, WallpaperFitMode.CROP);

        Assert.assertEquals(2, result.getWidth());
        Assert.assertEquals(2, result.getHeight());
        Assert.assertEquals(Color.GREEN, result.getPixel(0, 0));
        Assert.assertEquals(Color.BLUE, result.getPixel(1, 0));
    }

    @Test
    public void containKeepsFullImageAndAddsBlackBars() {
        Bitmap source = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888);
        source.eraseColor(Color.RED);

        Bitmap result = WallpaperBitmapTransformer.transform(source, 4, 4, WallpaperFitMode.CONTAIN);

        Assert.assertEquals(4, result.getWidth());
        Assert.assertEquals(4, result.getHeight());
        Assert.assertEquals(Color.BLACK, result.getPixel(0, 0));
        Assert.assertEquals(Color.RED, result.getPixel(0, 1));
        Assert.assertEquals(Color.RED, result.getPixel(1, 2));
        Assert.assertEquals(Color.BLACK, result.getPixel(0, 3));
    }

    @Test
    public void stretchScalesToTargetSize() {
        Bitmap source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        source.eraseColor(Color.RED);

        Bitmap result = WallpaperBitmapTransformer.transform(source, 3, 5, WallpaperFitMode.STRETCH);

        Assert.assertEquals(3, result.getWidth());
        Assert.assertEquals(5, result.getHeight());
        Assert.assertEquals(Color.RED, result.getPixel(2, 4));
    }

    private static void fillColumn(Bitmap bitmap, int x, int color) {
        for (int y = 0; y < bitmap.getHeight(); y++) {
            bitmap.setPixel(x, y, color);
        }
    }
}
