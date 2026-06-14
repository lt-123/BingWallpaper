package xyz.liut.bingwallpaper.v3.source;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Bing 壁纸来源。
 * <p>
 * 这是 v3 内置的唯一壁纸来源，负责读取 Bing API 文本并解析成通用的
 * {@link WallpaperInfo}，方便后续下载流程与具体来源解耦。
 */
public class BingWallpaperSource implements WallpaperSource {

    public static final String API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&n=1";
    public static final String DEFAULT_RESOLUTION = "UHD";

    private static final String BING_HOST = "https://www.bing.com";
    private static final String NAME = "Bing";

    private final TextFetcher textFetcher;
    private final String resolution;

    /**
     * 创建使用默认网络请求和默认分辨率的 Bing 来源。
     */
    public BingWallpaperSource() {
        this(new UrlConnectionTextFetcher(), DEFAULT_RESOLUTION);
    }

    /**
     * 创建使用指定文本获取器和默认分辨率的 Bing 来源。
     *
     * @param textFetcher 文本获取器
     */
    public BingWallpaperSource(TextFetcher textFetcher) {
        this(textFetcher, DEFAULT_RESOLUTION);
    }

    /**
     * 创建 Bing 来源。
     *
     * @param textFetcher 文本获取器
     * @param resolution  图片分辨率后缀
     */
    public BingWallpaperSource(TextFetcher textFetcher, String resolution) {
        this.textFetcher = textFetcher;
        this.resolution = resolution;
    }

    @Override
    public WallpaperInfo fetch() throws Exception {
        return parseJson(textFetcher.get(API_URL), resolution);
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
        JSONObject image = new JSONObject(json)
                .getJSONArray("images")
                .getJSONObject(0);

        String urlbase = image.getString("urlbase");
        String imageUrl = BING_HOST + urlbase + "_" + resolution + ".jpg";
        String fileName = fileNameFromUrlbase(urlbase, resolution);
        String title = image.optString("title");
        String description = image.optString("copyright");

        return new WallpaperInfo(imageUrl, fileName, title, description);
    }

    private static String fileNameFromUrlbase(String urlbase, String resolution) {
        int index = urlbase.lastIndexOf('=');
        String baseName = index >= 0 ? urlbase.substring(index + 1) : urlbase;
        return baseName + "_" + resolution + ".jpg";
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

    private static class UrlConnectionTextFetcher implements TextFetcher {

        @Override
        public String get(String url) throws Exception {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            try {
                InputStream inputStream = connection.getInputStream();
                return readAll(inputStream);
            } finally {
                connection.disconnect();
            }
        }

        private String readAll(InputStream inputStream) throws Exception {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
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
