package xyz.liut.wallora.platform

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.WindowManager
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

/** WorkManager 唯一任务名，用于 enqueue 和 cancel 时定位同一个定时任务。 */
const val SCHEDULE_WORK_NAME = "wallora-schedule"

/** SharedPreferences 文件名，用于在 Worker 和 Plugin 之间持久化调度配置。 */
const val SCHEDULE_PREFS_NAME = "wallora-schedule"

/** SharedPreferences 键：存储序列化的 AppConfig JSON，Worker 启动时读取。 */
const val PREF_CONFIG_JSON = "configJson"

/** SharedPreferences 键：是否在后台更新完成后发送通知。 */
const val PREF_NOTIFY_BACKGROUND = "notifyBackground"

/**
 * WorkManager 周期任务的最小触发间隔（分钟）。
 * Android 系统对 PeriodicWorkRequest 的最小间隔有 15 分钟的硬限制。
 */
const val MIN_PERIODIC_INTERVAL_MINUTES = 15L

/** Rust 侧通过 Tauri IPC 传入的 configureSchedule 参数结构。 */
@InvokeArg
class ConfigureScheduleArgs {
  var enabled: Boolean = false
  var intervalMinutes: Long = MIN_PERIODIC_INTERVAL_MINUTES
  var notifyOnBackgroundUpdate: Boolean = true
  lateinit var configJson: String
  var scheduleMode: String = "Interval"
}

/**
 * 将用户配置的间隔分钟数规范化到系统允许的最小值。
 * WorkManager 不接受小于 15 分钟的周期，传入更小的值会被静默抬升。
 */
fun normalizedWorkIntervalMinutes(intervalMinutes: Long): Long {
  return intervalMinutes.coerceAtLeast(MIN_PERIODIC_INTERVAL_MINUTES)
}

/**
 * 壁纸写入目标：主屏幕和/或锁屏。
 *
 * [flags] 将字段组合为 [WallpaperManager] 所需的位标志，
 * 传给 [WallpaperManager.setBitmap] 或 [WallpaperManager.setStream] 的 `which` 参数。
 */
data class WallpaperTargets(val home: Boolean, val lock: Boolean) {
  /** 将写入目标转换为 WallpaperManager 的 FLAG_SYSTEM / FLAG_LOCK 位标志组合。 */
  fun flags(): Int {
    var flags = 0
    if (home) flags = flags or WallpaperManager.FLAG_SYSTEM
    if (lock) flags = flags or WallpaperManager.FLAG_LOCK
    return flags
  }

  companion object {
    /** 始终写入主屏幕；[setLockScreen] 为 true 时同时写入锁屏。 */
    fun from(setLockScreen: Boolean): WallpaperTargets {
      return WallpaperTargets(home = true, lock = setLockScreen)
    }
  }
}

/**
 * Tauri 平台插件，向 Rust 层暴露壁纸相关的 Android 平台能力。
 *
 * 暴露的命令：
 * - [clearWallpaper]：恢复系统默认壁纸
 * - [configureSchedule]：通过 WorkManager 配置定时自动更新
 * - [cancelSchedule]：取消定时任务
 * - [checkBatteryOptimization]：查询电池优化豁免状态
 * - [requestBatteryExemption]：跳转系统设置申请豁免
 */
@TauriPlugin
class WallpaperPlatformPlugin(private val activity: Activity) : Plugin(activity) {
  init {
    // 存储 JavaVM 和 Application Context，供 Rust 异步命令路径（spawn_blocking）使用。
    RustCore.initialize(activity.applicationContext)
  }

  /**
   * 清除当前系统壁纸，恢复到系统内置默认壁纸。
   *
   * API 28（Android 9）起使用 [WallpaperManager.clearWallpaper]；
   * 更低版本回退到已废弃的 [WallpaperManager.clear]，行为等价。
   */
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

  /**
   * 配置定时壁纸自动更新任务。
   *
   * - [ConfigureScheduleArgs.enabled] 为 false 时取消现有任务并直接返回。
   * - Interval 模式：按 [ConfigureScheduleArgs.intervalMinutes] 触发，
   *   最小值被规范化到 [MIN_PERIODIC_INTERVAL_MINUTES]。
   * - DailyAt 模式：Worker 固定以最小间隔（15 分钟）轮询，由 Rust 侧
   *   在 [RustCore.fetchAndApplyLatestWallpaper] 内判断是否在目标时刻窗口内。
   *
   * 配置通过 SharedPreferences 持久化，Worker 启动时自行读取，
   * 无需在任务参数中传递（避免 WorkManager 数据大小限制）。
   */
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

  /**
   * 查询当前应用是否已被系统豁免电池优化。
   *
   * 未豁免时 WorkManager 的周期任务可能被系统延迟或跳过，
   * 前端根据此结果决定是否显示豁免引导入口。
   * API 23 以下无电池优化机制，始终返回已豁免。
   */
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

  /**
   * 跳转系统设置页面，引导用户为本应用申请电池优化豁免。
   *
   * 优先打开精确的"忽略电池优化"请求对话框；若部分厂商 ROM 未提供该页面，
   * 回退到通用的"电池优化"列表页。
   */
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

