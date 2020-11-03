package xyz.liut.bingwallpaper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by liut
 * 2020/10/31
 */
public class SourceListActivity extends Activity {

    private List<Pair<String, String>> sourceList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_list);

        setTitle(R.string.wallpaper_source_list);

        sourceList = new ArrayList<>();
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));
        sourceList.add(new Pair<>("必应", "xx/sss/dd"));

        ListView listView = findViewById(R.id.lv_source);
        listView.setAdapter(new SourceListAdapter(this, sourceList));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.source_list_menu, menu);
        return true;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_source:
                showAddSourceDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAddSourceDialog() {
        new Dialog(this).show();
    }
}

