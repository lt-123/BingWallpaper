package xyz.liut.bingwallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.io.File;

import xyz.liut.bingwallpaper.bean.SourceBean;
import xyz.liut.bingwallpaper.engine.DirectEngine;
import xyz.liut.bingwallpaper.engine.IWallpaperEngine;
import xyz.liut.bingwallpaper.engine.TimeFileNameFormat;
import xyz.liut.bingwallpaper.utils.ToastUtil;

/**
 * 添加源
 * <p>
 * Create by liut on 20-11-5
 */
public class AddSourceActivity extends Activity {

    private ImageView ivWallpaper;

    private boolean detectedResult;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_source);

        EditText etName = findViewById(R.id.et_name);
        EditText etUrl = findViewById(R.id.et_url);
        EditText etDesc = findViewById(R.id.et_desc);

        ivWallpaper = findViewById(R.id.iv_wallpaper);

        if (BuildConfig.DEBUG) {
            etUrl.setText("https://picsum.photos/1080");
        }

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
    }

    // 检测
    private void detectUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            ToastUtil.showToast(AddSourceActivity.this, "url 为空");
            return;
        }
        ToastUtil.showToast(AddSourceActivity.this, "开始检测");
        new Thread(() -> {
            DirectEngine engine = new DirectEngine(url, getExternalCacheDir().toString(), TimeFileNameFormat.getInstance());
            engine.downLoadWallpaper(new IWallpaperEngine.Callback() {
                @Override
                public void onSucceed(File file) {
                    runOnUiThread(() -> {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeFile(file.toString());
                            ivWallpaper.setImageBitmap(bitmap);
                            detectedResult = true;
                            ToastUtil.showToast(AddSourceActivity.this, "检测成功");
                        } catch (Exception e) {
                            e.printStackTrace();
                            ToastUtil.showToast(AddSourceActivity.this, "检测失败: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onMessage(String msg) {
                    ToastUtil.showToast(AddSourceActivity.this, msg);
                }

                @Override
                public void onFailed(Exception e) {
                    ToastUtil.showToast(AddSourceActivity.this, e.getMessage());
                    detectedResult = false;
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
