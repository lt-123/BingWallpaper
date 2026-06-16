# Background Scheduling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将自动更新从前端 `setInterval` 重构为 Linux 托盘常驻后端调度和 Android WorkManager 原生调度。

**Architecture:** 前端只保存配置并调用 Tauri 调度命令。Linux 桌面端在 Rust 进程内维护可取消的后台线程，并通过 Tauri tray 隐藏/显示窗口和退出应用。Android 端由 Kotlin plugin 注册/取消 WorkManager 周期任务，Worker 读取 SharedPreferences 后下载并应用壁纸。

**Tech Stack:** Tauri 2, Rust std thread/channel, serde, vanilla JavaScript, Android Kotlin, AndroidX WorkManager.

---

### Task 1: Rust 调度状态与命令

**Files:**
- Create: `src-tauri/src/scheduler.rs`
- Modify: `src-tauri/src/lib.rs`
- Test: `src-tauri/src/scheduler.rs`

- [ ] **Step 1: Write failing Rust tests**

Add tests for `SchedulerState` behavior:

```rust
#[test]
fn disabled_config_cancels_active_schedule() {
    let scheduler = SchedulerState::default();
    let status = scheduler.configure(AppConfig::default()).unwrap();
    assert!(!status.enabled);
}

#[test]
fn enabled_config_reports_interval_minutes() {
    let scheduler = SchedulerState::default();
    let mut config = AppConfig::default();
    config.schedule.enabled = true;
    config.schedule.interval_minutes = 42;

    let status = scheduler.configure(config).unwrap();

    assert!(status.enabled);
    assert!(status.message.contains("42"));
}
```

- [ ] **Step 2: Run tests to verify RED**

Run: `cargo test --manifest-path src-tauri/Cargo.toml scheduler`

Expected: compile failure because `scheduler` module and `SchedulerState` do not exist.

- [ ] **Step 3: Implement scheduler module**

Create `src-tauri/src/scheduler.rs` with:

- `ScheduleStatus { enabled, message }`
- `SchedulerState` storing current cancellation sender.
- `configure(config)` starts a desktop thread only when `config.schedule.enabled`.
- `cancel()` stops the current thread.
- On desktop, the thread waits for the interval and calls `manual_update_with_config`.
- On Android, exported Rust command forwards to `platform::PlatformWallpaper`.

- [ ] **Step 4: Wire Tauri commands**

Modify `src-tauri/src/lib.rs`:

- `mod scheduler;`
- manage `SchedulerState` in `run()`.
- add commands `configure_schedule`, `cancel_schedule`, `schedule_status`.
- extract manual update internals into reusable async functions callable by scheduler.

- [ ] **Step 5: Run GREEN**

Run: `cargo test --manifest-path src-tauri/Cargo.toml scheduler`

Expected: scheduler tests pass.

### Task 2: Linux 托盘常驻

**Files:**
- Modify: `src-tauri/Cargo.toml`
- Modify: `src-tauri/src/lib.rs`
- Create: `src-tauri/src/tray.rs`

- [ ] **Step 1: Enable tray feature**

Change Tauri dependency to include tray support:

```toml
tauri = { version = "2", features = ["tray-icon"] }
```

- [ ] **Step 2: Add tray setup**

Create `src-tauri/src/tray.rs` with desktop-only function `setup_tray(app)`:

- menu items: `show`, `update_now`, `toggle_schedule`, `quit`.
- close requested hides the main window instead of exiting.
- quit menu item sets an app flag and exits.

- [ ] **Step 3: Wire setup**

Call tray setup inside `tauri::Builder::setup`.

- [ ] **Step 4: Verify compile**

Run: `cargo check --manifest-path src-tauri/Cargo.toml`

Expected: build succeeds.

### Task 3: 前端移除页面 timer

**Files:**
- Modify: `src/main.js`

- [ ] **Step 1: Remove timer state**

Remove `timerId` from `state`.

- [ ] **Step 2: Replace `restartSchedule`**

Implement:

```js
async function syncSchedule() {
  if (state.config.schedule.enabled) {
    const status = await invoke("configure_schedule", { config: state.config });
    setStatus(status.message);
  } else {
    const status = await invoke("cancel_schedule");
    setStatus(status.message);
  }
}
```

- [ ] **Step 3: Update call sites**

On startup and settings changes call `syncSchedule()` instead of `restartSchedule()`.

- [ ] **Step 4: Verify JavaScript**

Run: `node --check src/main.js`

Expected: no syntax errors.

### Task 4: Android WorkManager

**Files:**
- Modify: `src-tauri/gen/android/app/build.gradle.kts`
- Modify: `src-tauri/gen/android/app/src/main/AndroidManifest.xml`
- Modify: `src-tauri/gen/android/app/src/main/java/com/liut/wallpaper/platform/WallpaperPlatformPlugin.kt`
- Create: `src-tauri/gen/android/app/src/main/java/com/liut/wallpaper/platform/ScheduledWallpaperWorker.kt`
- Test: `src-tauri/gen/android/app/src/test/java/com/liut/wallpaper/platform/WallpaperPlatformPluginTest.kt`

- [ ] **Step 1: Add dependency**

Add:

```kotlin
implementation("androidx.work:work-runtime-ktx:2.10.0")
```

- [ ] **Step 2: Add command args**

Add `ConfigureScheduleArgs` with schedule fields and serialized config JSON/base fields needed by Worker.

- [ ] **Step 3: Add plugin commands**

Add `configureSchedule` and `cancelSchedule` commands:

- save config to SharedPreferences.
- register `PeriodicWorkRequestBuilder<ScheduledWallpaperWorker>(interval, TimeUnit.MINUTES)`.
- use unique work name `wallpaper-client-schedule`.
- cancel by unique work name.

- [ ] **Step 4: Add Worker**

Worker reads SharedPreferences, fetches Bing archive, downloads first image, and calls shared Android wallpaper helper code.

- [ ] **Step 5: Add unit tests**

Add pure tests for interval coercion and unique work name constants.

- [ ] **Step 6: Run Android unit tests**

Run:

```bash
cd src-tauri/gen/android
./gradlew :app:testUniversalDebugUnitTest --tests com.liut.wallpaper.platform.WallpaperPlatformPluginTest
```

Expected: tests pass.

### Task 5: Full verification

**Files:**
- All modified files.

- [ ] **Step 1: Rust core tests**

Run: `cargo test -p wallpaper-core`

Expected: pass.

- [ ] **Step 2: Tauri Rust check**

Run: `cargo check --manifest-path src-tauri/Cargo.toml`

Expected: pass.

- [ ] **Step 3: JavaScript check**

Run: `node --check src/main.js`

Expected: pass.

- [ ] **Step 4: Android unit test**

Run Android unit test command from Task 4.

Expected: pass.

- [ ] **Step 5: Manual Linux smoke test**

Run: `npm run tauri dev`

Expected:

- closing window hides it to tray.
- tray show restores it.
- enabling schedule returns an enabled status.
- tray quit exits the process.
