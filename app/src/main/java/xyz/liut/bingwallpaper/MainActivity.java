package xyz.liut.bingwallpaper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import xyz.liut.bingwallpaper.bean.SourceBean;
import xyz.liut.bingwallpaper.utils.WallpaperTool;

/**
 * 主界面 设置页
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";


    //    private View llSource, llFrequency, llSavePath, llSetLockScreen;
    private TextView tvSource, tvFrequency, tvSavePath, tvSetLockScreen;
//    private Button btSetupNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View llSource = findViewById(R.id.ll_source);
        View llFrequency = findViewById(R.id.ll_frequency);
        View llSavePath = findViewById(R.id.ll_save_path);
        View llSetLockScreen = findViewById(R.id.ll_set_lock_screen);

        tvSource = findViewById(R.id.tv_source);
        tvFrequency = findViewById(R.id.tv_frequency);
        tvSavePath = findViewById(R.id.tv_save_path);
        tvSetLockScreen = findViewById(R.id.tv_set_lock_screen);

        Button btSetupNow = findViewById(R.id.bt_setup_now);

        llSource.setOnClickListener(this);
        llFrequency.setOnClickListener(this);
        llSavePath.setOnClickListener(this);
        llSetLockScreen.setOnClickListener(this);

        btSetupNow.setOnClickListener(this);

        findViewById(R.id.bt_clear).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadData();
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_source:
                startActivity(new Intent(this, SourceListActivity.class));
                break;
            case R.id.ll_frequency:

                break;
            case R.id.ll_save_path:
                showPathDialog();
                break;
            case R.id.ll_set_lock_screen:

                break;
            case R.id.bt_setup_now:
                startService(new Intent(getApplicationContext(), SyncWallpaperService.class));
                finish();
                break;
            case R.id.bt_clear:
                WallpaperTool.clearWallpaper(this);
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadData() {
        SourceBean bean = SourceManager.getDefaultSource(this);
        tvSource.setText(bean.getName() + "\n" + bean.getDesc());
        Log.d(TAG, "loadData() called bean = " + bean);
    }


    private void showPathDialog() {
        new AlertDialog.Builder(this)
                .setTitle("请输入保存路径")
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1123);
                    } else {
                    }
                })
                .show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        showPathDialog();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting_menu, menu);
        return true;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
