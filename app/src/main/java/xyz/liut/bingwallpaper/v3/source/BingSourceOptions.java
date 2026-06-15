package xyz.liut.bingwallpaper.v3.source;

import java.util.regex.Pattern;

/**
 * Bing 壁纸源配置。
 * <p>
 * 负责保存和规范化 Bing API 地区参数，并根据地区推导图片域名。
 */
public final class BingSourceOptions {

    public static final String HOST_GLOBAL = "www.bing.com";
    public static final String HOST_CHINA = "cn.bing.com";

    public static final String RESOLUTION_UHD = "UHD";
    public static final String RESOLUTION_1920_1080 = "1920x1080";
    public static final String RESOLUTION_1920_1200 = "1920x1200";
    public static final String RESOLUTION_1366_768 = "1366x768";
    public static final String RESOLUTION_1080_1920 = "1080x1920";
    public static final String RESOLUTION_768_1366 = "768x1366";
    public static final String[] RESOLUTION_VALUES = {
            RESOLUTION_UHD,
            RESOLUTION_1920_1080,
            RESOLUTION_1920_1200,
            RESOLUTION_1366_768,
            RESOLUTION_1080_1920,
            RESOLUTION_768_1366
    };

    public static final String MARKET_DEFAULT = "";
    public static final String MARKET_CHINA = "zh-CN";
    public static final String MARKET_GLOBAL = "en-WW";
    public static final String MARKET_US = "en-US";
    public static final String MARKET_UK = "en-GB";
    public static final String MARKET_JAPAN = "ja-JP";

    private static final int MIN_MARKET_LENGTH = 2;
    private static final int MAX_MARKET_LENGTH = 16;
    private static final Pattern MARKET_PATTERN = Pattern.compile("[A-Za-z0-9-]+");

    private final String market;
    private final String resolution;

    public BingSourceOptions(String market) {
        this(market, RESOLUTION_UHD);
    }

    public BingSourceOptions(String market, String resolution) {
        this.market = normalizeMarket(market);
        this.resolution = normalizeResolution(resolution);
    }

    public String getMarket() {
        return market;
    }

    public String getImageHost() {
        return imageHostForMarket(market);
    }

    public String getResolution() {
        return resolution;
    }

    public static BingSourceOptions defaults() {
        return new BingSourceOptions(MARKET_DEFAULT);
    }

    public static boolean isValidMarket(String market) {
        if (market == null) {
            return true;
        }
        String value = market.trim();
        if (value.isEmpty()) {
            return true;
        }
        return value.length() >= MIN_MARKET_LENGTH
                && value.length() <= MAX_MARKET_LENGTH
                && MARKET_PATTERN.matcher(value).matches();
    }

    public static String normalizeMarket(String market) {
        if (market == null) {
            return MARKET_DEFAULT;
        }
        String value = market.trim();
        if (!isValidMarket(value)) {
            return MARKET_DEFAULT;
        }
        return value;
    }

    public static String imageHostForMarket(String market) {
        if (MARKET_CHINA.equals(normalizeMarket(market))) {
            return HOST_CHINA;
        }
        return HOST_GLOBAL;
    }

    public static String normalizeResolution(String resolution) {
        if (resolution == null) {
            return RESOLUTION_UHD;
        }
        String value = resolution.trim();
        for (String knownValue : RESOLUTION_VALUES) {
            if (knownValue.equals(value)) {
                return value;
            }
        }
        return RESOLUTION_UHD;
    }
}
