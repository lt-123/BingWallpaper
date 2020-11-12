package xyz.liut.bingwallpaper;

import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

/**
 * 使用jobService拉起同步壁纸服务
 * <p>
 * Create by liut on 2018/11/8 0008
 */
public class AlarmJob extends JobService {

    private static final String TAG = "AlarmJob";


    /**
     * 定时
     *
     * @param hour        小时
     * @param minute      分钟
     * @param delayMinute 最多 延后 delayMinute 分钟执行
     * @return 结果
     */
    public static boolean setupTimed(Context context, int hour, int minute, int delayMinute) {
        Calendar now = Calendar.getInstance();
        Log.d(TAG, "time " + now.getTime());

        Calendar targetTime = (Calendar) now.clone();
        targetTime.set(Calendar.HOUR_OF_DAY, hour);
        targetTime.set(Calendar.MINUTE, minute);
        targetTime.set(Calendar.SECOND, 1);
        targetTime.set(Calendar.MILLISECOND, 0);

        if (targetTime.before(now)) {
            targetTime.add(Calendar.DATE, 1);
        }

        long minLatencyMillis = targetTime.getTimeInMillis() - now.getTimeInMillis();
        long maxExecutionDelayMillis = minLatencyMillis + 1000L * 60 * delayMinute;

        return setupDelay(context, minLatencyMillis, maxExecutionDelayMillis);
    }

    /**
     * 定时
     *
     * @param minLatencyMillis        毫秒之后
     * @param maxExecutionDelayMillis 毫秒之前
     * @return 结果
     */
    public static boolean setupDelay(Context context, long minLatencyMillis, long maxExecutionDelayMillis) {
        JobScheduler scheduler = (JobScheduler) context.getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);
        ComponentName componentName = new ComponentName(context.getApplicationContext(), AlarmJob.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder((int) System.currentTimeMillis(), componentName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setPrefetch(true);
        }
        JobInfo jobInfo = builder
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(minLatencyMillis)
                .setOverrideDeadline(maxExecutionDelayMillis)
                .build();

        Log.d(TAG, "jonInfo --> " + jobInfo.getMinLatencyMillis() / 60 / 1000L);

        int scheduleResult = scheduler.schedule(jobInfo);
        boolean ret = scheduleResult == JobScheduler.RESULT_SUCCESS;

        Log.i(TAG, "result: " + ret);
        return ret;
    }

    @SuppressLint("LongLogTag")
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob() called");
        SyncWallpaperService.start(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


}
