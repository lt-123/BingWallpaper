# BingWallpaper v3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the app as a minimal Android 10+ Bing wallpaper app that can save wallpapers through MediaStore without storage permissions.

**Architecture:** The v3 flow is split into source, network, storage, wallpaper, sync, schedule, and settings modules. The UI remains simple Java Activity code, while the old custom-source feature is removed and source extensibility is preserved through `WallpaperSource`.

**Tech Stack:** Android Java, Android SDK APIs, `HttpURLConnection`, `MediaStore`, `WallpaperManager`, `JobScheduler`, JUnit/Robolectric only for tests.

---

## File Structure

Create:
- `app/src/main/java/xyz/liut/bingwallpaper/v3/source/WallpaperSource.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/source/WallpaperInfo.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/source/BingWallpaperSource.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/network/HttpDownloader.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/storage/StoredWallpaper.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/storage/WallpaperStore.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/wallpaper/WallpaperSetter.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/settings/SettingsStore.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/schedule/ScheduleManager.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/sync/SyncResult.java`
- `app/src/main/java/xyz/liut/bingwallpaper/v3/sync/WallpaperSyncUseCase.java`
- `app/src/test/java/xyz/liut/bingwallpaper/v3/source/BingWallpaperSourceTest.java`
- `app/src/test/java/xyz/liut/bingwallpaper/v3/schedule/ScheduleManagerTest.java`
- `app/src/test/java/xyz/liut/bingwallpaper/v3/sync/WallpaperSyncUseCaseTest.java`

Modify:
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/xyz/liut/bingwallpaper/Constants.java`
- `app/src/main/java/xyz/liut/bingwallpaper/SettingActivity.java`
- `app/src/main/java/xyz/liut/bingwallpaper/TimeListActivity.java`
- `app/src/main/java/xyz/liut/bingwallpaper/SyncWallpaperService.java`
- `app/src/main/java/xyz/liut/bingwallpaper/AlarmJob.java`
- `app/src/main/java/xyz/liut/bingwallpaper/ManualSetActivity.java`
- `app/src/main/java/xyz/liut/bingwallpaper/SyncTileService.java`
- `app/src/main/res/layout/activity_setting.xml`
- `app/src/main/res/menu/setting_menu.xml`
- `app/src/main/res/values/strings.xml`

Delete or leave unused until final cleanup:
- `app/src/main/java/xyz/liut/bingwallpaper/SourceListActivity.java`
- `app/src/main/java/xyz/liut/bingwallpaper/AddSourceActivity.java`
- `app/src/main/java/xyz/liut/bingwallpaper/SourceListAdapter.java`
- `app/src/main/java/xyz/liut/bingwallpaper/SourceManager.java`
- `app/src/main/java/xyz/liut/bingwallpaper/bean/SourceBean.java`
- `app/src/main/java/xyz/liut/bingwallpaper/engine/*`
- `app/src/main/java/xyz/liut/bingwallpaper/http/*`
- `app/src/main/res/layout/activity_add_source.xml`
- `app/src/main/res/layout/activity_source_list.xml`
- `app/src/main/res/layout/item_source.xml`
- `app/src/main/res/menu/source_list_menu.xml`
- Network-dependent tests under `app/src/test/java/xyz/liut/bingwallpaper/engine`, `app/src/test/java/xyz/liut/bingwallpaper/http`, and `SourceManagerTest`.

## Task 1: Build and Manifest Baseline

**Files:**
- Modify: `app/build.gradle`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/Constants.java`

- [ ] **Step 1: Raise minSdk to 29**

In `app/build.gradle`, set:

```groovy
defaultConfig {
    applicationId = "xyz.liut.bingwallpaper"
    minSdk = 29
    targetSdk = 36
}
```

- [ ] **Step 2: Remove external storage permission**

In `AndroidManifest.xml`, remove:

```xml
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />
```

Also remove `android:requestLegacyExternalStorage="true"` from `<application>`.

- [ ] **Step 3: Remove old public path constant**

In `Constants.Config`, remove `WALLPAPER_SAVE_PATH`. Add:

```java
/**
 * 使用 MediaStore 保存到公共图片库时的相对目录。
 */
