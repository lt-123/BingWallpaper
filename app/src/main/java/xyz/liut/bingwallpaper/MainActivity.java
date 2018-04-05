package xyz.liut.bingwallpaper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

    public static final String TEXT = "BingWallpaper\n" +
            "\n" +
            "注意：本软件不会常驻后台，但需接收系统广播，请在手机管家中加白。\n" +
            "\n" +
            "自动设置 Bing 每日美图为壁纸，设置时机如下：\n" +
            "  桌面点击“同步壁纸”；\n" +
            "  开机启动时会尝试设置；\n" +
            "  当日期发生变更时设置；\n" +
            "当因网络等原因设置失败时，会约在半小时后会重试，直到设置成功。\n" +
            "\n" +
            "说明：\n" +
            "  1. 此界面可通过桌面设置壁纸中，选择 “BingWallpaper” 或拨号盘输入 *#*#2464#*#* 进入软件界面，(2464 为 T9 输入法 bing 拼音全拼)；\n" +
            "  2. 下载的壁纸为\"1920x1080\"分辨率（BingAPI不支持自定义分辨率功能，但可手动改URL来设定分辨率，尝试多次，发现这是适合手机的最大分辨率），保存在“BingWallpaper”文件夹中；\n" +
            "  3. Android N 以上版本会同时设置锁屏壁纸。\n\n" +
            "\n" +
            "本软件纯绿色，不常驻后台，无不良代码，具体可查阅源码。\n" +
            "\n" +
            "如过大家有需要什么功能可酷安评论中直接说，我尽量改。\n" +
            "\n" +
            "感谢 bing 壁纸 API";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = findViewById(R.id.text_tv);
        textView.setText(TEXT);
        startService(new Intent(getApplicationContext(), SyncWallpaperService.class));
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.show_src_bt:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri content_url = Uri.parse("https://github.com/lt-123/BingWallpaper");
                intent.setData(content_url);
                startActivity(intent);
                break;
        }

    }

}
