package xyz.liut.bingwallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.io.File;

import xyz.liut.bingwallpaper.bean.SourceBean;
import xyz.liut.bingwallpaper.engine.DirectEngine;
import xyz.liut.bingwallpaper.engine.IWallpaperEngine;
import xyz.liut.bingwallpaper.utils.ToastUtil;

/**
 * 添加源
 * <p>
 * Create by liut on 20-11-5
 */
public class AddSourceActivity extends Activity {

    private static final String TAG = "AddSourceActivity";

    private ImageView ivWallpaper;

    private boolean detectedResult;

    private ProgressDialog progressDialog;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_source);

        EditText etName = findViewById(R.id.et_name);
        EditText etUrl = findViewById(R.id.et_url);
        EditText etDesc = findViewById(R.id.et_desc);

        ivWallpaper = findViewById(R.id.iv_wallpaper);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("检测中");
        progressDialog.setCancelable(false);

        // 检测
        findViewById(R.id.bt_test).setOnClickListener(v -> {
            String url = etUrl.getText().toString();
            detectUrl(url);
        });

        // 添加
        findViewById(R.id.bt_add).setOnClickListener(v -> {
            if (detectedResult) {
                String name = etName.getText().toString();
                String url = etUrl.getText().toString();
                String desc = etDesc.getText().toString();
                addSource(name, url, desc);
            } else {
                ToastUtil.showToast(AddSourceActivity.this, "请先通过检测");
            }
        });

        if (BuildConfig.DEBUG) {
            etName.setText("测试");
            etUrl.setText("https://picsum.photos/4096");
            etDesc.setText("测试源");
        }
    }

    // 检测
    private void detectUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            ToastUtil.showToast(AddSourceActivity.this, "url 为空");
            return;
        }
        progressDialog.show();
        new Thread(() -> {
            DirectEngine engine = new DirectEngine("addSource", url, getExternalCacheDir().toString());
            engine.downLoadWallpaper(new IWallpaperEngine.Callback() {
                @Override
                public void onSucceed(File file) {

                    Bitmap bitmap;
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(file.toString(), options);
                        options.inJustDecodeBounds = false;
                        Log.d(TAG, "options.outHeight = " + options.outHeight);
                        Log.d(TAG, "options.outWidth = " + options.outWidth);
                        options.inSampleSize = calculateInSampleSize(options.outHeight, options.outWidth);
                        Log.d(TAG, "options.inSampleSize = " + options.inSampleSize);
                        options.inPreferredConfig = Bitmap.Config.RGB_565;
                        bitmap = BitmapFactory.decodeFile(file.toString(), options);
                    } catch (Exception e) {
                        e.printStackTrace();

                        ToastUtil.showToast(AddSourceActivity.this, "检测失败： " + e.getMessage());
                        return;
                    }


                    Bitmap finalBitmap = bitmap;
                    runOnUiThread(() -> {
                        if (finalBitmap != null) {
                            Log.d(TAG, "finalBitmap.Width " + finalBitmap.getWidth());
                            Log.d(TAG, "finalBitmap.Height " + finalBitmap.getHeight());
                            ivWallpaper.setImageBitmap(finalBitmap);
                            detectedResult = true;
                            ToastUtil.showToast(AddSourceActivity.this, "检测成功");
                        } else {
                            ToastUtil.showToast(AddSourceActivity.this, "检测失败");
                        }
                    });

                    progressDialog.dismiss();
                }

                @Override
                public void onMessage(String msg) {
                    ToastUtil.showToast(AddSourceActivity.this, msg);
                }

                @Override
                public void onFailed(Exception e) {
                    ToastUtil.showToast(AddSourceActivity.this, e.getMessage());
                    detectedResult = false;

                    progressDialog.dismiss();
                }

                private int calculateInSampleSize(int w, int h) {
                    int ivWidth = ivWallpaper.getWidth();
                    int ivHeight = ivWallpaper.getHeight();

                    Log.d(TAG, "ivWidth " + ivWidth);
                    Log.d(TAG, "ivHeight " + ivHeight);
                    return Math.max(h / ivHeight, w / ivWidth);
                }

            });
        }).start();
    }


    // 添加
    private void addSource(String name, String url, String desc) {
        if (TextUtils.isEmpty(name)) {
            ToastUtil.showToast(AddSourceActivity.this, "请输入 name");
            return;
        }
        if (TextUtils.isEmpty(url)) {
            ToastUtil.showToast(AddSourceActivity.this, "请输入 url");
            return;
        }

        SourceManager.add(this, new SourceBean(name, url, desc, false));

        finish();
    }

}
