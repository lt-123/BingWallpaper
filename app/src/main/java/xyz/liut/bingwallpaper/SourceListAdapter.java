package xyz.liut.bingwallpaper;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * 壁纸源列表
 * <p>
 * Create by liut
 * 2020/10/31
 */
public class SourceListAdapter extends BaseAdapter {

    private final Context context;

    private final List<Pair<String, String>> sourceList;

    public SourceListAdapter(@NonNull Context context, @NonNull List<Pair<String, String>> sourceList) {
        this.context = context;
        this.sourceList = sourceList;
    }

    @Override
    public int getCount() {
        return sourceList.size();
    }

    @Override
    public Pair<String, String> getItem(int i) {
        return sourceList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = (ViewGroup) View.inflate(context, R.layout.item_source, null);
            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder == null) {
                holder = new ViewHolder(view);
            }
            holder.title.setText(getItem(i).first);
            holder.subTitle.setText(getItem(i).second);
        }
        return view;
    }

    private static class ViewHolder {

        TextView title, subTitle;

        public ViewHolder(View view) {
            title = view.findViewById(R.id.tv_title);
            subTitle = view.findViewById(R.id.tv_sub_title);
        }
    }

}
