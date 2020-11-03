package xyz.liut.bingwallpaper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import xyz.liut.bingwallpaper.utils.ToastUtil;

/**
 * 关于
 * <p>
 * Create by liut
 * 2020/11/2
 */
public class AboutActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView tvMsg = findViewById(R.id.tv_msg);

        tvMsg.setText("自动同步壁纸工具");


        findViewById(R.id.bt_see_source).setOnClickListener(view -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://github.com/lt-123/BingWallpaper"));
                startActivity(intent);
            } catch (Exception e) {
                ToastUtil.showToast(AboutActivity.this, "打开失败， 请检查是否安装了浏览器");
            }
        });

    }


}
