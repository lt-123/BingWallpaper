package xyz.liut.bingwallpaper.v3.source;

/**
 * 壁纸信息模型。
 * <p>
 * 用于描述一个壁纸来源返回的图片地址、建议保存文件名以及展示文案。
 */
public class WallpaperInfo {

    private final String imageUrl;
    private final String fileName;
    private final String title;
    private final String description;

    /**
     * 创建壁纸信息。
     *
     * @param imageUrl    图片下载地址
     * @param fileName    建议保存文件名
     * @param title       壁纸标题
     * @param description 壁纸描述
     */
    public WallpaperInfo(String imageUrl, String fileName, String title, String description) {
        this.imageUrl = imageUrl;
        this.fileName = fileName;
        this.title = title;
        this.description = description;
    }

    /**
     * 获取图片下载地址。
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * 获取建议保存文件名。
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取壁纸标题。
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取壁纸描述。
     */
    public String getDescription() {
        return description;
    }
}
