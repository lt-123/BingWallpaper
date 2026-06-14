package xyz.liut.bingwallpaper.v3.source;

import org.junit.Assert;
import org.junit.Test;

/**
 * Bing 壁纸来源测试。
 */
public class BingWallpaperSourceTest {

    private static final String SAMPLE_JSON = "{"
            + "\"images\":[{"
            + "\"urlbase\":\"/th?id=OHR.Test\","
            + "\"title\":\"测试标题\","
            + "\"copyright\":\"测试描述\""
            + "}]}";

    @Test
    public void parseJsonBuildsWallpaperInfoFromUrlbase() throws Exception {
        WallpaperInfo info = BingWallpaperSource.parseJson(SAMPLE_JSON, "UHD");

        Assert.assertEquals("https://www.bing.com/th?id=OHR.Test_UHD.jpg", info.getImageUrl());
        Assert.assertEquals("OHR.Test_UHD.jpg", info.getFileName());
        Assert.assertEquals("测试标题", info.getTitle());
        Assert.assertEquals("测试描述", info.getDescription());
    }

    @Test
    public void fetchUsesInjectedTextFetcher() throws Exception {
        BingWallpaperSource source = new BingWallpaperSource(new BingWallpaperSource.TextFetcher() {
            @Override
            public String get(String url) {
                Assert.assertEquals(BingWallpaperSource.API_URL, url);
                return SAMPLE_JSON;
            }
        }, "UHD");

        WallpaperInfo info = source.fetch();

        Assert.assertEquals("Bing", source.name());
        Assert.assertEquals("https://www.bing.com/th?id=OHR.Test_UHD.jpg", info.getImageUrl());
        Assert.assertEquals("OHR.Test_UHD.jpg", info.getFileName());
    }
}
