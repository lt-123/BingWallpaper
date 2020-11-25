package xyz.liut.bingwallpaper;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xyz.liut.bingwallpaper.bean.SourceBean;
import xyz.liut.bingwallpaper.engine.BingWallpaperEngine;
import xyz.liut.bingwallpaper.utils.SpTool;

/**
 * 源管理器
 * <p>
 * Create by liut on 20-11-4
 */
public class SourceManager {

    private static final String TAG = "SourceManager";

    public static final String separator = "separator";

    private static final List<SourceBean> internalSourceBeans;

    static {
        List<SourceBean> beans = new ArrayList<>();
        beans.add(new SourceBean(BingWallpaperEngine.NAME, BingWallpaperEngine.BING_URL, "[推荐]微软bing搜索每日壁纸", true));

        beans.add(new SourceBean("Unsplash Random", "https://source.unsplash.com/random", "[推荐]The most powerful photo engine in the world.", true));

        beans.add(new SourceBean("Lorem Picsum 1080P", "https://picsum.photos/1080", "[推荐]The Lorem Ipsum for photos. 1080P", true));
        beans.add(new SourceBean("Lorem Picsum 1920P", "https://picsum.photos/1920", "[推荐]The Lorem Ipsum for photos. 1920P", true));
        beans.add(new SourceBean("Lorem Picsum 2160P", "https://picsum.photos/2160", "[推荐]The Lorem Ipsum for photos. 2160P", true));

        beans.add(new SourceBean("小歪高清", "https://api.ixiaowai.cn/gqapi/gqapi.php", "小歪API - 随机图片API 随心所动 不再单调", true));
        beans.add(new SourceBean("小歪mc酱动漫", "https://api.ixiaowai.cn/mcapi/mcapi.php", "小歪API - 随机图片API 随心所动 不再单调", true));
        beans.add(new SourceBean("小歪二次元动漫", "https://api.ixiaowai.cn/api/api.php", "小歪API - 随机图片API 随心所动 不再单调", true));

        beans.add(new SourceBean("岁月小筑", "http://img.xjh.me/random_img.php", "岁月小筑随机背景API", true));

        internalSourceBeans = Collections.unmodifiableList(beans);
    }

    /**
     * 获取所有源
     */
    public static List<SourceBean> getSourceList(Context context) {
        List<SourceBean> beans = new ArrayList<>();
        beans.addAll(internalSourceBeans);
        beans.addAll(getAddedSourceList(context));
        return Collections.unmodifiableList(beans);
    }


    /**
     * 添加一个源
     */
    public static void add(Context context, SourceBean bean) {
        List<SourceBean> list = getAddedSourceList(context);
        list.add(bean);
        saveAddedSourceList(context, list);
    }

    /**
     * 移除一个源
     *
     * @param name 圆名
     */
    public static void remove(Context context, String name) {
        List<SourceBean> list = getAddedSourceList(context);
        List<SourceBean> del = new ArrayList<>();
        for (SourceBean bean : list) {
            if (TextUtils.equals(name, bean.getName())) {
                del.add(bean);
            }
        }
        for (SourceBean bean : del) {
            list.remove(bean);
        }
        saveAddedSourceList(context, list);
    }

    /**
     * 保存默认源
     */
    public static void saveDefaultSource(Context context, SourceBean bean) {
        SpTool tool = SpTool.getDefault(context);
        String sb = bean.getName() + separator + bean.getUrl() + separator + bean.getDesc() + separator + bean.isInternal();
        Log.i(TAG, "saveDefaultSource: " + sb);
        tool.save(Constants.Default.KEY_DEFAULT_SOURCE, sb);
    }

    /**
     * 获取默认源
     */
    public static SourceBean getDefaultSource(Context context) {
        SpTool tool = SpTool.getDefault(context);
        String string = tool.get(Constants.Default.KEY_DEFAULT_SOURCE);
        Log.i(TAG, "getDefaultSource: " + string);
        if (TextUtils.isEmpty(string)) {
            return internalSourceBeans.get(0);
        } else {
            String[] items = string.split(separator);
            boolean internal = Boolean.parseBoolean(items[3]);
            return new SourceBean(items[0], items[1], items[2], internal);
        }
    }


    /**
     * @return 手动添加的源列表
     */
    private static List<SourceBean> getAddedSourceList(Context context) {
        List<SourceBean> beans = new ArrayList<>();

        SpTool tool = SpTool.getDefault(context);
        Set<String> sourceSet = tool.get(Constants.Default.KEY_SOURCE_LIST, new HashSet<>());

        for (String string : sourceSet) {
            if (!TextUtils.isEmpty(string)) {
                try {
                    String[] items = string.split(separator);
                    boolean internal = Boolean.parseBoolean(items[3]);
                    beans.add(new SourceBean(items[0], items[1], items[2], internal));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return beans;
    }


    /**
     * 保存手动添加的源列表
     */
    private static void saveAddedSourceList(Context context, List<SourceBean> beans) {
        Set<String> sets = new HashSet<>();
        for (SourceBean bean : beans) {
            sets.add(bean.getName() + separator + bean.getUrl() + separator + bean.getDesc() + separator + bean.isInternal());
        }

        SpTool tool = SpTool.getDefault(context);
        tool.saveStringSet(Constants.Default.KEY_SOURCE_LIST, sets);
    }


}
