package xyz.liut.bingwallpaper;

import android.app.IntentService;
import android.app.WallpaperManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class SyncWallpaperService extends IntentService {

    private static final String TAG = "SyncWallpaperService";

    private static final String BING_WALLPAPER_API = "https://www.bing.com/HPImageArchive.aspx?format=js&n=1";

    private Handler handler;

    public SyncWallpaperService() {
        super("SyncWallpaperService");
    }

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

        boolean result = false;

        try {

            String wallpaperPath = Environment.getExternalStorageDirectory() + File.separator + "bingWallpaper";

            File fileDir = new File(wallpaperPath);
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }

            HttpURLConnection urlConnection = (HttpURLConnection) new URL(BING_WALLPAPER_API).openConnection();
            String resp = new BufferedReader(new InputStreamReader(urlConnection.getInputStream())).readLine();
            String jpgUrl = new JSONObject(resp)
                    .getJSONArray("images")
                    .getJSONObject(0)
                    .getString("url")
                    .replace("1920x1080", "1080x1920");
            Log.d(TAG, "jpgUrl: " + jpgUrl);

            showToastMsg("获取URL成功，开始下载");

            String urls[] = jpgUrl.split("/");

            HttpURLConnection wallpaperConn = (HttpURLConnection) new URL("https://cn.bing.com" + jpgUrl).openConnection();
            int fileLength = wallpaperConn.getContentLength();

            File jpgFile = new File(wallpaperPath + File.separator + urls[urls.length - 1]);
            Log.i(TAG, "jpgFile: " + jpgFile.getPath());

            FileOutputStream os = new FileOutputStream(jpgFile);
            InputStream is = wallpaperConn.getInputStream();

            int len;
            byte[] buffer = new byte[1024];
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }

            if (fileLength == jpgFile.length()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    WallpaperManager.getInstance(getApplicationContext()).setStream(
                            new FileInputStream(jpgFile), new Rect(0, 0, 1080, 1920), true,
                            WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
                    showToastMsg("设置成功，壁纸已保存");
                } else {
                    WallpaperManager.getInstance(getApplicationContext()).setStream(new FileInputStream(jpgFile));
                    showToastMsg("设置成功，壁纸已保存 < N");
                }
                result = true;
            } else {
                showToastMsg("下载壁纸失败");
                jpgFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToastMsg("壁纸设置失败：" + e.getMessage());
        }

        JobScheduler scheduler = (JobScheduler) getApplication().getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler == null) {
            showToastMsg("不支持自动同步壁纸");
            return;
        }
        scheduler.cancelAll();

        ComponentName componentName = new ComponentName(getApplication(), AlarmJob.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(1123, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
//                .setPrefetch(true);   // 开启会崩溃？
        if (result) {
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
            showToastMsg("设置壁纸失败，半个多小时后自动重试");
        }

        JobInfo jobInfo = builder.build();

        Log.d(TAG, "jonInfo --> " + jobInfo.getMinLatencyMillis() / 60 / 1000L);

        int scheduleResult = scheduler.schedule(jobInfo);
        if (scheduleResult == JobScheduler.RESULT_SUCCESS) {
            Log.i(TAG, "定时ok");
        } else {
            showToastMsg("-不支持自动同步壁纸-");
        }


    }

    private void showToastMsg(final String msg) {
        Log.d(TAG, msg);
        handler.post(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
    }


}