String MEDIASTORE_RELATIVE_PATH = "Pictures/BingWallpaper";
```

- [ ] **Step 4: Compile**

Run: `./gradlew assembleDebug --warning-mode all`

Expected: if compilation fails because `SyncWallpaperService` still references `WALLPAPER_SAVE_PATH`, record the failure and continue to Task 7, where that old save path is removed. No other failure is acceptable for this step.

## Task 2: Settings Store

**Files:**
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/settings/SettingsStore.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/Constants.java`

- [ ] **Step 1: Add v3 preference keys**

In `Constants.Default`, keep existing keys that still apply and add no Wi-Fi key usage. Add comments:

```java
/**
 * 保存到系统相册 KEY。
 */
String KEY_SAVE_TO_GALLERY = "KEY_SAVE_TO_GALLERY";
```

- [ ] **Step 2: Create SettingsStore**

Implement `SettingsStore` with Chinese Javadoc:

```java
package xyz.liut.bingwallpaper.v3.settings;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import xyz.liut.bingwallpaper.Constants;
import xyz.liut.bingwallpaper.TimedListManager;
import xyz.liut.bingwallpaper.utils.SpTool;

/**
 * v3 配置仓库。
 *
 * <p>该类封装 SharedPreferences 读写，避免业务流程直接依赖偏好项 key。</p>
 */
public class SettingsStore {

    private final Context appContext;
    private final SpTool spTool;

    public SettingsStore(Context context) {
        this.appContext = context.getApplicationContext();
        this.spTool = SpTool.getDefault(appContext);
    }

    public boolean saveToGallery() {
        return spTool.get(Constants.Default.KEY_SAVE_TO_GALLERY, true);
    }

    public void saveToGallery(boolean value) {
        spTool.save(Constants.Default.KEY_SAVE_TO_GALLERY, value);
    }

    public boolean setLockScreen() {
        return spTool.get(Constants.Default.KEY_LOCK_SCREEN, false);
    }

    public void setLockScreen(boolean value) {
        spTool.save(Constants.Default.KEY_LOCK_SCREEN, value);
    }

    public boolean showToast() {
        return spTool.get(Constants.Default.KEY_SHOW_TOAST, true);
    }

    public void showToast(boolean value) {
        spTool.save(Constants.Default.KEY_SHOW_TOAST, value);
    }

    public boolean showManualEntry() {
        return spTool.get(Constants.Default.KEY_SHOW_MANUAL_SYNC, false);
    }

    public void showManualEntry(boolean value) {
        spTool.save(Constants.Default.KEY_SHOW_MANUAL_SYNC, value);
    }

    public boolean hideMainIcon() {
        return spTool.get(Constants.Default.KEY_HIDE_MAIN, false);
    }

    public void hideMainIcon(boolean value) {
        spTool.save(Constants.Default.KEY_HIDE_MAIN, value);
    }

    public List<String> timedList() {
        return new ArrayList<>(TimedListManager.loadTimedList(appContext));
    }
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew compileDebugJavaWithJavac`

Expected: compilation can still fail on old Service references until Task 5 replaces them.

## Task 3: Wallpaper Source

**Files:**
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/source/WallpaperInfo.java`
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/source/WallpaperSource.java`
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/source/BingWallpaperSource.java`
- Test: `app/src/test/java/xyz/liut/bingwallpaper/v3/source/BingWallpaperSourceTest.java`

- [ ] **Step 1: Write parsing test**

Create `BingWallpaperSourceTest` using a fixed Bing JSON sample:

```java
@Test
public void parseBingJson_returnsWallpaperInfo() throws Exception {
    String json = "{\"images\":[{\"url\":\"/th?id=OHR.Test_1920x1080.jpg\",\"urlbase\":\"/th?id=OHR.Test\",\"copyright\":\"测试版权\",\"title\":\"测试标题\"}]}";

    WallpaperInfo info = BingWallpaperSource.parseJson(json, "UHD");

    assertEquals("https://www.bing.com/th?id=OHR.Test_UHD.jpg", info.getImageUrl());
    assertEquals("OHR.Test_UHD.jpg", info.getFileName());
    assertEquals("测试标题", info.getTitle());
}
```

- [ ] **Step 2: Implement source model and interface**

`WallpaperInfo`:

```java
/**
 * 一张可下载壁纸的描述信息。
 */
