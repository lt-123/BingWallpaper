package xyz.liut.bingwallpaper;

import android.app.IntentService;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.Calendar;

import xyz.liut.bingwallpaper.engine.BingWallpaperEngine;
import xyz.liut.bingwallpaper.engine.EngineFactory;
import xyz.liut.bingwallpaper.engine.IWallpaperEngine;
import xyz.liut.bingwallpaper.engine.TimeFileFormat;
import xyz.liut.bingwallpaper.utils.ToastUtil;
import xyz.liut.bingwallpaper.utils.WallpaperTool;

public class SyncWallpaperService extends IntentService {

    private static final String TAG = "SyncWallpaperService";

    private Handler handler;

    @SuppressWarnings("unused")
    public SyncWallpaperService() {
        super("SyncWallpaperService");
    }

    @SuppressWarnings("unused")
    public SyncWallpaperService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent: start ====");

        String path = getFilesDir().toString();
        IWallpaperEngine.FileNameFormat format = new TimeFileFormat();
        final boolean isSave = false;

        IWallpaperEngine engine = EngineFactory.getEngine(BingWallpaperEngine.NAME);
        if (engine != null) {
            engine.setWallpaper(path, format, new IWallpaperEngine.Callback() {
                @Override
                public void onSucceed(File file) {
                    boolean ret = WallpaperTool.setFile2Wallpaper(SyncWallpaperService.this, file);
                    if (ret) {
                        ToastUtil.showToast(SyncWallpaperService.this, "设置壁纸成功");
                    } else {
                        ToastUtil.showToast(SyncWallpaperService.this, "设置壁纸失败");
                    }
                    if (!isSave) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }

                    setJob(ret);
                }

                @Override
                public void onMessage(String msg) {
                    ToastUtil.showToast(SyncWallpaperService.this, msg);
                }

                @Override
                public void onFailed(String msg) {
                    ToastUtil.showToast(SyncWallpaperService.this, msg);
                }
            });
        } else {
            ToastUtil.showToast(SyncWallpaperService.this, "没有源");
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
            ToastUtil.showToast(SyncWallpaperService.this, "不支持自动同步壁纸");
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
            ToastUtil.showToast(SyncWallpaperService.this, "设置壁纸失败，半个多小时后自动重试");
        }

        JobInfo jobInfo = builder.build();

        Log.d(TAG, "jonInfo --> " + jobInfo.getMinLatencyMillis() / 60 / 1000L);

        int scheduleResult = scheduler.schedule(jobInfo);
        if (scheduleResult == JobScheduler.RESULT_SUCCESS) {
            Log.i(TAG, "定时ok");
        } else {
            ToastUtil.showToast(SyncWallpaperService.this, "-不支持自动同步壁纸-");
        }

    }


}
