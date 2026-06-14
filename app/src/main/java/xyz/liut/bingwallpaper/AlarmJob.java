package xyz.liut.bingwallpaper;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.util.Log;

import xyz.liut.bingwallpaper.v3.schedule.ScheduleManager;

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
        return new ScheduleManager(context).scheduleDaily(hour, minute, delayMinute);
    }

    /**
     * 定时
     *
     * @param minLatencyMinutes        分钟之后
     * @param maxExecutionDelayMinutes 分钟之前
     * @return 结果
     */
    public static boolean setupDelay(Context context, long minLatencyMinutes, long maxExecutionDelayMinutes) {
        return new ScheduleManager(context).scheduleDelay(minLatencyMinutes, maxExecutionDelayMinutes);
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
