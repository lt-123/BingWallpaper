package xyz.liut.bingwallpaper;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

/**
 * 使用jobService拉起同步壁纸服务
 * <p>
 * Create by liut on 2018/11/8 0008
 */
public class AlarmJob extends JobService {

    private static final String TAG = "SyncWallpaperService-AlarmJob";

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
