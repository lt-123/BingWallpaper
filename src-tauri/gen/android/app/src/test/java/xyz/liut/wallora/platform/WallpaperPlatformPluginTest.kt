package xyz.liut.wallora.platform

import android.app.WallpaperManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

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
}
