package xyz.liut.wallora.platform

import android.content.Context

/**
 * Android 平台薄接口层，供 Rust JNI 通过 call_static_method 回调。
 * 每个方法只做一件事，不含业务决策逻辑。
 */
object PlatformApis {

    /** 将图片存入系统相册（Wallora 目录）。 */
    @JvmStatic
    fun saveToGallery(context: Context, fileName: String, imageBytes: ByteArray) {
        saveToPictures(context, fileName, "image/jpeg", imageBytes)
    }

    /** 将图片设置为系统壁纸。 */
    @JvmStatic
    fun setWallpaper(
        context: Context,
        imageBytes: ByteArray,
        fitMode: String,
        setLockScreen: Boolean
    ) {
        val bitmap = decodeBitmap(imageBytes)
        applyWallpaper(
            context,
            bitmap,
            WallpaperFitMode.fromWire(fitMode),
            WallpaperTargets.from(setLockScreen)
        )
    }

    /** 发送系统通知。 */
    @JvmStatic
    fun postNotification(context: Context, title: String, message: String, notificationId: Int) {
        postWallpaperNotification(context, title, message, notificationId)
    }
}
