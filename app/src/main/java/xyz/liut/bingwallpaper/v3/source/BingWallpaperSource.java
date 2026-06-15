package xyz.liut.bingwallpaper.v3.source;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import android.net.Uri;

import xyz.liut.bingwallpaper.v3.network.HttpDownloader;

/**
 * Bing 壁纸来源。
 * <p>
 * 这是 v3 内置的唯一壁纸来源，负责读取 Bing API 文本并解析成通用的
 * {@link WallpaperInfo}，方便后续下载流程与具体来源解耦。
 */
public class BingWallpaperSource implements WallpaperSource {

    public static final String API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&n=1";
    public static final String DEFAULT_RESOLUTION = "UHD";
    public static final int CONNECT_TIMEOUT_MS = 3000;
    public static final int READ_TIMEOUT_MS = 8000;

    private static final String BING_HOST = "https://www.bing.com";
    private static final String NAME = "Bing";
    private static final String ID_PARAMETER = "id=";

    private final TextFetcher textFetcher;
    private final String resolution;
    private final BingSourceOptions options;

    /**
     * 创建使用默认网络请求和默认分辨率的 Bing 来源。
     */
    public BingWallpaperSource() {
        this(new HttpDownloader(), DEFAULT_RESOLUTION, BingSourceOptions.defaults());
    }

    /**
     * 创建使用共享下载器和指定分辨率的 Bing 来源。
     *
     * @param downloader 共享 HTTP 下载器
     * @param resolution 图片分辨率后缀
     */
    public BingWallpaperSource(HttpDownloader downloader, String resolution) {
        this(downloader, resolution, BingSourceOptions.defaults());
    }

    /**
     * 创建使用共享下载器、指定分辨率和 Bing 源配置的 Bing 来源。
     *
     * @param downloader 共享 HTTP 下载器
     * @param resolution 图片分辨率后缀
     * @param options    Bing 源配置
     */
    public BingWallpaperSource(HttpDownloader downloader, String resolution, BingSourceOptions options) {
        this(new DownloaderTextFetcher(downloader), resolution, options);
    }

    public BingWallpaperSource(HttpDownloader downloader, BingSourceOptions options) {
        this(new DownloaderTextFetcher(downloader), options);
    }

    /**
     * 创建使用指定文本获取器和默认分辨率的 Bing 来源。
     *
     * @param textFetcher 文本获取器
     */
    public BingWallpaperSource(TextFetcher textFetcher) {
        this(textFetcher, DEFAULT_RESOLUTION, BingSourceOptions.defaults());
    }

    /**
     * 创建 Bing 来源。
     *
     * @param textFetcher 文本获取器
     * @param resolution  图片分辨率后缀
     */
    public BingWallpaperSource(TextFetcher textFetcher, String resolution) {
        this(textFetcher, resolution, BingSourceOptions.defaults());
    }

    /**
     * 创建 Bing 来源。
     *
     * @param textFetcher 文本获取器
     * @param resolution  图片分辨率后缀
     * @param options     Bing 源配置
     */
    public BingWallpaperSource(TextFetcher textFetcher, String resolution, BingSourceOptions options) {
        this.textFetcher = textFetcher;
        this.resolution = BingSourceOptions.normalizeResolution(resolution);
        this.options = options == null ? BingSourceOptions.defaults() : options;
    }

    public BingWallpaperSource(TextFetcher textFetcher, BingSourceOptions options) {
        this(textFetcher,
                options == null ? BingSourceOptions.RESOLUTION_UHD : options.getResolution(),
                options);
    }

    @Override
    public WallpaperInfo fetch() throws Exception {
        return parseJson(textFetcher.get(buildApiUrl(options.getMarket())), resolution, options.getImageHost());
    }

    @Override
    public String name() {
        return NAME;
    }

    /**
     * 解析 Bing API JSON。
     *
     * @param json       Bing API 返回的 JSON 文本
     * @param resolution 图片分辨率后缀，例如 UHD
     * @return 壁纸信息
     * @throws Exception JSON 格式不符合预期时抛出
     */
    public static WallpaperInfo parseJson(String json, String resolution) throws Exception {
        return parseJson(json, resolution, BingSourceOptions.HOST_GLOBAL);
    }

