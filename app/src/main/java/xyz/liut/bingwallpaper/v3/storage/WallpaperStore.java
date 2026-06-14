package xyz.liut.bingwallpaper.v3.storage;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import xyz.liut.bingwallpaper.Constants;
import xyz.liut.bingwallpaper.v3.source.WallpaperInfo;

/**
 * 壁纸保存器。
 * <p>
 * 根据用户设置将壁纸写入系统相册或应用缓存目录，实际图片字节由调用方通过
 * {@link OutputWriter} 写入，便于复用网络下载、测试数据或其他图片来源。
 */
public class WallpaperStore {

    private static final String CACHE_DIR_NAME = "wallpapers";
    private static final String MIME_TYPE_JPEG = "image/jpeg";

    private final Context appContext;

    /**
     * 创建壁纸保存器。
     * <p>
     * 保存应用级 Context，避免调用方在每次保存时重复传入 Context，也避免持有 Activity。
     *
     * @param context Android 上下文
     */
    public WallpaperStore(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * 输出流写入回调。
     */
    public interface OutputWriter {
        /**
         * 向输出流写入壁纸内容。
         *
         * @param outputStream 目标输出流
         * @throws IOException 写入失败
         */
        void write(OutputStream outputStream) throws IOException;
    }

    /**
     * 保存壁纸。
     *
     * @param wallpaperInfo 壁纸信息
     * @param saveToGallery 是否保存到系统相册
     * @param writer        图片内容写入回调
     * @return 保存后的壁纸位置
     * @throws IOException 创建目标、打开输出流或写入失败
     */
    public StoredWallpaper save(WallpaperInfo wallpaperInfo, boolean saveToGallery, OutputWriter writer)
            throws IOException {
        if (saveToGallery) {
            return saveToMediaStore(wallpaperInfo, writer);
        }
        return saveToCache(wallpaperInfo, writer);
    }

    private StoredWallpaper saveToMediaStore(WallpaperInfo wallpaperInfo, OutputWriter writer)
            throws IOException {
        ContentResolver resolver = appContext.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, wallpaperInfo.getFileName());
        values.put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_JPEG);
        // RELATIVE_PATH 指定公共图片库中的相对目录，由系统负责映射到实际存储路径。
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Constants.Config.MEDIASTORE_RELATIVE_PATH);
        // IS_PENDING=1 表示图片仍在写入中，其他应用暂时不可见，避免相册读取到半成品文件。
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Failed to insert MediaStore row");
        }

        boolean success = false;
        try (OutputStream outputStream = resolver.openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Failed to open MediaStore output stream");
            }
            writer.write(outputStream);
            success = true;
        } finally {
            if (success) {
                ContentValues publishedValues = new ContentValues();
                // IS_PENDING=0 发布图片，使其对相册和其他媒体应用可见。
                publishedValues.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, publishedValues, null, null);
            } else {
                resolver.delete(uri, null, null);
            }
        }

        return StoredWallpaper.mediaStore(uri);
    }

    private StoredWallpaper saveToCache(WallpaperInfo wallpaperInfo, OutputWriter writer)
            throws IOException {
        File dir = new File(appContext.getCacheDir(), CACHE_DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create cache directory: " + dir);
        }
        if (!dir.isDirectory()) {
            throw new IOException("Cache path is not a directory: " + dir);
        }

        File file = new File(dir, wallpaperInfo.getFileName());
        boolean success = false;
        try (OutputStream outputStream = new FileOutputStream(file)) {
            writer.write(outputStream);
            success = true;
        } finally {
            if (!success && file.exists()) {
                file.delete();
            }
        }
        return StoredWallpaper.cache(file);
    }
}
