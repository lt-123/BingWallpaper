package com.liut.wallpaper.platform

import android.app.Activity
import android.app.WallpaperManager
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@InvokeArg
class SaveAndApplyWallpaperArgs {
  lateinit var fileName: String
  lateinit var mimeType: String
  lateinit var imageBase64: String
  lateinit var fitMode: String
  var setLockScreen: Boolean = false
  var saveToGallery: Boolean = true
  var showToast: Boolean = true
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
  @Command
  fun saveAndApplyWallpaper(invoke: Invoke) {
    try {
      val args = invoke.parseArgs(SaveAndApplyWallpaperArgs::class.java)
      val imageBytes = Base64.decode(args.imageBase64, Base64.DEFAULT)
      val uri = if (args.saveToGallery) {
        saveToPictures(args.fileName, args.mimeType, imageBytes).toString()
      } else {
        ""
      }
      val targets = WallpaperTargets.from(args.setLockScreen)
      val bitmap = decodeBitmap(imageBytes)
      applyWallpaper(bitmap, WallpaperFitMode.fromWire(args.fitMode), targets)

      val result = JSObject()
      result.put("uri", uri)
      result.put("appliedHomeScreen", targets.home)
      result.put("appliedLockScreen", targets.lock)
      if (args.showToast) {
        activity.runOnUiThread {
          Toast.makeText(activity, "Wallpaper updated", Toast.LENGTH_SHORT).show()
        }
      }
      invoke.resolve(result)
    } catch (ex: Exception) {
      invoke.reject(ex.message ?: ex.toString())
    }
  }

  @Command
  fun clearWallpaper(invoke: Invoke) {
    try {
      WallpaperManager.getInstance(activity).clear()
      val result = JSObject()
      result.put("message", "System wallpaper cleared")
      invoke.resolve(result)
    } catch (ex: Exception) {
      invoke.reject(ex.message ?: ex.toString())
    }
  }

  private fun saveToPictures(fileName: String, mimeType: String, imageBytes: ByteArray): Uri {
    val resolver = activity.contentResolver
    val values = ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
      put(MediaStore.Images.Media.MIME_TYPE, mimeType)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(
          MediaStore.Images.Media.RELATIVE_PATH,
          "${Environment.DIRECTORY_PICTURES}/Wallpaper Client"
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

  private fun decodeBitmap(imageBytes: ByteArray): Bitmap {
    return BitmapFactory.decodeStream(ByteArrayInputStream(imageBytes))
      ?: error("Could not decode wallpaper image")
  }

  private fun applyWallpaper(
    bitmap: Bitmap,
    fitMode: WallpaperFitMode,
    targets: WallpaperTargets
  ) {
    val manager = WallpaperManager.getInstance(activity)
    val targetBitmap = renderForDevice(bitmap, fitMode, manager.desiredMinimumWidth, manager.desiredMinimumHeight)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      manager.setBitmap(targetBitmap, null, true, targets.flags())
    } else {
      manager.setBitmap(targetBitmap)
    }
  }

  private fun renderForDevice(
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
