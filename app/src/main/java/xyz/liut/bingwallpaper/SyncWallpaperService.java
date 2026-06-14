package xyz.liut.bingwallpaper;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import xyz.liut.bingwallpaper.utils.ToastUtil;
import xyz.liut.bingwallpaper.v3.network.HttpDownloader;
import xyz.liut.bingwallpaper.v3.schedule.ScheduleManager;
import xyz.liut.bingwallpaper.v3.settings.SettingsStore;
import xyz.liut.bingwallpaper.v3.source.BingWallpaperSource;
import xyz.liut.bingwallpaper.v3.storage.WallpaperStore;
import xyz.liut.bingwallpaper.v3.sync.SyncResult;
import xyz.liut.bingwallpaper.v3.sync.WallpaperSyncUseCase;
import xyz.liut.bingwallpaper.v3.wallpaper.WallpaperSetter;

/**
 * 同步壁纸
 */
public class SyncWallpaperService extends Service {

    private static final String TAG = "SyncWallpaperService";
    private static final int MAX_RETRY_COUNT = 3;

    private volatile SettingsStore settingsStore;
    private volatile BingWallpaperSource wallpaperSource;
    private volatile ScheduleManager scheduleManager;
    private volatile WallpaperSyncUseCase syncUseCase;
    private volatile Thread wallpaperThread;

    /**
     * 启动
     */
    public static void start(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context.getApplicationContext(), SyncWallpaperService.class));
        } else {
            context.startService(new Intent(context.getApplicationContext(), SyncWallpaperService.class));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        settingsStore = new SettingsStore(this);
        HttpDownloader httpDownloader = new HttpDownloader();
        wallpaperSource = new BingWallpaperSource();
        WallpaperStore wallpaperStore = new WallpaperStore(this);
        WallpaperSetter wallpaperSetter = new WallpaperSetter(this);
        scheduleManager = new ScheduleManager(this);
        syncUseCase = new WallpaperSyncUseCase(
                wallpaperSource,
                wallpaperStore,
                settingsStore,
                wallpaperSetter,
                httpDownloader);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (wallpaperThread == null) {
            wallpaperThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    syncWallpaper(startId);
                }
            });
            wallpaperThread.start();
        } else {
            setNotification("wallpaperThread 正在执行中");
            Log.w(TAG, "wallpaperThread 正在执行中");
            // 重复启动只确认本次请求，不打断当前正在执行的同步。
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    private void syncWallpaper(int startId) {
        Log.d(TAG, "syncWallpaper: start");
        try {
            runSyncWithRetry();
        } finally {
            wallpaperThread = null;
            stopSelf(startId);
        }
    }

    /**
     * 执行 v3 同步流程。
     * <p>
     * 首次同步失败后最多再重试 3 次；成功后恢复用户配置的定时任务，
     * 最终失败时安排 30 分钟后的补偿重试。
     */
    private void runSyncWithRetry() {
        for (int retryCount = 0; retryCount <= MAX_RETRY_COUNT; retryCount++) {
            int attempt = retryCount + 1;
            setNotification("正在同步壁纸（第 " + attempt + " 次）...");

            SyncResult result = syncUseCase.sync();
            if (result.isSuccess()) {
                showMsg("设置壁纸成功");
                scheduleManager.cancelRetry();
                scheduleConfiguredJobs();
                return;
            }

            Exception exception = result.getException();
            if (exception != null) {
                Log.w(TAG, "sync failed: " + result.getMessage(), exception);
            } else {
                Log.w(TAG, "sync failed: " + result.getMessage());
            }

            if (retryCount < MAX_RETRY_COUNT) {
                setNotification("同步壁纸失败，正在重试（" + (retryCount + 1) + "/" + MAX_RETRY_COUNT + "）...");
            }
        }

        showMsg("同步壁纸失败，30 分钟后自动重试");
        scheduleRetryJob();
    }

    /**
     * 根据用户配置重新安排每日定时同步任务。
     */
    private void scheduleConfiguredJobs() {
        List<int[]> timedJobs = parseTimedJobs(settingsStore.timedList());
        if (!scheduleManager.scheduleDaily(timedJobs)) {
            showMsg("不支持自动同步壁纸");
        }
    }

    /**
     * 安排最终失败后的延迟重试任务。
     */
    private void scheduleRetryJob() {
        if (!scheduleManager.scheduleRetry()) {
            showMsg("不支持自动同步壁纸");
        }
    }

    private List<int[]> parseTimedJobs(List<String> timedList) {
        List<int[]> jobs = new ArrayList<>();
        for (String timed : timedList) {
            try {
                // 用户配置格式为 HH:mm，解析失败时跳过单个异常配置，不影响其他定时任务。
                String[] times = timed.split(":");
                int hour = Integer.parseInt(times[0]);
                int minute = Integer.parseInt(times[1]);
                jobs.add(new int[]{hour, minute});
            } catch (RuntimeException e) {
                Log.w(TAG, "invalid timed job: " + timed, e);
            }
        }
        return jobs;
    }

    /**
     * 显示通知 / toast
     *
     * @param msg content
     */
    private void showMsg(String msg) {
        setNotification(msg);
        ToastUtil.showToast(this, msg);
    }

    /**
     * 显示通知
     */
    private void setNotification(String msg) {
        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, Constants.Config.CHANNEL_ONE_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Intent intent = new Intent(this, SettingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                11,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_bing)
                .setContentTitle(wallpaperSource.name())
                .setContentText(msg);

        startForeground(1, builder.build());

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
