package xyz.liut.bingwallpaper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        startService(new Intent(this, SyncWallpaperService.class));
    }

    public void onClick(View view) {
        startService(new Intent(this, SyncWallpaperService.class));

    }

}
