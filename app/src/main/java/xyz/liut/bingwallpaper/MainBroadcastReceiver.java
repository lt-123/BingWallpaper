package xyz.liut.bingwallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MainBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "MainBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) return;

        context.startService(new Intent(context, SyncWallpaperService.class));

        Log.d(TAG, intent.getAction());
    }

}
