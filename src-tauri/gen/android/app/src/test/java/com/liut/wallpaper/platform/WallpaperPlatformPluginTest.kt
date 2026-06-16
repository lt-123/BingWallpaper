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
}
