package xyz.liut.bingwallpaper.v3.schedule;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

import xyz.liut.bingwallpaper.AlarmJob;

/**
 * 壁纸同步定时管理器。
 * <p>
 * 该类集中封装 {@link JobScheduler} 的调度逻辑，避免入口 {@link AlarmJob}
 * 同时承担 JobService 和定时计算职责。
 */
public class ScheduleManager {

    private static final String TAG = "ScheduleManager";
    private static final long MINUTE_MILLIS = 60 * 1000L;
    /*
     * JobId 保留区间：
     * 30000-31439：每日 HH:mm 任务，按 hour * 60 + minute 映射，避免非法时间归一化后碰撞。
     * 39998：通用延迟任务，当前只需要一个，重复调度时有意替换同一任务。
     * 39999：同步失败后的 30 分钟重试任务。
     */
    private static final int DAILY_JOB_ID_BASE = 30000;
    private static final int GENERIC_DELAY_JOB_ID = 39998;
    private static final int RETRY_JOB_ID = 39999;
    private static final int DEFAULT_DELAY_MINUTES = 30;
    private static final int RETRY_DELAY_MINUTES = 30;

    private final Context context;
    private final JobScheduler scheduler;

    /**
     * 创建定时管理器。
     *
     * @param context Android 上下文，会转为 application context 保存
     */
    public ScheduleManager(Context context) {
        this.context = context.getApplicationContext();
        this.scheduler = (JobScheduler) this.context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    /**
     * 计算下一次触发距离当前时间的分钟数。
     * <p>
     * 目标时间固定为当天的 HH:mm:01.000；如果该目标时间没有晚于当前时间，
     * 则顺延到第二天同一时间。返回值最小为 1 分钟，避免 JobScheduler 收到 0 分钟延迟。
     *
     * @param now    当前时间
     * @param hour   目标小时，24 小时制
     * @param minute 目标分钟
     * @return 下一次触发距离当前时间的分钟数
     */
    public static long nextDelayMinutes(Calendar now, int hour, int minute) {
        validateTime(hour, minute);

        Calendar targetTime = (Calendar) now.clone();
        targetTime.set(Calendar.HOUR_OF_DAY, hour);
        targetTime.set(Calendar.MINUTE, minute);
        targetTime.set(Calendar.SECOND, 1);
        targetTime.set(Calendar.MILLISECOND, 0);

        // 当天目标时间未晚于当前时间时，下一次触发应落在明天。
        if (!targetTime.after(now)) {
            targetTime.add(Calendar.DATE, 1);
        }

        // JobScheduler 使用毫秒延迟；这里向上取整到分钟，避免早于 HH:mm:01.000 触发。
        long delayMillis = targetTime.getTimeInMillis() - now.getTimeInMillis();
        long delayMinutes = (delayMillis + MINUTE_MILLIS - 1) / MINUTE_MILLIS;
        return Math.max(1, delayMinutes);
    }

    /**
     * 按多个 HH:mm 时间点安排每日同步任务。
     *
     * @param times 每项为 int[]{hour, minute}
     * @return 全部任务均调度成功时返回 true
     */
    public boolean scheduleDaily(List<int[]> times) {
        boolean success = true;
        for (int[] time : times) {
            if (time == null || time.length < 2) {
                success = false;
                continue;
            }
            success = scheduleDaily(time[0], time[1], DEFAULT_DELAY_MINUTES) && success;
        }
        return success;
    }

    /**
     * 按指定 HH:mm 安排每日同步任务。
     *
     * @param hour        目标小时
     * @param minute      目标分钟
     * @param delayMinute 最多延后多少分钟执行
     * @return 调度成功时返回 true
     */
    public boolean scheduleDaily(int hour, int minute, int delayMinute) {
        validateScheduleOptions(hour, minute, delayMinute);
        long minLatencyMinutes = nextDelayMinutes(Calendar.getInstance(), hour, minute);
        long maxExecutionDelayMinutes = minLatencyMinutes + delayMinute;
        return scheduleDelay(dailyJobId(hour, minute), minLatencyMinutes, maxExecutionDelayMinutes);
    }

    /**
     * 安排 30 分钟后的重试任务。
     *
     * @return 调度成功时返回 true
     */
    public boolean scheduleRetry() {
        return scheduleDelay(RETRY_JOB_ID, RETRY_DELAY_MINUTES, RETRY_DELAY_MINUTES * 2L);
    }

    /**
     * 取消同步失败后安排的补偿重试任务。
     * <p>
     * 成功同步后只清理重试任务，避免影响用户配置的每日定时任务。
     */
    public void cancelRetry() {
        scheduler.cancel(RETRY_JOB_ID);
    }

    /**
     * 按延迟分钟数安排一次同步任务。
     *
     * @param minLatencyMinutes        最早多少分钟后执行
     * @param maxExecutionDelayMinutes 最晚多少分钟后执行
     * @return 调度成功时返回 true
     */
    public boolean scheduleDelay(long minLatencyMinutes, long maxExecutionDelayMinutes) {
        return scheduleDelay(GENERIC_DELAY_JOB_ID, minLatencyMinutes, maxExecutionDelayMinutes);
    }

    private boolean scheduleDelay(int jobId, long minLatencyMinutes, long maxExecutionDelayMinutes) {
        ComponentName componentName = new ComponentName(context, AlarmJob.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(jobId, componentName);
        builder.setPrefetch(true);
        JobInfo jobInfo = builder
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(minLatencyMinutes * MINUTE_MILLIS)
                .setOverrideDeadline(maxExecutionDelayMinutes * MINUTE_MILLIS)
                .build();

        Log.d(TAG, "jobInfo minLatencyMinutes=" + jobInfo.getMinLatencyMillis() / MINUTE_MILLIS);
        int scheduleResult = scheduler.schedule(jobInfo);
        boolean success = scheduleResult == JobScheduler.RESULT_SUCCESS;
        Log.i(TAG, "schedule result: " + success);
        return success;
    }

    private static int dailyJobId(int hour, int minute) {
        return DAILY_JOB_ID_BASE + hour * 60 + minute;
    }

    static void validateScheduleOptions(int hour, int minute, int delayMinute) {
        validateTime(hour, minute);
        if (delayMinute < 0) {
            throw new IllegalArgumentException("delayMinute must be >= 0");
        }
    }

    private static void validateTime(int hour, int minute) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be between 0 and 23");
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("minute must be between 0 and 59");
        }
    }
}
