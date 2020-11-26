package xyz.liut.bingwallpaper;

import android.app.Activity;
import android.os.Bundle;

/**
 * 手动同步壁纸
 */
public class ManualSetActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        finish();

        SyncWallpaperService.start(this);
    }


}
