package xyz.liut.bingwallpaper.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

/**
 * bitmap 操作工具
 * <p>
 * Create by liut on 20-12-18
 */
public class AdjustBitmap {

    private static final String TAG = "AdjustBitmap";

    /**
     * 从文件读取 bm， 并缩放， 缩放结果不会小于需求
     *
     * @param reqWidth  需求 宽
     * @param reqHeight 需求 高
     * @return bm
     */
    @Nullable
    public static Bitmap decodeSampledBitmapFromFile(File file, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.toString(), options);
            options.inJustDecodeBounds = false;
            Log.d(TAG, "options.outHeight = " + options.outHeight);
            Log.d(TAG, "options.outWidth = " + options.outWidth);

            return decodeSampledBitmapFromFile(file, reqWidth, reqHeight, options.outWidth, options.outHeight);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从文件读取 bm， 并缩放， 缩放结果不会小于需求
     *
     * @param reqWidth  需求 宽
     * @param reqHeight 需求 高
     * @param width     实际 宽
     * @param height    实际 高
     * @return bm
     */
    @Nullable
    public static Bitmap decodeSampledBitmapFromFile(File file, int reqWidth, int reqHeight, int width, int height) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // 缩放
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = (int) ((float) height / (float) reqHeight);
            final int widthRatio = (int) ((float) width / (float) reqWidth);
            inSampleSize = Math.min(heightRatio, widthRatio);
        }

        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getPath(), options);
    }

    /**
     * 将bm调整到指定高度
     *
     * @param origin    src
     * @param newHeight h
     */
    public static Bitmap sizeBitmapByHeight(@NonNull Bitmap origin, int newHeight) {
        int height = origin.getHeight();
        if (height == newHeight) {
            return origin;
        }
        float scaleHeight = ((float) newHeight) / height;
        return scaleBitmap(origin, scaleHeight, scaleHeight);
    }

    /**
     * 将bitmap调整到指定大小
     */
    public static Bitmap sizeBitmap(@NonNull Bitmap origin, int newWidth, int newHeight) {
        int height = origin.getHeight();
        int width = origin.getWidth();
        if (height == newHeight && width == newWidth) {
            return origin;
        }
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        return scaleBitmap(origin, scaleWidth, scaleHeight);
    }

    /**
     * 按比例缩放
     */
    public static Bitmap scaleBitmap(Bitmap origin, float scaleW, float scaleH) {
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(scaleW, scaleH);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    /**
     * 裁剪
     */
    public static Bitmap cropBitmapWidth(Bitmap bitmap, int reqWidth) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Bitmap newBM = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - reqWidth) / 2, 0, reqWidth, h);
        if (!newBM.equals(bitmap)) {
            bitmap.recycle();
        }
        return newBM;
    }


}
