package xyz.liut.wallora.platform

import android.content.Context

/**
 * Rust 业务逻辑入口，通过 JNI 桥接到 wallora_lib.so 中的 Rust 实现。
 * 壁纸下载、重试、DailyAt 时间窗口、保存决策、通知均在 Rust 侧完成。
 */
object RustCore {
    init {
        // wallora_lib 由 TauriActivity 在有 UI 时加载；
        // Worker/Tile 在无 UI 进程中首次访问此对象时，由此 init 块确保加载。
        // System.loadLibrary 幂等，重复调用无副作用。
        System.loadLibrary("wallora_lib")
    }

    /**
     * 初始化：存储 JavaVM 和 Application Context，供 Tauri 异步命令路径使用。
     * 由 WallpaperPlatformPlugin 在构建时调用一次。
     */
    external fun initialize(appContext: Context)

    /**
     * 获取最新 Bing 壁纸并应用（供 Worker 和 QS 瓷砖调用）。
     * 包含 DailyAt 时间窗口检查、下载重试（3 次）、存相册、设置壁纸、结果通知。
     *
     * @return true = 成功；false = 失败（Worker 应返回 Result.retry()）
     */
    external fun fetchAndApplyLatestWallpaper(context: Context, configJson: String): Boolean
}