public final class WallpaperInfo {
    private final String imageUrl;
    private final String fileName;
    private final String title;
    private final String description;
}
```

`WallpaperSource`:

```java
/**
 * 壁纸源抽象。
 *
 * <p>后续新增其它来源时，只需要实现该接口，不需要改同步主流程。</p>
 */
public interface WallpaperSource {
    WallpaperInfo fetch() throws Exception;
    String name();
}
```

- [ ] **Step 3: Implement BingWallpaperSource**

Use `HttpURLConnection` or existing v3 downloader in later task for network; keep parsing static and testable:

```java
public static WallpaperInfo parseJson(String json, String resolution) throws JSONException {
    JSONObject root = new JSONObject(json);
    JSONObject image = root.getJSONArray("images").getJSONObject(0);
    String urlBase = image.getString("urlbase");
    String id = urlBase.substring(urlBase.indexOf("=") + 1);
    String fileName = id + "_" + resolution + ".jpg";
    String imageUrl = "https://www.bing.com" + urlBase + "_" + resolution + ".jpg";
    return new WallpaperInfo(imageUrl, fileName, image.optString("title"), image.optString("copyright"));
}
```

- [ ] **Step 4: Run test**

Run: `./gradlew testDebugUnitTest --tests xyz.liut.bingwallpaper.v3.source.BingWallpaperSourceTest`

Expected: pass.

## Task 4: Network and Storage

**Files:**
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/network/HttpDownloader.java`
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/storage/StoredWallpaper.java`
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/storage/WallpaperStore.java`

- [ ] **Step 1: Implement HttpDownloader**

Add Chinese Javadoc and support writing to an `OutputStream`:

```java
/**
 * 基于 HttpURLConnection 的下载器。
 *
 * <p>下载器只负责网络读取和流复制，不决定图片保存位置。</p>
 */
public class HttpDownloader {
    public interface ProgressCallback {
        void onProgress(long current, long total);
    }

    public void download(String url, OutputStream outputStream, ProgressCallback callback) throws IOException {
        HttpURLConnection connection = open(url);
        int code = connection.getResponseCode();
        if (code >= 300 && code < 400) {
            String location = connection.getHeaderField("Location");
            if (location == null) {
                throw new IOException("重定向失败");
            }
            download(new URL(new URL(url), location).toString(), outputStream, callback);
            return;
        }
        if (code < 200 || code >= 300) {
            throw new IOException("网络请求出错 " + code);
        }
        long total = connection.getContentLengthLong();
        try (InputStream inputStream = connection.getInputStream()) {
            byte[] buffer = new byte[8192];
            long current = 0L;
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
                current += length;
                if (callback != null) {
                    callback.onProgress(current, total);
                }
            }
        }
    }

    public String get(String url) throws IOException {
        HttpURLConnection connection = open(url);
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("网络请求出错 " + code);
        }
        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toString("UTF-8");
        }
    }
}
```

- [ ] **Step 2: Implement StoredWallpaper**

Represent either MediaStore URI or cache file:

```java
/**
 * 已保存的壁纸文件。
 */
public final class StoredWallpaper {
    public enum Location {
        MEDIASTORE,
        CACHE
    }
}
```

- [ ] **Step 3: Implement WallpaperStore**

`WallpaperStore.save(WallpaperInfo info, boolean saveToGallery, DownloadWriter writer)` should:
- create MediaStore item with `IS_PENDING=1` when gallery is enabled;
- open output stream and call writer;
- set `IS_PENDING=0` after success;
- delete MediaStore row on failure;
- otherwise save to `getCacheDir()/wallpapers/<fileName>`.

Include Chinese comments around `IS_PENDING`:

```java
// Android 10+ 写入公共图片库时先标记为 pending，避免相册扫描到未写完的文件。
values.put(MediaStore.Images.Media.IS_PENDING, 1);
```

- [ ] **Step 4: Compile**

Run: `./gradlew compileDebugJavaWithJavac`

Expected: new classes compile.

## Task 5: Wallpaper Setter and Sync Use Case

