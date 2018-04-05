package xyz.liut.bingwallpaper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ManualSetActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(getApplicationContext(), SyncWallpaperService.class));
        finish();
    }
}
