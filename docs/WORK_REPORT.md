# Wallpaper Client Work Report

Date: 2026-06-16

## Summary

This project now contains a Tauri 2 wallpaper client with a vanilla HTML/CSS/JavaScript frontend, a shared Rust core crate, a Rust Tauri command layer, and an Android native plugin for applying and saving wallpapers.

The current implementation is strongest on Android. Desktop support has the shared UI and command path in place, but full desktop packaging was not verified on this machine because required Linux system packages are missing. iOS is scaffold-level only and still needs a native adapter.

## Implemented Work

### Project Structure

- Created a Rust workspace with:
  - `crates/wallpaper-core` for source/config/wallpaper domain logic.
  - `src-tauri` for the Tauri application shell and commands.
- Created a Tauri 2 app using the static `src` frontend as `frontendDist`.
- Added project ignore rules for dependency folders, Rust targets, Android build outputs, and Gradle caches.

### Bing Wallpaper Source

- Added Bing archive request configuration:
  - Market selection.
  - Resolution selection.
  - Requested image count.
- Added Bing response mapping into app wallpaper items.
- Normalized relative Bing image URLs into absolute URLs.
- Added deterministic download file naming from wallpaper metadata.

### User Configuration

- Added app config models for:
  - Bing source settings.
  - Fit mode: fill, fit, stretch, center.
  - Scheduled update interval and background notification preference.
  - File-system saving.
  - Android lock-screen and manual-update toast preferences.

### Tauri Commands

- Added commands for:
  - Loading default config.
  - Fetching the Bing gallery.
  - Manually downloading and applying a wallpaper.
  - Applying a selected gallery wallpaper.
  - Clearing the system wallpaper where supported.
- Added platform-specific wallpaper behavior:
  - Android uses a native plugin.
  - Desktop downloads to a persistent file when enabled, or a temporary file otherwise, then calls the desktop wallpaper crate.
  - iOS currently returns an unsupported-platform error for native wallpaper application.

### Android Native Plugin

- Added `WallpaperPlatformPlugin` under `com.liut.wallpaper.platform`.
- Added Android `SET_WALLPAPER` manifest permission.
- Implemented:
  - Base64 image handoff from Rust to Kotlin.
  - Optional MediaStore save into `Pictures/Wallpaper Client`.
  - Main-screen wallpaper application.
  - Optional lock-screen wallpaper application.
  - Wallpaper clearing.
  - Toast notification after applying wallpaper when requested.
  - Android-side fit rendering for fill, fit, stretch, and center.
- Used MediaStore on Android Q+ without requiring broad storage permissions.

### Frontend

- Built a vanilla JavaScript UI with Pico CSS.
- Added controls for:
  - Bing market, resolution, and count.
  - Fit mode.
  - Save-to-file-system toggle.
  - Lock-screen toggle.
  - Scheduled updates.
  - Background and manual notification preferences.
  - Manual sync.
  - Gallery refresh.
  - Apply selected wallpaper.
  - Clear wallpaper.
- Added gallery and preview rendering.
- Added a browser mock path so the static UI can be previewed without the Tauri runtime.

## Verified Work

The following checks were run successfully:

```bash
cargo test -p wallpaper-core
node --check src/main.js
cd src-tauri/gen/android && ./gradlew :app:testUniversalDebugUnitTest --tests com.liut.wallpaper.platform.WallpaperPlatformPluginTest
npm run tauri -- android build --debug
```

The Android debug build produced:

```text
src-tauri/gen/android/app/build/outputs/apk/universal/debug/app-universal-debug.apk
src-tauri/gen/android/app/build/outputs/bundle/universalDebug/app-universal-debug.aab
```

## Not Completed

- Native iOS wallpaper integration is not implemented.
- Real OS-level background scheduling is not implemented. The current scheduler runs only while the web frontend process is alive.
- Desktop wallpaper clearing is not implemented.
- Desktop fit mode is represented in config and preview, but desktop OS-level fit behavior is not implemented.
- Release signing, versioning, store metadata, and CI packaging are not configured.

## Not Verified

- Desktop Tauri build/run was not verified on this Linux machine because `webkit2gtk-4.1` and `rsvg2` are missing.
- Android behavior was not verified on a physical device or emulator. Unit tests and debug packaging pass, but actual wallpaper application and gallery writes still need device validation.
- iOS build was not attempted.
- Windows and macOS release packages were not built.

## Known Environment Notes

- `npm run tauri -- info` reported missing Linux desktop dependencies:
  - `webkit2gtk-4.1`
  - `rsvg2`
- Gradle reported deprecation warnings from the generated Android build, indicating future incompatibility with Gradle 9.0 unless the generated scripts or upstream tooling are updated.

