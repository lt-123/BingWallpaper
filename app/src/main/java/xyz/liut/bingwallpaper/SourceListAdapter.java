package xyz.liut.bingwallpaper;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import xyz.liut.bingwallpaper.bean.SourceBean;

/**
 * 壁纸源列表
 * <p>
 * Create by liut
 * 2020/10/31
 */
public class SourceListAdapter extends BaseAdapter {

    private final Context context;

    private final List<SourceBean> sourceList;

    public SourceListAdapter(@NonNull Context context, @NonNull List<SourceBean> sourceList) {
        this.context = context;
        this.sourceList = sourceList;
    }

    @Override
    public int getCount() {
        return sourceList.size();
    }

    @Override
    public SourceBean getItem(int i) {
        return sourceList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = View.inflate(context, R.layout.item_source, null);
        }
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder == null) {
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        SourceBean bean = getItem(i);
        holder.title.setText(bean.getName());

        String subTitle = bean.getDesc();
        if (TextUtils.isEmpty(subTitle)) {
            subTitle = bean.getUrl();
        } else {
            subTitle = subTitle + "\n" + bean.getUrl();
        }
        holder.subTitle.setText(subTitle);
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
