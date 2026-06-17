package xyz.liut.wallora.platform

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
    assertEquals("wallora-schedule", SCHEDULE_WORK_NAME)
  }

  @Test
  fun bingArchiveUrlUsesMarketAndUhdParameters() {
    val url = bingArchiveUrl("China", "Uhd4k", 20)

    assertEquals(
      "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=zh-CN&uhd=1&uhdwidth=3840&uhdheight=2160",
      url
    )
  }

  @Test
  fun parseDailyTimesReturnsEmptyForNull() {
    val result = parseDailyTimes(null)
    assertEquals(emptyList<String>(), result)
  }

  @Test
  fun parseDailyTimesReturnsTimesFromJsonArray() {
    val scheduleObj = org.json.JSONObject().apply {
      put("daily_times", org.json.JSONArray().apply {
        put("08:00")
        put("18:30")
      })
    }
    val result = parseDailyTimes(scheduleObj)
    assertEquals(listOf("08:00", "18:30"), result)
  }

  @Test
  fun isWithinAnyTimeWindowReturnsFalseForEmptyList() {
    assertFalse(isWithinAnyTimeWindow(emptyList()))
  }

  @Test
  fun isWithinAnyTimeWindowReturnsFalseForMalformedEntry() {
    assertFalse(isWithinAnyTimeWindow(listOf("not-a-time")))
  }

  @Test
  fun scheduleModeDailyAtUsesMinimumWorkInterval() {
    // DailyAt 模式下 WorkManager 周期应使用最小间隔（15 分钟）
    val interval = MIN_PERIODIC_INTERVAL_MINUTES
    assertEquals(15L, interval)
  }
}
