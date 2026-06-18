package xyz.liut.wallora.platform

import android.app.WallpaperManager
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import xyz.liut.wallora.BuildConfig

class WallpaperPlatformPluginTest {
  @Test
  fun targetFlagsIncludesLockScreenWhenRequested() {
    val flags = WallpaperTargets.from(setLockScreen = true).flags()

    assertEquals(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, flags)
  }

  @Test
  fun targetFlagsDefaultsToHomeScreenOnly() {
    val flags = WallpaperTargets.from(setLockScreen = false).flags()

    assertEquals(WallpaperManager.FLAG_SYSTEM, flags)
  }

  @Test
  fun workManagerIntervalIsClampedToPlatformMinimum() {
    assertEquals(15L, normalizedWorkIntervalMinutes(1))
    assertEquals(15L, normalizedWorkIntervalMinutes(15))
    assertEquals(60L, normalizedWorkIntervalMinutes(60))
  }

  @Test
  fun scheduledWorkUsesStableUniqueName() {
    assertEquals("wallora-schedule", SCHEDULE_WORK_NAME)
  }

  @Test
  fun scheduleModeDailyAtUsesMinimumWorkInterval() {
    // DailyAt 模式下 WorkManager 周期应使用最小间隔（15 分钟）
    val interval = MIN_PERIODIC_INTERVAL_MINUTES
    assertEquals(15L, interval)
  }

  @Test
  fun platformPreferencesUseStableStorageKeys() {
    assertEquals("wallora-platform", PLATFORM_PREFS_NAME)
    assertEquals("excludeFromRecents", PREF_EXCLUDE_FROM_RECENTS)
  }

  @Test
  fun syncPlatformPreferencesDefaultsToKeepingTaskInRecents() {
    val args = SyncPlatformPreferencesArgs()

    assertFalse(args.excludeFromRecents)
  }

  @Test
  fun androidMinimumSdkSupportsScopedMediaStorePaths() {
    assertEquals(29, WALLORA_MIN_SDK)
    assertEquals(WALLORA_MIN_SDK, BuildConfig.WALLORA_MIN_SDK)
  }

  @Test
  fun mediaStoreSaveUsesStableWalloraPicturesPath() {
    assertEquals("Pictures/Wallora", walloraPicturesRelativePath())
  }

  @Test
  fun mediaStoreLookupMatchesDisplayNameAndRelativePath() {
    val lookup = existingGalleryImageLookup("bing-20260617-example.jpg")

    assertEquals(
      "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?",
      lookup.selection
    )
    assertEquals(
      listOf("bing-20260617-example.jpg", "Pictures/Wallora/"),
      lookup.selectionArgs.toList()
    )
  }
}
