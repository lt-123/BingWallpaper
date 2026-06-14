package xyz.liut.bingwallpaper.v3.storage;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * 已保存壁纸的位置描述。
 * <p>
 * 壁纸可能已经写入系统相册的 MediaStore，也可能只是暂存在应用缓存目录。
 */
public class StoredWallpaper {

    /**
     * 壁纸保存位置。
     */
    public enum Location {
        /**
         * 保存在系统相册 MediaStore 中。
         */
        MEDIASTORE,

        /**
         * 保存在应用缓存目录中。
         */
        CACHE
    }

    private final Location location;
    private final Uri uri;
    private final File file;

    private StoredWallpaper(Location location, Uri uri, File file) {
        this.location = location;
        this.uri = uri;
        this.file = file;
    }

    /**
     * 创建 MediaStore 保存结果。
     */
    public static StoredWallpaper mediaStore(Uri uri) {
        return new StoredWallpaper(Location.MEDIASTORE, uri, null);
    }

    /**
     * 创建缓存文件保存结果。
     */
    public static StoredWallpaper cache(File file) {
        return new StoredWallpaper(Location.CACHE, null, file);
    }

    /**
     * 获取保存位置。
     */
    public Location getLocation() {
        return location;
    }

    /**
     * 获取 MediaStore Uri；仅 {@link Location#MEDIASTORE} 有值。
     */
    public Uri getUri() {
        return uri;
    }

    /**
     * 获取缓存文件；仅 {@link Location#CACHE} 有值。
     */
    public File getFile() {
        return file;
    }

    /**
     * 打开壁纸输入流。
     *
     * @param context Android 上下文
     * @return 壁纸内容输入流
     * @throws FileNotFoundException 文件或 Uri 不存在
     */
    public InputStream openInputStream(Context context) throws FileNotFoundException {
        if (location == Location.MEDIASTORE) {
            return context.getContentResolver().openInputStream(uri);
        }
        return new FileInputStream(file);
    }

    /**
     * 删除临时文件。
     * <p>
     * 只有缓存目录中的文件会被删除，MediaStore 中的正式图片不会被删除。
     *
     * @return 删除成功或无需删除时返回 true
     */
    public boolean deleteTemporary() {
        if (location != Location.CACHE || file == null || !file.exists()) {
            return true;
        }
        return file.delete();
    }
}
