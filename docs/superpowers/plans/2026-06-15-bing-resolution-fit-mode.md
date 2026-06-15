# Bing Resolution and Fit Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add user-configurable Bing download resolution and wallpaper fit mode while preserving current defaults.

**Architecture:** `SettingsStore` persists resolution and fit mode. `BingSourceOptions` owns source-level normalization and passes resolution to `BingWallpaperSource`. `WallpaperSetter` owns setting behavior and delegates explicit bitmap resizing to a focused `WallpaperBitmapTransformer`.

**Tech Stack:** Android Java, platform `WallpaperManager`, `Bitmap`/`Canvas`, JUnit/Robolectric, existing native `AlertDialog` settings UI.

---

## File Structure

- Modify: `app/src/main/java/xyz/liut/bingwallpaper/Constants.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/v3/source/BingSourceOptions.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/v3/source/BingWallpaperSource.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/v3/settings/SettingsStore.java`
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/wallpaper/WallpaperFitMode.java`
- Create: `app/src/main/java/xyz/liut/bingwallpaper/v3/wallpaper/WallpaperBitmapTransformer.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/v3/wallpaper/WallpaperSetter.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/SyncWallpaperService.java`
- Modify: `app/src/main/java/xyz/liut/bingwallpaper/SettingActivity.java`
- Modify: `app/src/main/res/layout/activity_setting.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify tests under `app/src/test/java/xyz/liut/bingwallpaper/v3/source/`
- Modify tests under `app/src/test/java/xyz/liut/bingwallpaper/v3/settings/`
- Add tests under `app/src/test/java/xyz/liut/bingwallpaper/v3/wallpaper/`

## Tasks

- [ ] Add failing tests for resolution normalization, store persistence, and source URL generation.
- [ ] Implement resolution constants and persistence through `SettingsStore`.
- [ ] Add failing tests for `WallpaperFitMode` and `WallpaperBitmapTransformer`.
- [ ] Implement fit mode enum and bitmap transformation logic.
- [ ] Wire `WallpaperSetter` to use system stream mode by default and bitmap mode when configured.
- [ ] Add settings UI rows, dialogs, labels, and service injection.
- [ ] Run `./gradlew testDebugUnitTest --warning-mode all`, `./gradlew assembleDebug --warning-mode all`, and `git diff --check`.
