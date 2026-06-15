# Bing Market Region Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add configurable Bing API market support with automatic image host selection for issue #2.

**Architecture:** Keep the feature inside the existing v3 boundaries: `SettingsStore` persists user choices, `BingSourceOptions` validates and normalizes source configuration, `BingWallpaperSource` builds API/image URLs, and `SettingActivity` only handles user interaction. `WallpaperSyncUseCase` remains unchanged.

**Tech Stack:** Android Java, platform `AlertDialog`/`EditText`, `Uri.Builder`, JUnit/Robolectric.

---

## File Structure

- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/source/BingSourceOptions.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/Constants.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/v3/settings/SettingsStore.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/v3/source/BingWallpaperSource.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/SyncWallpaperService.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/SettingActivity.java`
- Modify: `app/src/main/res/layout/activity_setting.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/xyz/liut/bingwallpaper/v3/source/BingSourceOptionsTest.java`
- Test: `app/src/test/java/xyz/liut/bingwallpaper/v3/settings/SettingsStoreTest.java`
- Modify test: `app/src/test/java/xyz/liut/bingwallpaper/v3/source/BingWallpaperSourceTest.java`

## Tasks

- [ ] Add failing tests for option normalization, settings defaults/read-write, API URL `mkt`, and market-derived image host parsing.
- [ ] Implement `BingSourceOptions`, settings keys, and `SettingsStore` methods.
- [ ] Update `BingWallpaperSource` to use `BingSourceOptions`, `buildApiUrl`, and host-aware `parseJson`.
- [ ] Inject `settingsStore.bingSourceOptions()` in `SyncWallpaperService`.
- [ ] Add settings UI row, strings, click handler, dialog, custom market validation, and subtitle.
- [ ] Run `./gradlew testDebugUnitTest --warning-mode all` and `./gradlew assembleDebug --warning-mode all`.
