package xyz.liut.bingwallpaper;

import android.app.Notification;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Calendar;

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

    private SourceBean sourceBean;

    private volatile IWallpaperEngine engine;

    private SpTool spTool;

    @Override
    public void onCreate() {
        super.onCreate();
        spTool = SpTool.getDefault(this);

        // 默认源
        sourceBean = SourceManager.getDefaultSource(this);

        // 根据源获取引擎
        engine = EngineFactory.getDefault(this).getEngineBySourceBean(sourceBean);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::syncWallpaper).start();
        return super.onStartCommand(intent, flags, startId);
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
    }

    @Override
    public void onSucceed(File file) {
        retryTime = 0;

        try {
            WallpaperTool.setFile2Wallpaper(SyncWallpaperService.this, file, true);
            showMsg("设置壁纸成功");

            // 保存文件
            saveFile(file);

            // 设置定时
            setJob(true);

        } catch (Exception e) {
            e.printStackTrace();
            showMsg("不支持设置壁纸: " + e.getMessage());
        }
    }

    /**
     * 保存壁纸文件
     *
     * @param file 文件
     */
    private void saveFile(File file) {
//            boolean isSave = spTool.get(Constants.Default.KEY_SAVE, false);
        boolean isSave = true;
        if (isSave) {
            boolean ret = false;
            try {
                // TODO 检查权限
                File dest = new File(Constants.Config.WALLPAPER_SAVE_PATH + file.getName());
                File dir = dest.getParentFile();
                Log.d("liu", dir.toString());
                if (!dir.exists()) {
                    boolean r = dir.mkdirs();
                    Log.d("liut", String.valueOf(r));
                }
                ret = file.renameTo(dest);
                Log.d("dest ret", dest.toString());
                Log.d("dest ret", String.valueOf(ret));
            } catch (Exception e) {
                e.printStackTrace();
                if (!ret) {
                    showMsg("保存文件失败: " + e.getMessage());
                }
            }
        } else {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @Override
    public void onMessage(String msg) {
        showMsg(msg);
    }

    @Override
    public void onFailed(String msg) {
        Log.d(TAG, "onFailed() called with: msg = [" + msg + "], retryTime=" + retryTime);

        if (retryTime < 3) {
            //noinspection NonAtomicOperationOnVolatileField
            retryTime++;
            engine.downLoadWallpaper(this);
            showMsg(msg + ", 正在重试(" + retryTime + "/3)...");
        } else {
            showMsg("同步壁纸失败");

            setJob(false);
        }

    }

    /**
     * 设置定时任务
     *
     * @param bool 是否成功的
     */
    private void setJob(boolean bool) {
        // --------------- 定时
        JobScheduler scheduler = (JobScheduler) getApplication().getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler == null) {
            showMsg("不支持自动同步壁纸");
            return;
        }
        scheduler.cancelAll();

        ComponentName componentName = new ComponentName(getApplication(), AlarmJob.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(1123, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
//                .setPrefetch(true);   // 开启会崩溃？
        if (bool) {
            Calendar now = Calendar.getInstance();
            Log.d(TAG, "time " + now.getTime());
            Calendar targetTime = (Calendar) now.clone();
            targetTime.set(Calendar.HOUR_OF_DAY, 0);
            targetTime.set(Calendar.MINUTE, 0);
            targetTime.set(Calendar.SECOND, 0);
            targetTime.set(Calendar.MILLISECOND, 0);

            if (targetTime.before(now)) {
                targetTime.add(Calendar.DATE, 1);
            }
            long latencyTime = targetTime.getTimeInMillis() - now.getTimeInMillis();
            builder.setMinimumLatency(latencyTime).setOverrideDeadline(latencyTime + 1000L * 60 * 30);
        } else {
            builder.setMinimumLatency(1000L * 60 * 30).setOverrideDeadline(1000L * 60 * 30 * 2);
            showMsg("设置壁纸失败，半个多小时后自动重试");
        }

        JobInfo jobInfo = builder.build();

        Log.d(TAG, "jonInfo --> " + jobInfo.getMinLatencyMillis() / 60 / 1000L);

        int scheduleResult = scheduler.schedule(jobInfo);
        if (scheduleResult == JobScheduler.RESULT_SUCCESS) {
            Log.i(TAG, "定时ok");
        } else {
            showMsg("-不支持自动同步壁纸-");
        }

    }

    private void setNotification(String msg) {
        Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, Constants.Config.CHANNEL_ONE_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_bing)
                .setContentTitle(sourceBean.getName())
                .setContentText(msg);

        startForeground(1, builder.build());

    }

    private void showMsg(String msg) {
        setNotification(msg);
        ToastUtil.showToast(this, msg);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
