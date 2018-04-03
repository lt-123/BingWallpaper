package xyz.liut.bingwallpaper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private PackageManager mPackageManager;

    private Button setIconBt;
    private ComponentName mainActivityComp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setIconBt = findViewById(R.id.hide_icon_bt);
        mPackageManager = getPackageManager();

        mainActivityComp = new ComponentName(getBaseContext(),
                MainActivity.class.getName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPackageManager.getComponentEnabledSetting(mainActivityComp)
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            setIconBt.setText(getString(R.string.hide_icon_bt));
        } else {
            setIconBt.setText(getString(R.string.show_icon_bt));
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.manual_set_bt:
                startService(new Intent(this, SyncWallpaperService.class));
                Toast.makeText(getApplicationContext(),
                        getString(R.string.toast_start_sync)
                        , Toast.LENGTH_SHORT).show();
                break;
            case R.id.hide_icon_bt:
                if (mPackageManager.getComponentEnabledSetting(mainActivityComp)
                        != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    mPackageManager.setComponentEnabledSetting(mainActivityComp,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    setIconBt.setText(getString(R.string.hide_icon_bt));
                } else {
                    mPackageManager.setComponentEnabledSetting(mainActivityComp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                    setIconBt.setText(getString(R.string.show_icon_bt));
                }
                break;
            case R.id.show_src_bt:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri content_url = Uri.parse("https://github.com/lt-123/BingWallpaper");
                intent.setData(content_url);
                startActivity(intent);
                break;
        }

    }

    public static class Main2Activity extends MainActivity {
    }

}
