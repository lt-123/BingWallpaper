package xyz.liut.bingwallpaper.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.Set;

/**
 * Create by liut on 20-10-27
 */
public class SpTool {

    private final SharedPreferences preferences;

    public static SpTool getDefault(Context context) {
        return new SpTool(context, null);
    }

    public SpTool(Context context, String sp) {
        if (TextUtils.isEmpty(sp)) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        } else {
            preferences = context.getSharedPreferences(sp, Context.MODE_PRIVATE);
        }
    }

    public void save(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    public String get(String key) {
        return preferences.getString(key, "");
    }

    public String get(String key, String def) {
        return preferences.getString(key, def);
    }


    public void saveStringSet(String key, Set<String> value) {
        preferences.edit().putStringSet(key, value).apply();
    }

    public Set<String> getStringSet(String key) {
        return preferences.getStringSet(key, null);
    }

    public Set<String> get(String key, Set<String> def) {
        return preferences.getStringSet(key, def);
    }


}
