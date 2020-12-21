package xyz.liut.bingwallpaper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import xyz.liut.bingwallpaper.bean.SourceBean;
import xyz.liut.bingwallpaper.utils.ToastUtil;

/**
 * 源列表
 * <p>
 * Create by liut
 * 2020/10/31
 */
public class SourceListActivity extends Activity {

    private List<SourceBean> sourceList;

    private SourceListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_list);

        setTitle(R.string.wallpaper_source_list);
        sourceList = new ArrayList<>();

        ListView listView = findViewById(R.id.lv_source);
        listView.setAdapter(adapter = new SourceListAdapter(this, sourceList));

        // 点击选择
        listView.setOnItemClickListener((parent, view, position, id) -> {
            SourceBean bean = sourceList.get(position);
            SourceManager.saveDefaultSource(SourceListActivity.this, bean);
            finish();
        });

        // 长按删除
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            SourceBean bean = sourceList.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("删除")
                    .setMessage("是否删除" + bean.getName() + "?")
                    .setPositiveButton("是", (dialog, which) -> {
                        if (bean.isInternal()) {
                            ToastUtil.showToast(SourceListActivity.this, "内置源无法删除");
                        } else {
                            SourceManager.remove(SourceListActivity.this, bean.getName());
                            sourceList.remove(bean);
                            adapter.notifyDataSetInvalidated();
                            ToastUtil.showToast(SourceListActivity.this, "已删除： " + bean.getName());
                        }
                    })
                    .setNegativeButton("否", null)
                    .show();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        sourceList.clear();
        sourceList.addAll(SourceManager.getSourceList(this));
        adapter.notifyDataSetInvalidated();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.source_list_menu, menu);
        return true;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_source:
                startActivity(new Intent(this, AddSourceActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

