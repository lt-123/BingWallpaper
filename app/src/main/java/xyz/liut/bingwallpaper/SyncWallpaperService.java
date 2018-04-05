package xyz.liut.bingwallpaper;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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

        handler = new MyHandler(getApplicationContext());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.e(TAG, "onHandleIntent: start ====");

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
                            new FileInputStream(jpgFile), null, true,
                            WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
                } else
                    WallpaperManager.getInstance(getApplicationContext()).setStream(new FileInputStream(jpgFile));
                result = true;
                showToastMsg("设置成功，壁纸已保存");
            } else {
                Log.e(TAG, "下载壁纸失败");
                jpgFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToastMsg("壁纸设置失败：" + e.getMessage());
        }

        if (!result) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                PendingIntent pi = PendingIntent.getService(
                        getApplicationContext(),
                        1123,
                        new Intent(getApplicationContext(), SyncWallpaperService.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);

                am.setWindow(
                        AlarmManager.RTC,
                        System.currentTimeMillis() + 1000L * 30,
                        1000L * 10,
                        pi);
                Log.e(TAG, "设置壁纸失败，约半小时后自动重试");
                showToastMsg("设置壁纸失败，约半小时后自动重试");
            }

        }


    }

    private void showToastMsg(String msg) {
        Message message = handler.obtainMessage();
        message.obj = msg;
        handler.sendMessage(message);
    }

    private static class MyHandler extends Handler {

        private Context context;

        private MyHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg != null && msg.obj != null && msg.obj instanceof String) {
                Toast.makeText(context, (String) (msg.obj), Toast.LENGTH_SHORT).show();
            }
        }

    }

}
