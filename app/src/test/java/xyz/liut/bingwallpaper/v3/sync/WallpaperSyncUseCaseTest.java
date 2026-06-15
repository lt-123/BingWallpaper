package xyz.liut.bingwallpaper.v3.sync;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import xyz.liut.bingwallpaper.v3.network.HttpDownloader;
import xyz.liut.bingwallpaper.v3.source.WallpaperInfo;
import xyz.liut.bingwallpaper.v3.source.WallpaperSource;
import xyz.liut.bingwallpaper.v3.storage.StoredWallpaper;
import xyz.liut.bingwallpaper.v3.storage.WallpaperStore;

/**
 * 壁纸同步用例测试。
 */
public class WallpaperSyncUseCaseTest {

    @Test
    public void syncSuccessSavesDownloadedWallpaperAndSetsIt() {
        FakeSource source = new FakeSource();
        FakeStore store = new FakeStore(StoredWallpaper.cache(new java.io.File("build/test-cache-wallpaper.jpg")));
        FakeSettings settings = new FakeSettings(true, true);
        FakeSetter setter = new FakeSetter();
        FakeDownloader downloader = new FakeDownloader();
        WallpaperSyncUseCase useCase = new WallpaperSyncUseCase(source, store, settings, setter, downloader);

        SyncResult result = useCase.sync();

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals("同步成功", result.getMessage());
        Assert.assertNull(result.getException());
        Assert.assertSame(source.info, store.savedInfo);
        Assert.assertTrue(store.saveToGallery);
        Assert.assertEquals(source.info.getImageUrl(), downloader.downloadedUrl);
        Assert.assertSame(store.storedWallpaper, setter.storedWallpaper);
        Assert.assertTrue(setter.lockScreen);
    }

    @Test
    public void syncFailureReturnsErrorResult() {
        FakeSource source = new FakeSource(new IOException("source failed"));
        FakeStore store = new FakeStore(StoredWallpaper.cache(new java.io.File("build/test-cache-wallpaper.jpg")));
        FakeSettings settings = new FakeSettings(false, false);
        FakeSetter setter = new FakeSetter();
        FakeDownloader downloader = new FakeDownloader();
        WallpaperSyncUseCase useCase = new WallpaperSyncUseCase(source, store, settings, setter, downloader);

        SyncResult result = useCase.sync();

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getMessage().contains("source failed"));
        Assert.assertNotNull(result.getException());
        Assert.assertFalse(store.saved);
        Assert.assertFalse(setter.set);
    }

    @Test
    public void syncSuccessDeletesTemporaryCacheResult() throws Exception {
        FakeSource source = new FakeSource();
        File cacheFile = File.createTempFile("wallpaper-sync-use-case", ".jpg");
        FakeStore store = new FakeStore(StoredWallpaper.cache(cacheFile));
        FakeSettings settings = new FakeSettings(false, false);
        FakeSetter setter = new FakeSetter();
        FakeDownloader downloader = new FakeDownloader();
        WallpaperSyncUseCase useCase = new WallpaperSyncUseCase(source, store, settings, setter, downloader);

        SyncResult result = useCase.sync();

        Assert.assertTrue(result.isSuccess());
        Assert.assertFalse(cacheFile.exists());
    }

    @Test
    public void syncDeletesTemporaryCacheResultWhenSetterFails() throws Exception {
        FakeSource source = new FakeSource();
        File cacheFile = File.createTempFile("wallpaper-sync-use-case-setter-fails", ".jpg");
        FakeStore store = new FakeStore(StoredWallpaper.cache(cacheFile));
        FakeSettings settings = new FakeSettings(false, false);
        FakeSetter setter = new FakeSetter(new IOException("setter failed"));
        FakeDownloader downloader = new FakeDownloader();
        WallpaperSyncUseCase useCase = new WallpaperSyncUseCase(source, store, settings, setter, downloader);

        SyncResult result = useCase.sync();

        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getMessage().contains("setter failed"));
        Assert.assertNotNull(result.getException());
        Assert.assertFalse(cacheFile.exists());
    }

    private static class FakeSource implements WallpaperSource {
        private final WallpaperInfo info =
                new WallpaperInfo("https://example.com/wallpaper.jpg", "wallpaper.jpg", "标题", "描述");
        private final Exception exception;

        private FakeSource() {
            this(null);
        }

        private FakeSource(Exception exception) {
            this.exception = exception;
        }

        @Override
        public WallpaperInfo fetch() throws Exception {
            if (exception != null) {
                throw exception;
            }
            return info;
        }

        @Override
        public String name() {
            return "fake";
        }
    }

    private static class FakeStore implements WallpaperSyncUseCase.Store {
        private final StoredWallpaper storedWallpaper;
        private boolean saved;
        private WallpaperInfo savedInfo;
        private boolean saveToGallery;

        private FakeStore(StoredWallpaper storedWallpaper) {
            this.storedWallpaper = storedWallpaper;
        }

        @Override
        public StoredWallpaper save(WallpaperInfo info, boolean saveToGallery, WallpaperStore.OutputWriter writer)
                throws IOException {
            saved = true;
            savedInfo = info;
            this.saveToGallery = saveToGallery;
            writer.write(new OutputStream() {
                @Override
                public void write(int b) {
                }
            });
            return storedWallpaper;
        }
    }

    private static class FakeSettings implements WallpaperSyncUseCase.Settings {
        private final boolean saveToGallery;
        private final boolean lockScreen;

        private FakeSettings(boolean saveToGallery, boolean lockScreen) {
            this.saveToGallery = saveToGallery;
            this.lockScreen = lockScreen;
        }

        @Override
        public boolean saveToGallery() {
            return saveToGallery;
        }

        @Override
        public boolean setLockScreen() {
            return lockScreen;
        }
    }

    private static class FakeSetter implements WallpaperSyncUseCase.Setter {
        private final IOException exception;
        private boolean set;
        private StoredWallpaper storedWallpaper;
        private boolean lockScreen;

        private FakeSetter() {
            this(null);
        }

        private FakeSetter(IOException exception) {
            this.exception = exception;
        }

        @Override
        public void set(StoredWallpaper storedWallpaper, boolean lockScreen) throws IOException {
            set = true;
            this.storedWallpaper = storedWallpaper;
            this.lockScreen = lockScreen;
            if (exception != null) {
                throw exception;
            }
        }
    }

    private static class FakeDownloader implements WallpaperSyncUseCase.Downloader {
        private String downloadedUrl;

        @Override
        public void download(String url, OutputStream output, HttpDownloader.ProgressCallback callback)
                throws IOException {
            downloadedUrl = url;
            output.write(1);
            if (callback != null) {
                callback.onProgress(1, 1);
            }
        }
    }
}
