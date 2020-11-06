package xyz.liut.bingwallpaper;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;

import java.util.List;

/**
 * Create by liut on 20-11-6
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        setupNotificationChannel();
    }

    /**
     * 通知渠道
     */
    private void setupNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            List<NotificationChannel> channels = manager.getNotificationChannels();
            Log.i("xxxxxxxx", "setupNotificationChannel: " + channels);
            if (channels == null || channels.size() == 0) {
                NotificationChannel notificationChannel = new NotificationChannel(
                        Constants.Config.CHANNEL_ONE_ID,
                        Constants.Config.CHANNEL_ONE_NAME,
                        NotificationManager.IMPORTANCE_LOW
                );
                notificationChannel.setShowBadge(false);
                manager.createNotificationChannel(notificationChannel);
            }
        }
    }


}


