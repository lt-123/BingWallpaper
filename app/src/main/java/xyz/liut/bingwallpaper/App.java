package xyz.liut.bingwallpaper;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

/**
 * BingWallpaper xyz.liut.bingwallpaper
 * Created by liut on 2018/4/9.
 */
public class App extends Application {

    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            PendingIntent pi = PendingIntent.getService(
                    getApplicationContext(),
                    11235,
                    new Intent(getApplicationContext(), SyncWallpaperService.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Calendar now = Calendar.getInstance(Locale.CHINESE);
            Calendar targetTime = (Calendar) now.clone();
            targetTime.set(Calendar.HOUR_OF_DAY, 0);
            targetTime.set(Calendar.MINUTE, 0);
            targetTime.set(Calendar.SECOND, 0);
            targetTime.set(Calendar.MILLISECOND, 0);

            if (targetTime.before(now))
                targetTime.add(Calendar.DATE, 1);
            am.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    targetTime.getTimeInMillis(),
                    6000L * 10,
                    pi);

            Log.i(TAG, "设置闹钟完成");
        }

    }

}
