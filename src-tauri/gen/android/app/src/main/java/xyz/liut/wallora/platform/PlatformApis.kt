package xyz.liut.wallora.platform

import android.content.Context

/**
 * Android 平台薄接口层，供 Rust JNI 通过 call_static_method 回调。
 * 每个方法只做一件事，不含业务决策逻辑。
 */
object PlatformApis {

    /** 将本地临时图片存入系统相册（Wallora 目录），若同名图片已存在则复用。 */
    @JvmStatic
    fun saveToGallery(context: Context, fileName: String, localPath: String) {
        saveToPictures(context, fileName, "image/jpeg", localPath)
    }

    /** 从本地临时图片路径读取内容并设置为系统壁纸。 */
    @JvmStatic
    fun setWallpaper(context: Context, localPath: String, setLockScreen: Boolean) {
        applyWallpaper(context, localPath, WallpaperTargets.from(setLockScreen))
    }

    /** 发送系统通知。 */
    @JvmStatic
    fun postNotification(context: Context, title: String, message: String, notificationId: Int) {
        postWallpaperNotification(context, title, message, notificationId)
    }
}