**Files:**
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/wallpaper/WallpaperSetter.java`
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/sync/SyncResult.java`
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/sync/WallpaperSyncUseCase.java`
- Test: `app/src/test/java/xyz/liut/bingwallpaper/v3/sync/WallpaperSyncUseCaseTest.java`

- [ ] **Step 1: Write use case tests**

Create fake source, fake store, fake setter, and fake scheduler. Cover:
- success saves and sets wallpaper;
- failure returns error;
- cache result is cleaned after success.

- [ ] **Step 2: Implement WallpaperSetter**

Add Chinese Javadoc:

```java
/**
 * 系统壁纸设置器。
 *
 * <p>该类只处理 WallpaperManager 调用，输入可以来自 MediaStore Uri 或应用缓存文件。</p>
 */
public class WallpaperSetter {
    public void set(StoredWallpaper wallpaper, boolean lockScreen) throws IOException {
        WallpaperManager manager = WallpaperManager.getInstance(context);
        try (InputStream inputStream = wallpaper.openInputStream(context)) {
            if (lockScreen) {
                // 同时设置系统桌面和锁屏壁纸。
                manager.setStream(inputStream, null, true, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
            } else {
                // 只设置系统桌面壁纸。
                manager.setStream(inputStream);
            }
        }
    }
}
```

- [ ] **Step 3: Implement SyncResult**

Return a simple success/error object:

```java
package xyz.liut.bingwallpaper.v3.sync;

/**
 * 一次壁纸同步的结果。
 */
public final class SyncResult {

    private final boolean success;
    private final String message;
    private final Exception exception;

    private SyncResult(boolean success, String message, Exception exception) {
        this.success = success;
        this.message = message;
        this.exception = exception;
    }

    public static SyncResult success(String message) {
        return new SyncResult(true, message, null);
    }

