package com.liut.wallpaper.platform

import android.app.WallpaperManager
import org.junit.Assert.assertEquals
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
  fun fitModeParsesWireValues() {
    assertEquals(WallpaperFitMode.FILL, WallpaperFitMode.fromWire("Fill"))
    assertEquals(WallpaperFitMode.FIT, WallpaperFitMode.fromWire("Fit"))
    assertEquals(WallpaperFitMode.STRETCH, WallpaperFitMode.fromWire("Stretch"))
    assertEquals(WallpaperFitMode.CENTER, WallpaperFitMode.fromWire("Center"))
  }

  @Test
  fun workManagerIntervalIsClampedToPlatformMinimum() {
    assertEquals(15L, normalizedWorkIntervalMinutes(1))
    assertEquals(15L, normalizedWorkIntervalMinutes(15))
    assertEquals(60L, normalizedWorkIntervalMinutes(60))
  }

  @Test
  fun scheduledWorkUsesStableUniqueName() {
    assertEquals("wallpaper-client-schedule", SCHEDULE_WORK_NAME)
  }

  @Test
  fun bingArchiveUrlUsesMarketAndUhdParameters() {
    val url = bingArchiveUrl("China", "Uhd4k", 20)

    assertEquals(
      "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=zh-CN&uhd=1&uhdwidth=3840&uhdheight=2160",
      url
    )
  }
}
