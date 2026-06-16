package xyz.liut.wallpaper.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.net.URL
import org.json.JSONObject

class ScheduledWallpaperWorker(
  context: Context,
  params: WorkerParameters
) : CoroutineWorker(context, params) {
  override suspend fun doWork(): Result {
    return try {
      // 网络请求和文件 I/O 均在 IO 调度器上执行，避免阻塞 Default 线程池
      withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences(SCHEDULE_PREFS_NAME, Context.MODE_PRIVATE)
        val configJson = prefs.getString(PREF_CONFIG_JSON, null) ?: return@withContext Result.failure()
        val config = JSONObject(configJson)
        val wallpaper = fetchLatestWallpaper(config)
        val imageBytes = URL(wallpaper.imageUrl).openStream().readBytes()
        if (config.optBoolean("save_to_file_system", true)) {
          saveToPictures(applicationContext, wallpaper.fileName(), "image/jpeg", imageBytes)
        }
        applyWallpaper(
          applicationContext,
          decodeBitmap(imageBytes),
          WallpaperFitMode.fromWire(config.optString("fit_mode", "Fill")),
          WallpaperTargets.from(config.optJSONObject("platform")?.optBoolean("set_lock_screen", false) ?: false)
        )
        Result.success()
      }
    } catch (ex: Exception) {
      Result.retry()
    }
  }
}

data class ScheduledWallpaper(
  val sourceId: String,
  val title: String,
  val publishedDate: String,
  val imageUrl: String
) {
  fun fileName(): String {
    val safeTitle = title
      .map { ch -> if (ch.isLetterOrDigit() || ch == '-') ch else '_' }
      .joinToString("")
      .trim('_')
    return "$sourceId-$publishedDate-$safeTitle.jpg"
  }
}

fun fetchLatestWallpaper(config: JSONObject): ScheduledWallpaper {
  val bing = config.getJSONObject("bing")
  val archiveUrl = bingArchiveUrl(
    market = bing.optString("market", "UnitedStates"),
    resolution = bing.optString("resolution", "Standard1920x1080"),
    count = bing.optInt("count", 1)
  )
  // 使用 openStream() 替代已废弃的 URL.readText()
  val response = JSONObject(URL(archiveUrl).openStream().bufferedReader().use { it.readText() })
  val image = response.getJSONArray("images").getJSONObject(0)
  val rawImageUrl = image.getString("url")
  val imageUrl = if (rawImageUrl.startsWith("http")) rawImageUrl else "https://www.bing.com$rawImageUrl"
  return ScheduledWallpaper(
    sourceId = "bing",
    title = image.optString("title", "wallpaper"),
    publishedDate = image.optString("startdate", "scheduled"),
    imageUrl = imageUrl
  )
}

fun bingArchiveUrl(market: String, resolution: String, count: Int): String {
  val pairs = mutableListOf(
    "format" to "js",
    "idx" to "0",
    "n" to count.coerceIn(1, 8).toString(),
    "mkt" to bingMarketQueryValue(market)
  )
  if (resolution == "Uhd4k") {
    pairs += "uhd" to "1"
    pairs += "uhdwidth" to "3840"
    pairs += "uhdheight" to "2160"
  }
  val query = pairs.joinToString("&") { (key, value) ->
    "${urlEncode(key)}=${urlEncode(value)}"
  }
  return "https://www.bing.com/HPImageArchive.aspx?$query"
}

fun bingMarketQueryValue(market: String): String {
  return when (market) {
    "China" -> "zh-CN"
    "Japan" -> "ja-JP"
    "UnitedKingdom" -> "en-GB"
    "Germany" -> "de-DE"
    "France" -> "fr-FR"
    else -> "en-US"
  }
}

private fun urlEncode(value: String): String {
  return URLEncoder.encode(value, "UTF-8")
}
