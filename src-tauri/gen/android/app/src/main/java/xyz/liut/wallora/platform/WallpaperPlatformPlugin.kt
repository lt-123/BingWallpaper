package xyz.liut.wallora.platform

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

const val SCHEDULE_WORK_NAME = "wallora-schedule"
const val SCHEDULE_PREFS_NAME = "wallora-schedule"
const val PREF_CONFIG_JSON = "configJson"
const val PREF_NOTIFY_BACKGROUND = "notifyBackground"
const val MIN_PERIODIC_INTERVAL_MINUTES = 15L

@InvokeArg
class ConfigureScheduleArgs {
  var enabled: Boolean = false
  var intervalMinutes: Long = MIN_PERIODIC_INTERVAL_MINUTES
  var notifyOnBackgroundUpdate: Boolean = true
  lateinit var configJson: String
  var scheduleMode: String = "Interval"
}

fun normalizedWorkIntervalMinutes(intervalMinutes: Long): Long {
  return intervalMinutes.coerceAtLeast(MIN_PERIODIC_INTERVAL_MINUTES)
}

enum class WallpaperFitMode {
  FILL,
  FIT,
  STRETCH,
  CENTER;

  companion object {
    fun fromWire(value: String): WallpaperFitMode {
      return when (value) {
        "Fill" -> FILL
        "Fit" -> FIT
        "Stretch" -> STRETCH
        "Center" -> CENTER
        else -> FILL
      }
    }
  }
}

data class WallpaperTargets(val home: Boolean, val lock: Boolean) {
  fun flags(): Int {
    var flags = 0
    if (home) flags = flags or WallpaperManager.FLAG_SYSTEM
    if (lock) flags = flags or WallpaperManager.FLAG_LOCK
    return flags
  }

  companion object {
    fun from(setLockScreen: Boolean): WallpaperTargets {
      return WallpaperTargets(home = true, lock = setLockScreen)
    }
  }
}

@TauriPlugin
class WallpaperPlatformPlugin(private val activity: Activity) : Plugin(activity) {
  init {
    // 存储 JavaVM 和 Application Context，供 Rust 异步命令路径（spawn_blocking）使用。
    RustCore.initialize(activity.applicationContext)
  }

  @Command
  fun clearWallpaper(invoke: Invoke) {
    try {
      val manager = WallpaperManager.getInstance(activity)
      // clearWallpaper(flags) 在 API 28 引入；API 24-27 回退到已废弃的 clear()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        manager.clearWallpaper()
      } else {
        @Suppress("DEPRECATION")
        manager.clear()
      }
      val result = JSObject()
      result.put("message", "System wallpaper cleared")
      invoke.resolve(result)
    } catch (ex: Exception) {
      invoke.reject(ex.message ?: ex.toString())
    }
  }

  @Command
  fun configureSchedule(invoke: Invoke) {
    try {
      val args = invoke.parseArgs(ConfigureScheduleArgs::class.java)
      if (!args.enabled) {
        cancelScheduledWork()
        invoke.resolve(scheduleResult("Automatic updates disabled"))
        return
      }

      activity
        .getSharedPreferences(SCHEDULE_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PREF_CONFIG_JSON, args.configJson)
        .putBoolean(PREF_NOTIFY_BACKGROUND, args.notifyOnBackgroundUpdate)
        .apply()

      // DailyAt 模式：Worker 内部按时间窗口决定是否执行，周期固定为最小值（15 分钟）
      val workIntervalMinutes = if (args.scheduleMode == "DailyAt") {
        MIN_PERIODIC_INTERVAL_MINUTES
      } else {
        normalizedWorkIntervalMinutes(args.intervalMinutes)
      }

      val request = PeriodicWorkRequestBuilder<ScheduledWallpaperWorker>(
        workIntervalMinutes,
        TimeUnit.MINUTES
      ).build()

      WorkManager.getInstance(activity).enqueueUniquePeriodicWork(
        SCHEDULE_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
      )

      val msg = if (args.scheduleMode == "DailyAt") {
        "Automatic updates enabled (daily time-based)"
      } else {
        "Automatic updates enabled every $workIntervalMinutes minute(s)"
      }
      invoke.resolve(scheduleResult(msg))
    } catch (ex: Exception) {
      invoke.reject(ex.message ?: ex.toString())
    }
  }

  @Command
  fun checkBatteryOptimization(invoke: Invoke) {
    val exempted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
      pm.isIgnoringBatteryOptimizations(activity.packageName)
    } else {
      true
    }
    val result = JSObject()
    result.put("exempted", exempted)
    invoke.resolve(result)
  }

  @Command
  fun requestBatteryExemption(invoke: Invoke) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
          data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
      } catch (ex: Exception) {
        // 部分厂商 ROM 可能没有该 Activity，回退到通用电池设置页
        activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
      }
    }
    invoke.resolve(JSObject())
  }

  @Command
  fun cancelSchedule(invoke: Invoke) {
    try {
      cancelScheduledWork()
      invoke.resolve(scheduleResult("Automatic updates disabled"))
    } catch (ex: Exception) {
      invoke.reject(ex.message ?: ex.toString())
    }
  }

  private fun cancelScheduledWork() {
    WorkManager.getInstance(activity).cancelUniqueWork(SCHEDULE_WORK_NAME)
  }
}

