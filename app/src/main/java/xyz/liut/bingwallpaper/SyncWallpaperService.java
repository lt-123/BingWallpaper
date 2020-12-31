package xyz.liut.bingwallpaper;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

import xyz.liut.bingwallpaper.bean.SourceBean;
import xyz.liut.bingwallpaper.engine.EngineFactory;
import xyz.liut.bingwallpaper.engine.IWallpaperEngine;
import xyz.liut.bingwallpaper.utils.SpTool;
import xyz.liut.bingwallpaper.utils.ToastUtil;
import xyz.liut.bingwallpaper.utils.WallpaperTool;

/**
 * 同步壁纸
 */
public class SyncWallpaperService extends Service implements IWallpaperEngine.Callback {

    private static final String TAG = "SyncWallpaperService";

    /**
     * 失败重试次数
     */
    private volatile int retryTime;

    private volatile IWallpaperEngine engine;

    private volatile SpTool spTool;

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
        spTool = SpTool.getDefault(this);

        // 默认源
        SourceBean sourceBean = SourceManager.getDefaultSource(this);

        // 根据源获取引擎
        engine = EngineFactory.getDefault(this).getEngineBySourceBean(sourceBean);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (wallpaperThread == null) {
            wallpaperThread = new Thread(this::syncWallpaper);
            wallpaperThread.start();
        } else {
            Log.w(TAG, "wallpaperThread 正在执行中");
        }
        return START_NOT_STICKY;
    }

    private void syncWallpaper() {
        Log.d(TAG, "onHandleIntent: start ====");

        setNotification("下载中...");

        // 开始下载
        engine.downLoadWallpaper(this);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopSelf();
        wallpaperThread = null;
    }

    @Override
    public void onSucceed(File file) {
        retryTime = 0;

        try {
            boolean setLockScreen = SpTool.getDefault(this).get(Constants.Default.KEY_LOCK_SCREEN, false);
            WallpaperTool.setFile2Wallpaper(SyncWallpaperService.this, file, setLockScreen);
            showMsg("设置壁纸成功");

            // 设置定时
            setJob(true);

        } catch (Exception e) {
            e.printStackTrace();
            showMsg("不支持设置壁纸: " + e.getMessage());
        }

        // 保存文件
        saveFile(file);

    }


    /**
     * 普通消息
     *
     * @param msg 消息
     */
    @Override
    public void onMessage(String msg) {
        showMsg(msg);
    }

    /**
     * 进度信息更新到通知栏
     *
     * @param msg 进度消息
     */
    @Override
    public void onProgressMessage(String msg) {
        setNotification(msg);
    }

    @Override
    public void onFailed(Exception e) {
        Log.d(TAG, "onFailed() called with: msg = [" + e.getMessage() + "], retryTime=" + retryTime);
        e.printStackTrace();

        if (retryTime < 3) {
            //noinspection NonAtomicOperationOnVolatileField
            retryTime++;
            showMsg("下载出错: " + e.getMessage() + ", 正在重试(" + retryTime + "/3)...");
            engine.downLoadWallpaper(this);
        } else {
            showMsg("同步壁纸失败");

            setJob(false);
        }

    }


    /**
     * 保存壁纸文件
     *
     * @param file 文件
     */
    private void saveFile(File file) {
        boolean isSave = spTool.get(Constants.Default.KEY_SAVE, false);
        if (isSave) {
            try {
                File dir = new File(Constants.Config.WALLPAPER_SAVE_PATH + engine.engineName() + File.separator);
                if (!dir.exists()) {
                    boolean r = dir.mkdirs();
                    Log.d(TAG, "创建文件夹: " + r);
                }
                if (dir.exists()) {
                    File dest = new File(dir, file.getName());
                    boolean ret = file.renameTo(dest);
                    Log.d(TAG, "dest ret: " + ret);

                    if (!ret) {
                        showMsg("保存文件失败: " + dest);
                    }
                } else {
                    Log.e(TAG, "创建文件夹失败: " + dir);

                }

            } catch (Exception e) {
                e.printStackTrace();
                showMsg("保存文件失败: " + e.getMessage());
            }
        } else {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    /**
     * 设置定时任务
     *
     * @param bool 是否成功的
     */
    private void setJob(boolean bool) {
        JobScheduler scheduler = (JobScheduler) getApplication().getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler == null) {
            showMsg("不支持自动同步壁纸");
            return;
        }
        scheduler.cancelAll();

        if (!bool) {
            AlarmJob.setupDelay(this, 30, 30 * 2);
            showMsg("设置壁纸失败，半个多小时后自动重试");
        }

        List<String> timedList = TimedListManager.loadTimedList(this);
        for (String timed : timedList) {
            // 时间, 格式: hh:mm
            String[] times = timed.split(":");
            int hour = Integer.parseInt(times[0]);
            int minute = Integer.parseInt(times[1]);

            boolean scheduleResult = AlarmJob.setupTimed(this, hour, minute, 30);
            if (scheduleResult) {
                Log.i(TAG, "定时ok");
            } else {
                showMsg("不支持自动同步壁纸");
                break;
            }
        }
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
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 11, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_bing)
                .setContentTitle(engine.engineName())
                .setContentText(msg);

        startForeground(1, builder.build());

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
