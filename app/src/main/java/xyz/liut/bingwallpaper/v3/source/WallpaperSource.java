package xyz.liut.bingwallpaper.v3.source;

/**
 * 壁纸来源接口。
 * <p>
 * v3 下载流程通过该接口读取壁纸信息，后续可以在不修改调用方的情况下接入
 * Bing 以外的远程 API、本地收藏、订阅源或其他自定义来源。
 */
public interface WallpaperSource {

    /**
     * 获取当前来源的最新壁纸信息。
     *
     * @return 壁纸信息
     * @throws Exception 获取或解析失败时抛出
     */
    WallpaperInfo fetch() throws Exception;

    /**
     * 获取来源名称，用于日志、展示和后续来源管理。
     */
    String name();
}
