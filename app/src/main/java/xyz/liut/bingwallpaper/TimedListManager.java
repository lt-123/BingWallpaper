package xyz.liut.bingwallpaper;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import xyz.liut.bingwallpaper.utils.SpTool;

/**
 * Create by liut on 20-11-12
 */
public class TimedListManager {

    public static List<String> loadTimedList(Context context) {
        List<String> list = new ArrayList<>();

        SpTool spTool = SpTool.getDefault(context);
        String timedList = spTool.get(Constants.Default.KEY_TIMED_LIST);

        String[] timeArray = timedList.split("#");
        for (String time : timeArray) {
            if (!TextUtils.isEmpty(time)) list.add(time);
        }
        return list;
    }

    public static void save(Context context, List<String> timedList) {
        SpTool spTool = SpTool.getDefault(context);
        spTool.save(Constants.Default.KEY_TIMED_LIST, TextUtils.join("#", timedList));
    }


    public static void clear(Context context) {
        SpTool spTool = SpTool.getDefault(context);
        spTool.save(Constants.Default.KEY_TIMED_LIST, null);
    }


}
