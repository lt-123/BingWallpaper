package xyz.liut.bingwallpaper.v3.source;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import xyz.liut.bingwallpaper.BaseTestCase;
import xyz.liut.bingwallpaper.v3.network.HttpDownloader;

/**
 * Bing 壁纸来源测试。
 */
public class BingWallpaperSourceTest extends BaseTestCase {

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
    public void parseJsonUsesConfiguredImageHost() throws Exception {
        WallpaperInfo info = BingWallpaperSource.parseJson(SAMPLE_JSON, "UHD", BingSourceOptions.HOST_CHINA);

        Assert.assertEquals("https://cn.bing.com/th?id=OHR.Test_UHD.jpg", info.getImageUrl());
        Assert.assertEquals("OHR.Test_UHD.jpg", info.getFileName());
    }

    @Test
    public void buildApiUrlOmitsEmptyMarket() {
        String apiUrl = BingWallpaperSource.buildApiUrl("");

        Assert.assertEquals("https://www.bing.com/HPImageArchive.aspx?format=js&n=1", apiUrl);
    }

    @Test
    public void buildApiUrlAddsConfiguredMarket() {
        String apiUrl = BingWallpaperSource.buildApiUrl("en-WW");

        Assert.assertEquals("https://www.bing.com/HPImageArchive.aspx?format=js&n=1&mkt=en-WW", apiUrl);
    }

    @Test
    public void parseJsonRejectsUrlbaseWithoutIdParameter() throws Exception {
        try {
            BingWallpaperSource.parseJson(jsonWithUrlbase("/th?bad=OHR.Test"), "UHD");
            Assert.fail("缺少 id= 的 urlbase 应该被拒绝");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("urlbase"));
        }
    }

    @Test
    public void parseJsonSanitizesUnsafeFileNameCharacters() throws Exception {
        WallpaperInfo info = BingWallpaperSource.parseJson(jsonWithUrlbase("/th?id=OHR.Test/Unsafe?Name"), "UHD");

        Assert.assertEquals("OHR.Test_Unsafe_Name_UHD.jpg", info.getFileName());
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

    @Test
    public void fetchCanUseSharedHttpDownloaderForApiText() throws Exception {
        FakeHttpDownloader downloader = new FakeHttpDownloader(SAMPLE_JSON);
        BingWallpaperSource source = new BingWallpaperSource(downloader, "UHD",
                new BingSourceOptions(BingSourceOptions.MARKET_GLOBAL));

        WallpaperInfo info = source.fetch();

        Assert.assertEquals("https://www.bing.com/HPImageArchive.aspx?format=js&n=1&mkt=en-WW",
                downloader.requestedUrl);
        Assert.assertEquals("https://www.bing.com/th?id=OHR.Test_UHD.jpg", info.getImageUrl());
        Assert.assertEquals("OHR.Test_UHD.jpg", info.getFileName());
    }

    @Test
    public void fetchUsesChinaImageHostForChinaMarket() throws Exception {
        FakeHttpDownloader downloader = new FakeHttpDownloader(SAMPLE_JSON);
        BingWallpaperSource source = new BingWallpaperSource(downloader, "UHD",
                new BingSourceOptions(BingSourceOptions.MARKET_CHINA));

        WallpaperInfo info = source.fetch();

        Assert.assertEquals("https://www.bing.com/HPImageArchive.aspx?format=js&n=1&mkt=zh-CN",
                downloader.requestedUrl);
        Assert.assertEquals("https://cn.bing.com/th?id=OHR.Test_UHD.jpg", info.getImageUrl());
        Assert.assertEquals("OHR.Test_UHD.jpg", info.getFileName());
    }

    @Test
    public void fetchUsesConfiguredResolutionFromOptions() throws Exception {
        FakeHttpDownloader downloader = new FakeHttpDownloader(SAMPLE_JSON);
        BingWallpaperSource source = new BingWallpaperSource(downloader,
                new BingSourceOptions(BingSourceOptions.MARKET_GLOBAL,
                        BingSourceOptions.RESOLUTION_1920_1080));

        WallpaperInfo info = source.fetch();

        Assert.assertEquals("https://www.bing.com/HPImageArchive.aspx?format=js&n=1&mkt=en-WW",
                downloader.requestedUrl);
        Assert.assertEquals("https://www.bing.com/th?id=OHR.Test_1920x1080.jpg", info.getImageUrl());
        Assert.assertEquals("OHR.Test_1920x1080.jpg", info.getFileName());
    }

    @Test
    public void urlConnectionTextFetcherConfiguresTimeouts() throws Exception {
        final FakeHttpURLConnection connection = new FakeHttpURLConnection(new URL("https://example.com"));
        BingWallpaperSource.UrlConnectionTextFetcher fetcher =
                new BingWallpaperSource.UrlConnectionTextFetcher(new BingWallpaperSource.ConnectionFactory() {
                    @Override
                    public HttpURLConnection open(String url) {
                        Assert.assertEquals(BingWallpaperSource.API_URL, url);
                        return connection;
                    }
                });

        Assert.assertEquals("{}", fetcher.get(BingWallpaperSource.API_URL));
        Assert.assertEquals(BingWallpaperSource.CONNECT_TIMEOUT_MS, connection.connectTimeout);
        Assert.assertEquals(BingWallpaperSource.READ_TIMEOUT_MS, connection.readTimeout);
        Assert.assertTrue(connection.disconnected);
    }

    private static String jsonWithUrlbase(String urlbase) {
        return "{"
                + "\"images\":[{"
                + "\"urlbase\":\"" + urlbase + "\","
                + "\"title\":\"测试标题\","
                + "\"copyright\":\"测试描述\""
                + "}]}";
    }

    private static class FakeHttpURLConnection extends HttpURLConnection {

        private int connectTimeout;
        private int readTimeout;
        private boolean disconnected;

        protected FakeHttpURLConnection(URL url) {
            super(url);
        }

        @Override
        public void disconnect() {
            disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }

        @Override
        public void setConnectTimeout(int timeout) {
            connectTimeout = timeout;
        }

        @Override
        public void setReadTimeout(int timeout) {
            readTimeout = timeout;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream("{}".getBytes());
        }
    }

    private static class FakeHttpDownloader extends HttpDownloader {
        private final String response;
        private String requestedUrl;

        private FakeHttpDownloader(String response) {
            this.response = response;
        }

        @Override
        public String get(String url) {
            requestedUrl = url;
            return response;
        }
    }
}