    /**
     * 解析 Bing API JSON。
     *
     * @param json       Bing API 返回的 JSON 文本
     * @param resolution 图片分辨率后缀，例如 UHD
     * @param imageHost  图片 URL 使用的 Bing 域名
     * @return 壁纸信息
     * @throws Exception JSON 格式不符合预期时抛出
     */
    public static WallpaperInfo parseJson(String json, String resolution, String imageHost) throws Exception {
        JSONObject image = new JSONObject(json)
                .getJSONArray("images")
                .getJSONObject(0);

        String urlbase = image.getString("urlbase");
        String imageUrl = "https://" + sanitizeImageHost(imageHost)
                + urlbase + "_" + resolution + ".jpg";
        String fileName = fileNameFromUrlbase(urlbase, resolution);
        String title = image.optString("title");
        String description = image.optString("copyright");

        return new WallpaperInfo(imageUrl, fileName, title, description);
    }

    private static String sanitizeImageHost(String imageHost) {
        if (BingSourceOptions.HOST_CHINA.equals(imageHost)) {
            return BingSourceOptions.HOST_CHINA;
        }
        return BingSourceOptions.HOST_GLOBAL;
    }

    static String buildApiUrl(String market) {
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority("www.bing.com")
                .path("HPImageArchive.aspx")
                .appendQueryParameter("format", "js")
                .appendQueryParameter("n", "1");
        String normalizedMarket = BingSourceOptions.normalizeMarket(market);
        if (!normalizedMarket.isEmpty()) {
            builder.appendQueryParameter("mkt", normalizedMarket);
        }
        return builder.build().toString();
    }

    private static String fileNameFromUrlbase(String urlbase, String resolution) {
        int idStart = urlbase.indexOf(ID_PARAMETER);
        String imageId = getString(urlbase, idStart);

        String safeName = imageId.replaceAll("[^A-Za-z0-9._-]+", "_");
        if (safeName.isEmpty()) {
            throw new IllegalArgumentException("Unexpected Bing urlbase with unsafe id: " + urlbase);
        }

        return safeName + "_" + resolution + ".jpg";
    }

    @NonNull
    private static String getString(String urlbase, int idStart) {
        if (idStart < 0) {
            throw new IllegalArgumentException("Unexpected Bing urlbase without id=: " + urlbase);
        }

        int valueStart = idStart + ID_PARAMETER.length();
        int valueEnd = urlbase.indexOf('&', valueStart);
        String imageId = valueEnd >= 0 ? urlbase.substring(valueStart, valueEnd) : urlbase.substring(valueStart);
        if (imageId.isEmpty()) {
            throw new IllegalArgumentException("Unexpected Bing urlbase with empty id: " + urlbase);
        }
        return imageId;
    }

    /**
     * 文本获取依赖，便于单元测试替换真实网络请求。
     */
    public interface TextFetcher {
        /**
         * 获取指定 URL 的文本内容。
         *
         * @param url 请求地址
         * @return 响应文本
         * @throws Exception 请求失败时抛出
         */
        String get(String url) throws Exception;
    }

    static class DownloaderTextFetcher implements TextFetcher {

        private final HttpDownloader downloader;

        DownloaderTextFetcher(HttpDownloader downloader) {
            this.downloader = downloader;
        }

        @Override
        public String get(String url) throws Exception {
            return downloader.get(url);
        }
    }

    interface ConnectionFactory {
        HttpURLConnection open(String url) throws Exception;
    }

    static class UrlConnectionTextFetcher implements TextFetcher {

        private final ConnectionFactory connectionFactory;

        UrlConnectionTextFetcher() {
            this(url -> (HttpURLConnection) new URL(url).openConnection());
        }

        UrlConnectionTextFetcher(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        @Override
        public String get(String url) throws Exception {
            HttpURLConnection connection = connectionFactory.open(url);
            connection.setRequestMethod("GET");
            // 等待时间保持较短，避免网络异常时长期阻塞。
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            try {
                InputStream inputStream = connection.getInputStream();
                return readAll(inputStream);
            } finally {
                connection.disconnect();
            }
        }

        private String readAll(InputStream inputStream) throws Exception {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            return builder.toString();
        }
    }
}
