package xyz.liut.bingwallpaper.v3.wallpaper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

/**
 * 按目标屏幕尺寸装配壁纸图片。
 */
public final class WallpaperBitmapTransformer {

    private WallpaperBitmapTransformer() {
    }

    public static Bitmap transform(Bitmap source, int targetWidth, int targetHeight, WallpaperFitMode mode) {
        if (source == null) {
            throw new IllegalArgumentException("source bitmap is null");
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("target size must be positive");
        }
        WallpaperFitMode fitMode = mode == null ? WallpaperFitMode.SYSTEM : mode;
        if (fitMode == WallpaperFitMode.STRETCH) {
            return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
        }
        if (fitMode == WallpaperFitMode.CONTAIN) {
            return contain(source, targetWidth, targetHeight);
        }
        return crop(source, targetWidth, targetHeight);
    }

    private static Bitmap crop(Bitmap source, int targetWidth, int targetHeight) {
        float scale = Math.max(
                targetWidth / (float) source.getWidth(),
                targetHeight / (float) source.getHeight());
        int scaledWidth = Math.max(targetWidth, Math.round(source.getWidth() * scale));
        int scaledHeight = Math.max(targetHeight, Math.round(source.getHeight() * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, false);
        int left = (scaledWidth - targetWidth) / 2;
        int top = (scaledHeight - targetHeight) / 2;
        return Bitmap.createBitmap(scaled, left, top, targetWidth, targetHeight);
    }

    private static Bitmap contain(Bitmap source, int targetWidth, int targetHeight) {
        float scale = Math.min(
                targetWidth / (float) source.getWidth(),
                targetHeight / (float) source.getHeight());
        int scaledWidth = Math.max(1, Math.round(source.getWidth() * scale));
        int scaledHeight = Math.max(1, Math.round(source.getHeight() * scale));
        int left = (targetWidth - scaledWidth) / 2;
        int top = (targetHeight - scaledHeight) / 2;
        Bitmap scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, false);
        Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        result.eraseColor(Color.BLACK);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(scaled, left, top, null);
        return result;
    }
}
