package xyz.liut.bingwallpaper.v3.source;

import org.junit.Assert;
import org.junit.Test;

/**
 * Bing 源配置测试。
 */
public class BingSourceOptionsTest {

    @Test
    public void defaultsUseNoMarketAndGlobalHost() {
        BingSourceOptions options = BingSourceOptions.defaults();

        Assert.assertEquals("", options.getMarket());
        Assert.assertEquals(BingSourceOptions.HOST_GLOBAL, options.getImageHost());
        Assert.assertEquals(BingSourceOptions.RESOLUTION_UHD, options.getResolution());
    }

    @Test
    public void normalizeMarketTrimsValidValue() {
        Assert.assertEquals("en-WW", BingSourceOptions.normalizeMarket(" en-WW "));
    }

    @Test
    public void normalizeMarketRejectsUnsafeValue() {
        Assert.assertEquals("", BingSourceOptions.normalizeMarket("en_WW"));
        Assert.assertFalse(BingSourceOptions.isValidMarket("en_WW"));
    }

    @Test
    public void imageHostUsesChinaHostOnlyForChinaMarket() {
        Assert.assertEquals(BingSourceOptions.HOST_CHINA,
                BingSourceOptions.imageHostForMarket(BingSourceOptions.MARKET_CHINA));
        Assert.assertEquals(BingSourceOptions.HOST_GLOBAL,
                BingSourceOptions.imageHostForMarket(BingSourceOptions.MARKET_GLOBAL));
        Assert.assertEquals(BingSourceOptions.HOST_GLOBAL,
                BingSourceOptions.imageHostForMarket(BingSourceOptions.MARKET_DEFAULT));
        Assert.assertEquals(BingSourceOptions.HOST_GLOBAL,
                BingSourceOptions.imageHostForMarket("en_WW"));
    }

    @Test
    public void constructorDerivesImageHostFromMarket() {
        BingSourceOptions options = new BingSourceOptions(BingSourceOptions.MARKET_CHINA);

        Assert.assertEquals(BingSourceOptions.HOST_CHINA, options.getImageHost());
    }

    @Test
    public void normalizeResolutionOnlyAllowsKnownValues() {
        Assert.assertEquals(BingSourceOptions.RESOLUTION_1920_1080,
                BingSourceOptions.normalizeResolution(BingSourceOptions.RESOLUTION_1920_1080));
        Assert.assertEquals(BingSourceOptions.RESOLUTION_1080_1920,
                BingSourceOptions.normalizeResolution(" 1080x1920 "));
        Assert.assertEquals(BingSourceOptions.RESOLUTION_UHD,
                BingSourceOptions.normalizeResolution("100x100"));
        Assert.assertEquals(BingSourceOptions.RESOLUTION_UHD,
                BingSourceOptions.normalizeResolution(null));
    }

    @Test
    public void constructorNormalizesResolution() {
        BingSourceOptions options = new BingSourceOptions(
                BingSourceOptions.MARKET_GLOBAL,
                BingSourceOptions.RESOLUTION_1366_768);

        Assert.assertEquals(BingSourceOptions.RESOLUTION_1366_768, options.getResolution());
    }
}
