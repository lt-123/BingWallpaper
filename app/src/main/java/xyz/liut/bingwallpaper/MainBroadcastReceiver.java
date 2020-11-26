package xyz.liut.bingwallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MainBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "MainBroadcastReceiver";

    public static final String ACTION_SECRET_CODE = "android.provider.Telephony.SECRET_CODE";

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) return;

        Log.d(TAG, intent.getAction());

        switch (intent.getAction()) {
            case ACTION_SECRET_CODE:
                Intent i = new Intent(context, SettingActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
                break;
        }

    }

}
