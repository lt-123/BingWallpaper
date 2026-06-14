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
    public static final int CONNECT_TIMEOUT_MS = 3000;
    public static final int READ_TIMEOUT_MS = 8000;

    private static final String BING_HOST = "https://www.bing.com";
    private static final String NAME = "Bing";
    private static final String ID_PARAMETER = "id=";

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
        int idStart = urlbase.indexOf(ID_PARAMETER);
        if (idStart < 0) {
            throw new IllegalArgumentException("Unexpected Bing urlbase without id=: " + urlbase);
        }

        int valueStart = idStart + ID_PARAMETER.length();
        int valueEnd = urlbase.indexOf('&', valueStart);
        String imageId = valueEnd >= 0 ? urlbase.substring(valueStart, valueEnd) : urlbase.substring(valueStart);
        if (imageId.length() == 0) {
            throw new IllegalArgumentException("Unexpected Bing urlbase with empty id: " + urlbase);
        }

        String safeName = imageId.replaceAll("[^A-Za-z0-9._-]+", "_");
        if (safeName.length() == 0) {
            throw new IllegalArgumentException("Unexpected Bing urlbase with unsafe id: " + urlbase);
        }

        return safeName + "_" + resolution + ".jpg";
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

    interface ConnectionFactory {
        HttpURLConnection open(String url) throws Exception;
    }

    static class UrlConnectionTextFetcher implements TextFetcher {

        private final ConnectionFactory connectionFactory;

        UrlConnectionTextFetcher() {
            this(new ConnectionFactory() {
                @Override
                public HttpURLConnection open(String url) throws Exception {
                    return (HttpURLConnection) new URL(url).openConnection();
                }
            });
        }

        UrlConnectionTextFetcher(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        @Override
        public String get(String url) throws Exception {
            HttpURLConnection connection = connectionFactory.open(url);
            connection.setRequestMethod("GET");
            // 与旧版 HttpClient 的等待时间保持接近，避免网络异常时长期阻塞。
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
