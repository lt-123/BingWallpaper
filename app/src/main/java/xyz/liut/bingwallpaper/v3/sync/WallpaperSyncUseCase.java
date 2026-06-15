package xyz.liut.bingwallpaper.v3.sync;

import java.io.IOException;
import java.io.OutputStream;

import xyz.liut.bingwallpaper.v3.network.HttpDownloader;
import xyz.liut.bingwallpaper.v3.settings.SettingsStore;
import xyz.liut.bingwallpaper.v3.source.WallpaperInfo;
import xyz.liut.bingwallpaper.v3.source.WallpaperSource;
import xyz.liut.bingwallpaper.v3.storage.StoredWallpaper;
import xyz.liut.bingwallpaper.v3.storage.WallpaperStore;
import xyz.liut.bingwallpaper.v3.wallpaper.WallpaperSetter;

/**
 * 壁纸同步用例。
 * <p>
 * 串联壁纸信息获取、图片下载保存、系统壁纸设置和临时缓存清理，使上层入口只需触发一次同步。
 */
public class WallpaperSyncUseCase {

    private static final String SUCCESS_MESSAGE = "同步成功";

    private final WallpaperSource source;
    private final Store store;
    private final Settings settings;
    private final Setter setter;
    private final Downloader downloader;
    private final HttpDownloader.ProgressCallback progressCallback;

    /**
     * 创建壁纸同步用例。
     *
     * @param source     壁纸来源
     * @param store      壁纸保存器
     * @param settings   设置存储
     * @param setter     系统壁纸设置器
     * @param downloader 图片下载器
     */
    public WallpaperSyncUseCase(WallpaperSource source, WallpaperStore store, SettingsStore settings,
                                WallpaperSetter setter, HttpDownloader downloader) {
        this(source,
                new WallpaperStoreAdapter(store),
                new SettingsStoreAdapter(settings),
                new WallpaperSetterAdapter(setter),
                new HttpDownloaderAdapter(downloader),
                null);
    }

    WallpaperSyncUseCase(WallpaperSource source, Store store, Settings settings, Setter setter,
                         Downloader downloader) {
        this(source, store, settings, setter, downloader, null);
    }

    WallpaperSyncUseCase(WallpaperSource source, Store store, Settings settings, Setter setter,
                         Downloader downloader, HttpDownloader.ProgressCallback progressCallback) {
        this.source = source;
        this.store = store;
        this.settings = settings;
        this.setter = setter;
        this.downloader = downloader;
        this.progressCallback = progressCallback;
    }

    /**
     * 执行一次壁纸同步。
     *
     * @return 同步结果
     */
    public SyncResult sync() {
        try {
            // 同步流程：获取元数据，根据设置保存下载结果，再调用系统壁纸设置入口。
            WallpaperInfo info = source.fetch();
            StoredWallpaper stored = store.save(info, settings.saveToGallery(), new WallpaperStore.OutputWriter() {
                @Override
                public void write(OutputStream output) throws IOException {
                    downloader.download(info.getImageUrl(), output, progressCallback);
                }
            });
            try {
                setter.set(stored, settings.setLockScreen());
            } finally {
                // 临时缓存清理：保存完成后无论设置成功或失败都清理缓存文件，已保存到相册的图片不会被删除。
                stored.deleteTemporary();
            }
            return SyncResult.success(SUCCESS_MESSAGE);
        } catch (Exception e) {
            return SyncResult.failure("同步失败: " + e.getMessage(), e);
        }
    }

    interface Store {
        StoredWallpaper save(WallpaperInfo info, boolean saveToGallery, WallpaperStore.OutputWriter writer)
                throws IOException;
    }

    interface Settings {
        boolean saveToGallery();

        boolean setLockScreen();
    }

    interface Setter {
        void set(StoredWallpaper storedWallpaper, boolean lockScreen) throws IOException;
    }

    interface Downloader {
        void download(String url, OutputStream output, HttpDownloader.ProgressCallback callback) throws IOException;
    }

    private static class WallpaperStoreAdapter implements Store {
        private final WallpaperStore store;

        private WallpaperStoreAdapter(WallpaperStore store) {
            this.store = store;
        }

        @Override
        public StoredWallpaper save(WallpaperInfo info, boolean saveToGallery, WallpaperStore.OutputWriter writer)
                throws IOException {
            return store.save(info, saveToGallery, writer);
        }
    }

    private static class SettingsStoreAdapter implements Settings {
        private final SettingsStore settings;

        private SettingsStoreAdapter(SettingsStore settings) {
            this.settings = settings;
        }

        @Override
        public boolean saveToGallery() {
            return settings.saveToGallery();
        }

        @Override
        public boolean setLockScreen() {
            return settings.setLockScreen();
        }
    }

    private static class WallpaperSetterAdapter implements Setter {
        private final WallpaperSetter setter;

        private WallpaperSetterAdapter(WallpaperSetter setter) {
            this.setter = setter;
        }

        @Override
        public void set(StoredWallpaper storedWallpaper, boolean lockScreen) throws IOException {
            setter.set(storedWallpaper, lockScreen);
        }
    }

    private static class HttpDownloaderAdapter implements Downloader {
        private final HttpDownloader downloader;

        private HttpDownloaderAdapter(HttpDownloader downloader) {
            this.downloader = downloader;
        }

        @Override
        public void download(String url, OutputStream output, HttpDownloader.ProgressCallback callback)
                throws IOException {
            downloader.download(url, output, callback);
        }
    }
}
