package xyz.liut.bingwallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * 主界面 设置页
 */
public class MainActivity extends Activity implements View.OnClickListener {


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

    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ll_source:
                startActivityForResult(new Intent(this, SourceListActivity.class), 0);
                break;
            case R.id.ll_frequency:

                break;
            case R.id.ll_save_path:

                break;
            case R.id.ll_set_lock_screen:

                break;
            case R.id.bt_setup_now:
                startService(new Intent(getApplicationContext(), SyncWallpaperService.class));
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

        }
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