  /**
   * 取消 WorkManager 中的定时壁纸更新任务。
   * 通过唯一任务名 [SCHEDULE_WORK_NAME] 定位，幂等操作，无任务时静默返回。
   */
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

/** 构造调度操作的标准 IPC 响应对象。 */
fun scheduleResult(message: String): JSObject {
  val result = JSObject()
  result.put("message", message)
  return result
}

/**
 * 将图片字节写入系统 MediaStore 相册（Pictures/Wallora 目录）。
 *
 * Android Q（API 29）起使用 [MediaStore.Images.Media.IS_PENDING] 机制：
 * 先标记为"写入中"，写完后再置为 0，防止其他应用在写入期间读到不完整文件。
 * 低版本直接写入，无此保护。
 *
 * @param fileName 相册中显示的文件名（含扩展名）
 * @param mimeType 图片 MIME 类型，如 "image/jpeg"
 * @param imageBytes 图片原始字节
 * @return 插入后的 MediaStore URI
 */
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

/**
 * 获取设备的实际物理屏幕分辨率（宽 × 高，单位：像素）。
 *
 * Android R（API 30）起使用 [WindowManager.currentWindowMetrics]；
 * 低版本通过已废弃的 [android.view.Display.getRealMetrics] 获取，
 * 二者均返回包含系统栏在内的完整屏幕尺寸，用于横图的缩放和裁剪基准。
 */
fun screenDimensions(context: Context): Pair<Int, Int> {
  val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val bounds = wm.currentWindowMetrics.bounds
    Pair(bounds.width(), bounds.height())
  } else {
    val dm = DisplayMetrics()
    @Suppress("DEPRECATION")
    wm.defaultDisplay.getRealMetrics(dm)
    Pair(dm.widthPixels, dm.heightPixels)
  }
}

/**
 * 将图片字节设置为系统壁纸，自动按图片方向选择处理策略：
 *
 * - **竖图**（高 > 宽）：直接以流式方式写入，不做任何缩放或裁剪，
 *   由系统自行处理缩放偏移，保留完整图像细节。
 * - **横图**（宽 ≥ 高）：先按屏幕尺寸整数倍下采样减少内存占用，
 *   再等比缩放到屏幕高度，最后居中裁剪到屏幕宽度，
 *   确保横图在竖屏设备上填满屏幕且不变形。
 *
 * @param imageBytes 图片原始字节（JPEG）
 * @param targets 写入目标（主屏幕 / 锁屏 / 两者）
 */
fun applyWallpaper(context: Context, imageBytes: ByteArray, targets: WallpaperTargets) {
  val manager = WallpaperManager.getInstance(context)
  val (screenW, screenH) = screenDimensions(context)

  // 仅解码边界信息，不加载像素，用于判断图片方向
  val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeStream(ByteArrayInputStream(imageBytes), null, boundsOpts)
  val srcW = boundsOpts.outWidth
  val srcH = boundsOpts.outHeight

  if (srcH > srcW) {
    // 竖图：直接流式写入，不做缩放裁剪
    manager.setStream(ByteArrayInputStream(imageBytes), Rect(0, 0, srcW, srcH), true, targets.flags())
  } else {
    // 横图：整数倍下采样 → 按屏幕高缩放 → 居中裁宽
    val bitmap = decodeSampledBitmapFromBytes(imageBytes, screenW, screenH)
    val scaled = sizeBitmapByHeight(bitmap, screenH)
    val cropped = cropBitmapWidth(scaled, screenW)
    manager.setBitmap(cropped, Rect(0, 0, cropped.width, cropped.height), true, targets.flags())
  }
}

/**
 * 从字节数组解码 Bitmap，并以整数倍缩放因子（inSampleSize）下采样，
 * 确保解码结果不小于目标尺寸，同时减少不必要的内存分配。
 *
 * inSampleSize 取宽高两个方向缩放比的较小值，保证缩放后的图像在两个方向
 * 上均不小于目标尺寸，后续的浮点缩放步骤可以安全地放大到精确尺寸。
 *
 * @param reqWidth  目标宽度（像素），通常为屏幕宽
 * @param reqHeight 目标高度（像素），通常为屏幕高
 */
fun decodeSampledBitmapFromBytes(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap {
  val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeStream(ByteArrayInputStream(bytes), null, opts)
  val srcW = opts.outWidth
  val srcH = opts.outHeight

  var inSampleSize = 1
  if (srcH > reqHeight || srcW > reqWidth) {
    val heightRatio = srcH / reqHeight
    val widthRatio = srcW / reqWidth
    // 取两个方向的较小值，保证下采样后的尺寸在两个维度上都不小于目标
    inSampleSize = minOf(heightRatio, widthRatio).coerceAtLeast(1)
  }

  return BitmapFactory.decodeStream(
    ByteArrayInputStream(bytes),
    null,
    BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
  ) ?: error("Could not decode wallpaper image")
}

/**
 * 将 Bitmap 等比缩放到指定高度。
 *
 * 宽高使用相同的缩放比以保持宽高比不变。
 * 缩放后原 Bitmap 被回收（若与结果不是同一对象），调用方不应再持有原引用。
 *
 * @param newHeight 目标高度（像素）
 */
fun sizeBitmapByHeight(origin: Bitmap, newHeight: Int): Bitmap {
  if (origin.height == newHeight) return origin
  val scale = newHeight.toFloat() / origin.height
  val matrix = android.graphics.Matrix().apply { preScale(scale, scale) }
  val result = Bitmap.createBitmap(origin, 0, 0, origin.width, origin.height, matrix, false)
  if (result != origin) origin.recycle()
  return result
}

/**
 * 从 Bitmap 中心居中裁剪出指定宽度，高度保持不变。
 *
 * 若 Bitmap 宽度已不超过目标宽度，直接返回原对象。
 * 裁剪后原 Bitmap 被回收（若与结果不是同一对象），调用方不应再持有原引用。
 *
 * @param reqWidth 目标宽度（像素）
 */
fun cropBitmapWidth(bitmap: Bitmap, reqWidth: Int): Bitmap {
  if (bitmap.width <= reqWidth) return bitmap
  val x = (bitmap.width - reqWidth) / 2
  val result = Bitmap.createBitmap(bitmap, x, 0, reqWidth, bitmap.height)
  if (result != bitmap) bitmap.recycle()
  return result
}
