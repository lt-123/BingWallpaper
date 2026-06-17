package xyz.liut.wallora.platform

import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import xyz.liut.wallora.R

/**
 * 快速设置瓷砖：在通知栏下拉面板点击可立即触发一次壁纸更新。
 * 依赖 SharedPreferences 中已保存的调度配置（configJson），若未配置则不触发。
 *
 * 单击：立即触发一次 OneTimeWork 更新壁纸。
 * 长按：系统自动调起在 manifest 中注册了 QS_TILE_PREFERENCES intent-filter 的 MainActivity。
 */
@RequiresApi(Build.VERSION_CODES.N)
class WallpaperTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        refreshTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (!hasScheduleConfig()) return

        val request = OneTimeWorkRequestBuilder<ScheduledWallpaperWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(request)

        // 触发后保持 ACTIVE 状态，onStartListening 再次进入时会重新校验
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    private fun refreshTile() {
        qsTile?.apply {
            icon = Icon.createWithResource(applicationContext, R.drawable.ic_qs_wallpaper)
            label = getString(R.string.app_name)
            state = if (hasScheduleConfig()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }

    private fun hasScheduleConfig(): Boolean {
        val prefs = getSharedPreferences(SCHEDULE_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_CONFIG_JSON, null) != null
    }
}