    public static SyncResult failure(String message, Exception exception) {
        return new SyncResult(false, message, exception);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Exception getException() {
        return exception;
    }
}
```

- [ ] **Step 4: Implement WallpaperSyncUseCase**

The main method:

```java
/**
 * 执行一次壁纸同步。
 *
 * <p>流程：获取 Bing 信息、下载并保存、设置壁纸、清理临时缓存。</p>
 */
public SyncResult sync(ProgressCallback callback) {
    try {
        WallpaperInfo info = source.fetch();
        StoredWallpaper stored = store.save(info, settings.saveToGallery(), output -> downloader.download(info.getImageUrl(), output, callback));
        setter.set(stored, settings.setLockScreen());
        stored.deleteTemporary();
        return SyncResult.success("设置壁纸成功");
    } catch (Exception e) {
        return SyncResult.failure("同步壁纸失败: " + e.getMessage(), e);
    }
}
```

- [ ] **Step 5: Run tests**

Run: `./gradlew testDebugUnitTest --tests xyz.liut.bingwallpaper.v3.sync.WallpaperSyncUseCaseTest`

Expected: pass.

## Task 6: Schedule Manager and Job Wiring

**Files:**
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/schedule/ScheduleManager.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/AlarmJob.java`
- Test: `app/src/test/java/xyz/liut/bingwallpaper/v3/schedule/ScheduleManagerTest.java`

- [ ] **Step 1: Write schedule calculation tests**

Test next run for a time later today and a time already passed today.

- [ ] **Step 2: Implement ScheduleManager**

Include Chinese Javadoc:

```java
/**
 * v3 定时任务管理器。
 *
 * <p>负责把用户设置的多个 HH:mm 时间点转换为 JobScheduler 任务。</p>
 */
public class ScheduleManager {
    static long nextDelayMinutes(Calendar now, int hour, int minute) {
        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 1);
        target.set(Calendar.MILLISECOND, 0);
        if (!target.after(now)) {
            target.add(Calendar.DATE, 1);
        }
        return Math.max(1L, (target.getTimeInMillis() - now.getTimeInMillis()) / (60L * 1000L));
    }
}
```

- [ ] **Step 3: Update AlarmJob**

Keep `AlarmJob` as the JobService entry. `onStartJob()` calls `SyncWallpaperService.start(this)`.

- [ ] **Step 4: Run tests**

Run: `./gradlew testDebugUnitTest --tests xyz.liut.bingwallpaper.v3.schedule.ScheduleManagerTest`

Expected: pass.

## Task 7: Replace SyncWallpaperService

**Files:**
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/SyncWallpaperService.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/SyncTileService.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/ManualSetActivity.java`

- [ ] **Step 1: Remove old EngineFactory flow**

Delete fields and imports related to `IWallpaperEngine`, `EngineFactory`, `SourceBean`, `NetworkUtil`, and old file save logic.

- [ ] **Step 2: Build v3 use case in service**

In `onCreate()`, instantiate:
- `SettingsStore`
- `HttpDownloader`
- `BingWallpaperSource`
- `WallpaperStore`
- `WallpaperSetter`
- `ScheduleManager`
- `WallpaperSyncUseCase`

- [ ] **Step 3: Implement retry loop**

Use service thread to run `sync()`. Retry up to 3 times on failure, update notification with Chinese messages, then schedule 30-minute retry.

- [ ] **Step 4: Preserve entry points**

Ensure `SyncTileService.onClick()` and `ManualSetActivity.onCreate()` still call `SyncWallpaperService.start(this)`.

- [ ] **Step 5: Compile**

Run: `./gradlew assembleDebug --warning-mode all`

Expected: debug APK builds.

## Task 8: Simplify Settings UI

**Files:**
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/SettingActivity.java`
- Modify: `app/src/main/res/layout/activity_setting.xml`
- Modify: `app/src/main/res/menu/setting_menu.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Remove source and Wi-Fi controls from layout**

Remove `ll_source`, `tv_source`, `ll_only_wifi`, `tv_only_wifi`, and `sw_only_wifi`.

- [ ] **Step 2: Update save copy**

Change save strings to describe album saving:

```xml
<string name="save_path">保存到相册</string>
<string name="save_to_gallery_desc">保存到 Pictures/BingWallpaper</string>
<string name="no_save">仅设置壁纸，不保存到相册</string>
```

- [ ] **Step 3: Update SettingActivity**

Use `SettingsStore`. Remove source list click handling and Wi-Fi click handling. Keep:
- time list click;
- save to gallery switch;
- lock screen switch;
- toast switch;
- manual entry switch;
- main icon switch;
- setup now button;
- clear wallpaper button;
- about menu.

Preserve Chinese comments for each branch.

- [ ] **Step 4: Compile**

Run: `./gradlew assembleDebug --warning-mode all`

Expected: debug APK builds.

## Task 9: Remove Custom Source and Old Engine Code

**Files:**
- Delete old source UI/activity/adapter/bean/engine/http files listed in File Structure.
- Delete or rewrite network-dependent tests.
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Remove manifest activities**

Remove `SourceListActivity` and `AddSourceActivity` declarations.

- [ ] **Step 2: Delete unused files**

Delete old custom source, engine, direct HTTP callback, and source tests.

- [ ] **Step 3: Update imports**

Run: `rg "SourceListActivity|AddSourceActivity|SourceManager|EngineFactory|IWallpaperEngine|HttpClient|KEY_ONLY_WIFI|WALLPAPER_SAVE_PATH" app/src/main app/src/test`

Expected: no matches, except historical docs if any.

- [ ] **Step 4: Compile**

Run: `./gradlew assembleDebug assembleRelease --warning-mode all`

Expected: both APKs build.

## Task 10: Final Verification

**Files:**
- All changed files.

- [ ] **Step 1: Run focused unit tests**

Run:

```bash
./gradlew testDebugUnitTest --tests 'xyz.liut.bingwallpaper.v3.*'
```

Expected: all v3 tests pass.

- [ ] **Step 2: Run final compile**

Run:

```bash
./gradlew assembleDebug assembleRelease --warning-mode all
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Verify permissions**

Run:

```bash
rg "WRITE_EXTERNAL_STORAGE|READ_EXTERNAL_STORAGE|READ_MEDIA_IMAGES|requestLegacyExternalStorage" app/src/main/AndroidManifest.xml app/build.gradle
```

Expected: no matches.

- [ ] **Step 4: Verify no runtime third-party dependency was added**

Run:

```bash
git diff -- app/build.gradle
```

Expected: no new `implementation` dependency lines. Existing `testImplementation` and `androidTestImplementation` dependencies may remain unchanged.

- [ ] **Step 5: Review comments**

Run:

```bash
rg -n "class WallpaperSource|class BingWallpaperSource|class WallpaperStore|class WallpaperSetter|class ScheduleManager|class WallpaperSyncUseCase|IS_PENDING|RELATIVE_PATH|重试|锁屏" app/src/main/java
```

Expected: core classes and complex Android-specific branches have Chinese Javadoc or Chinese comments.