fun scheduleResult(message: String): JSObject {
  val result = JSObject()
  result.put("message", message)
  return result
}

fun saveToPictures(context: Context, fileName: String, mimeType: String, imageBytes: ByteArray): Uri {
  val resolver = context.contentResolver
  val values = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      put(
        MediaStore.Images.Media.RELATIVE_PATH,
        "${Environment.DIRECTORY_PICTURES}/Wallora"
      )
      put(MediaStore.Images.Media.IS_PENDING, 1)
    }
  }

  val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    ?: error("Could not create MediaStore image entry")
  resolver.openOutputStream(uri)?.use { output -> output.write(imageBytes) }
    ?: error("Could not open MediaStore output stream")

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val finishedValues = ContentValues().apply {
      put(MediaStore.Images.Media.IS_PENDING, 0)
    }
    resolver.update(uri, finishedValues, null, null)
  }

  return uri
}

fun decodeBitmap(imageBytes: ByteArray): Bitmap {
  return BitmapFactory.decodeStream(ByteArrayInputStream(imageBytes))
    ?: error("Could not decode wallpaper image")
}

fun applyWallpaper(
  context: Context,
  bitmap: Bitmap,
  fitMode: WallpaperFitMode,
  targets: WallpaperTargets
) {
  val manager = WallpaperManager.getInstance(context)
  val targetBitmap = renderForDevice(bitmap, fitMode, manager.desiredMinimumWidth, manager.desiredMinimumHeight)
  // minSdk=24 即 API N，setBitmap(Bitmap) 的废弃版本在此 minSdk 下永远不会执行，直接调用带 flags 的版本
  manager.setBitmap(targetBitmap, null, true, targets.flags())
}

fun renderForDevice(
  bitmap: Bitmap,
  fitMode: WallpaperFitMode,
  desiredWidth: Int,
  desiredHeight: Int
): Bitmap {
  val width = if (desiredWidth > 0) desiredWidth else bitmap.width
  val height = if (desiredHeight > 0) desiredHeight else bitmap.height
  if (width == bitmap.width && height == bitmap.height) return bitmap

  val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(output)
  val target = destinationRect(bitmap.width, bitmap.height, width, height, fitMode)
  canvas.drawBitmap(bitmap, null, target, null)
  return output
}

fun destinationRect(
  sourceWidth: Int,
  sourceHeight: Int,
  targetWidth: Int,
  targetHeight: Int,
  fitMode: WallpaperFitMode
): Rect {
  if (fitMode == WallpaperFitMode.STRETCH) {
    return Rect(0, 0, targetWidth, targetHeight)
  }

  if (fitMode == WallpaperFitMode.CENTER) {
    val left = (targetWidth - sourceWidth) / 2
    val top = (targetHeight - sourceHeight) / 2
    return Rect(left, top, left + sourceWidth, top + sourceHeight)
  }

  val sourceRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
  val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
  val scale = if (fitMode == WallpaperFitMode.FILL) {
    if (sourceRatio > targetRatio) targetHeight.toFloat() / sourceHeight else targetWidth.toFloat() / sourceWidth
  } else {
    if (sourceRatio > targetRatio) targetWidth.toFloat() / sourceWidth else targetHeight.toFloat() / sourceHeight
  }
  val scaledWidth = (sourceWidth * scale).toInt()
  val scaledHeight = (sourceHeight * scale).toInt()
  val left = (targetWidth - scaledWidth) / 2
  val top = (targetHeight - scaledHeight) / 2
  return Rect(left, top, left + scaledWidth, top + scaledHeight)
}
