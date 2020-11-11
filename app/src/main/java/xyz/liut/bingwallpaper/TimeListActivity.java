package xyz.liut.bingwallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.Nullable;

import xyz.liut.bingwallpaper.utils.BatteryUtil;
import xyz.liut.bingwallpaper.utils.SpTool;

/**
 * 定时任务列表
 * <p>
 * Create by liut on 20-11-6
 */
public class TimeListActivity extends Activity implements View.OnClickListener, TimePickerDialog.OnTimeSetListener {

    private TimePickerDialog timePickerDialog;

    private LinearLayout llTimedList;
    private Button btIgnoreBattery;

    private SpTool spTool;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_list);

        setTitle(R.string.title_timed_list);

        spTool = SpTool.getDefault(this);

        timePickerDialog = new TimePickerDialog(this, this, 0, 0, true);

        llTimedList = findViewById(R.id.ll_timed_list);

        findViewById(R.id.bt_add_timed).setOnClickListener(this);
        findViewById(R.id.bt_clear_timed).setOnClickListener(this);

        btIgnoreBattery = findViewById(R.id.bt_ignore_battery);

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 如果未忽略显示提醒按钮
            if (!BatteryUtil.hasIgnoredBatteryOptimization(this)) {
                btIgnoreBattery.setEnabled(true);
                btIgnoreBattery.setOnClickListener(this);
            } else {
                btIgnoreBattery.setEnabled(false);
                btIgnoreBattery.setText(getString(R.string.complete_ignore_battery));
            }
        } else {
            btIgnoreBattery.setVisibility(View.GONE);
        }
    }


    /**
     * 加载保存的定时列表
     */
    private void loadData() {
        String timedList = spTool.get(Constants.Default.KEY_TIMED_LIST);

        String[] timeArray = timedList.split("#");
        for (String time : timeArray) {
            if (!TextUtils.isEmpty(time)) addTime(time);
        }
    }

    /**
     * 添加一条定时到 llTimedList
     */
    private void addTime(String time) {
        TextView textView = new TextView(this);
        textView.setPadding(0, 20, 0, 20);
        textView.setText(time);
        llTimedList.addView(textView);
    }

    @SuppressLint({"NonConstantResourceId", "NewApi"})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 添加定时
            case R.id.bt_add_timed:
                timePickerDialog.updateTime(0, 0);
                timePickerDialog.show();
                break;

            // 清空定时
            case R.id.bt_clear_timed:
                llTimedList.removeAllViews();
                spTool.save(Constants.Default.KEY_TIMED_LIST, null);
                break;

            // 忽略电池优化
            case R.id.bt_ignore_battery:
                new AlertDialog
                        .Builder(this)
                        .setTitle("忽略电池优化")
                        .setMessage("忽略电池优化会使本软件的定时更新功能更加稳定， 并不会增加耗电量， 请在接下来的提示中选择允许。")
                        .setPositiveButton("确定", (dialog, which) -> BatteryUtil.ignoreBatteryOptimization(this))
                        .show();
                break;
        }
    }


    /**
     * 添加定时dialog回调
     *
     * @param view      定时选择器
     * @param hourOfDay 选择的时间 hour
     * @param minute    选择的时间 minute
     */
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        StringBuilder builder = new StringBuilder();
        if (hourOfDay < 10) {
            builder.append("0");
        }
        builder.append(hourOfDay).append(":");
        if (minute < 10) {
            builder.append("0");
        }
        builder.append(minute);
        addTime(builder.toString());

        saveTimed();
    }

    /**
     * 保存定时
     */
    private void saveTimed() {
        int count = llTimedList.getChildCount();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            TextView textView = (TextView) llTimedList.getChildAt(i);
            String time = textView.getText().toString();
            builder.append(time);
            if (i != count - 1) {
                builder.append("#");
            }
        }
        spTool.save(Constants.Default.KEY_TIMED_LIST, builder.toString());
    }


}
