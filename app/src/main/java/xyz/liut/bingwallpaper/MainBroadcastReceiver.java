package xyz.liut.bingwallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MainBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "MainBroadcastReceiver";

    public static final String ACTION_SECRET_CODE = "android.provider.Telephony.SECRET_CODE";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) return;

        Log.d(TAG, intent.getAction());

        switch (intent.getAction()) {
            case ACTION_SECRET_CODE:
                context.startActivity(new Intent(context, MainActivity.class));
                break;
            default:
                context.startService(new Intent(context, SyncWallpaperService.class));
                break;


        }

    }

}
