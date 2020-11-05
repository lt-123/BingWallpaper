package xyz.liut.bingwallpaper;

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
import xyz.liut.bingwallpaper.utils.ToastUtil;
import xyz.liut.bingwallpaper.utils.WallpaperTool;

public class SyncWallpaperService extends Service {

    private static final String TAG = "SyncWallpaperService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::syncWallpaper).start();
        return super.onStartCommand(intent, flags, startId);
    }

    private void syncWallpaper() {
        Log.d(TAG, "onHandleIntent: start ====");
        final boolean isSave = false;

        // 默认源
        SourceBean defaultBean = SourceManager.getDefaultSource(this);

        // 根据源获取引擎
        IWallpaperEngine engine = EngineFactory.getDefault(this).getEngineBySourceBean(defaultBean);

        if (engine != null) {
            engine.downLoadWallpaper(new IWallpaperEngine.Callback() {
                @Override
                public void onSucceed(File file) {
                    ToastUtil.showToast(SyncWallpaperService.this, file.toString());

                    try {
                        WallpaperTool.setFile2Wallpaper(SyncWallpaperService.this, file, true);
                        ToastUtil.showToast(SyncWallpaperService.this, "设置壁纸成功");
                        setJob(true);

                        if (!isSave) {
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                        } else {
//                        file.renameTo()
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ToastUtil.showToast(SyncWallpaperService.this, "不支持设置壁纸: " + e.getMessage());
                    }
                }

                @Override
                public void onMessage(String msg) {
                    ToastUtil.showToast(SyncWallpaperService.this, msg);
                }

                @Override
                public void onFailed(String msg) {
                    ToastUtil.showToast(SyncWallpaperService.this, msg);
                    setJob(false);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
