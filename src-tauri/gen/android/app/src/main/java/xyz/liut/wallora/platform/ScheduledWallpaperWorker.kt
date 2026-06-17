package xyz.liut.wallora.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.liut.wallora.R

const val NOTIFICATION_CHANNEL_ID = "wallora-updates"
const val NOTIFICATION_CHANNEL_NAME = "壁纸更新"
private const val FOREGROUND_NOTIFICATION_ID = 1001

/** 确保通知渠道已创建（Android 8+ 必须），可从任意 Context 调用，重复调用无副作用。 */
fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { setShowBadge(false) }
            manager.createNotificationChannel(channel)
        }
    }
}

/** 发送壁纸结果通知，由 PlatformApis.postNotification 调用（通知 ID 由 Rust 传入）。 */
fun postWallpaperNotification(context: Context, title: String, message: String, notificationId: Int) {
    ensureNotificationChannel(context)
    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_qs_wallpaper)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(notificationId, notification)
}

/**
 * 定时壁纸更新 Worker。
 * 业务逻辑（Bing 获取、下载重试、DailyAt 时间窗口、存图、通知策略）完全由 Rust 实现。
 * 此类仅负责：前台进度通知（WorkManager 前台服务）+ 调用 Rust + 返回结果。
 */
class ScheduledWallpaperWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(SCHEDULE_PREFS_NAME, Context.MODE_PRIVATE)
        val configJson = prefs.getString(PREF_CONFIG_JSON, null) ?: return Result.failure()

        setForeground(createForegroundInfo("正在更新壁纸…"))

        return withContext(Dispatchers.IO) {
            val success = RustCore.fetchAndApplyLatestWallpaper(applicationContext, configJson)
            if (success) Result.success() else Result.retry()
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        ensureNotificationChannel(applicationContext)
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Wallora")
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_qs_wallpaper)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }
}
