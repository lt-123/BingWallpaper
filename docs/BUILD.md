# Build Instructions

## Prerequisites

Install the common toolchain:

- Node.js and npm.
- Rust and Cargo.
- Tauri CLI dependency from `package.json` via `npm install`.

Install platform-specific toolchains as needed:

- Linux desktop: WebKitGTK 4.1 development package, librsvg tools, and the normal Tauri Linux dependencies for the target distribution.
- Android: Android Studio or Android SDK command-line tools, Android NDK, Java, and Android platform/build tools.
- macOS: Xcode command-line tools.
- Windows: Microsoft C++ Build Tools and WebView2 runtime.
- iOS: Xcode, iOS SDK, and the Tauri iOS setup. The current app still needs native iOS wallpaper support before a functional iOS release.

Install project dependencies:

```bash
npm install
```

## Validation Before Release Builds

Run the portable checks first:

```bash
cargo test -p wallpaper-core
node --check src/main.js
```

Run Android native unit tests when building Android:

```bash
cd src-tauri/gen/android
./gradlew :app:testUniversalDebugUnitTest --tests com.liut.wallpaper.platform.WallpaperPlatformPluginTest
cd ../../..
```

## Desktop Debug Run

Use this during local development:

```bash
npm run dev
```

The frontend is static and can also be previewed in a browser with:

```bash
python3 -m http.server 4173 -d src
```

Open `http://127.0.0.1:4173`. This uses the JavaScript mock path and does not exercise native wallpaper APIs.

## Desktop Release Packages

Build desktop bundles for the current host platform:

```bash
npm run tauri -- build
```

Typical output locations are under:

```text
src-tauri/target/release/bundle/
```

Notes:

- Linux builds require the WebKitGTK and librsvg packages installed before running the command.
- macOS release signing/notarization is not configured yet.
- Windows installer signing is not configured yet.
- Cross-building desktop bundles is not configured; build each platform on the matching host unless a dedicated cross-build setup is added.

## Android Debug Package

Build a debug APK and AAB:

```bash
npm run tauri -- android build --debug
```

Expected debug outputs:

```text
src-tauri/gen/android/app/build/outputs/apk/universal/debug/app-universal-debug.apk
src-tauri/gen/android/app/build/outputs/bundle/universalDebug/app-universal-debug.aab
```

## Android Release Package

Build a release package:

```bash
npm run tauri -- android build
```

Before distributing a release build:

- Configure Android signing credentials.
- Verify the release artifact on a device or emulator.
- Confirm gallery saving and home/lock-screen application on the target Android versions.

Release outputs are expected under:

```text
src-tauri/gen/android/app/build/outputs/
```

## iOS

The Tauri iOS project can be initialized and built only after the iOS native wallpaper adapter is implemented and the iOS toolchain is configured.

Expected flow after that work exists:

```bash
npm run tauri -- ios init
npm run tauri -- ios build
```

Do not treat iOS output as releasable until native wallpaper behavior has been implemented and tested on iOS hardware or simulator where applicable.

