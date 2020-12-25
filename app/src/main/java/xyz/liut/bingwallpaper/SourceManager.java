package xyz.liut.bingwallpaper;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xyz.liut.bingwallpaper.bean.SourceBean;
import xyz.liut.bingwallpaper.engine.BingWallpaperEngine;
import xyz.liut.bingwallpaper.utils.ScreenUtils;
import xyz.liut.bingwallpaper.utils.SpTool;

/**
 * 源管理器
 * <p>
 * Create by liut on 20-11-4
 */
public class SourceManager {

    private static final String TAG = "SourceManager";

    private static final String separator = "separator";

    private static final String HUAWEI = "huawei";

    private static final String HONOR = "honor";

    private static final List<SourceBean> bingSourceBeans;
    private static final List<SourceBean> directSourceBeans;

    static {
        List<SourceBean> BingBeans = new ArrayList<>();
        BingBeans.add(new SourceBean(BingWallpaperEngine.NAME_UHD, BingWallpaperEngine.BING_URL, "[推荐]微软bing搜索每日壁纸", true));
        BingBeans.add(new SourceBean(BingWallpaperEngine.NAME_1080_1920, BingWallpaperEngine.BING_URL, "[推荐]微软bing搜索每日壁纸", true));
        bingSourceBeans = Collections.unmodifiableList(BingBeans);

        List<SourceBean> directBeans = new ArrayList<>();
        directBeans.add(new SourceBean("岁月小筑", "https://img.xjh.me/random_img.php?return=302", "岁月小筑随机背景API", true));

        directBeans.add(new SourceBean("小歪高清", "https://api.ixiaowai.cn/gqapi/gqapi.php", "小歪API - 随机图片API 随心所动 不再单调", true));
        directBeans.add(new SourceBean("小歪mc酱动漫", "https://api.ixiaowai.cn/mcapi/mcapi.php", "小歪API - 随机图片API 随心所动 不再单调", true));
        directBeans.add(new SourceBean("小歪二次元动漫", "https://api.ixiaowai.cn/api/api.php", "小歪API - 随机图片API 随心所动 不再单调", true));
        directSourceBeans = Collections.unmodifiableList(directBeans);
    }

    /**
     * 获取所有源
     */
    public static List<SourceBean> getSourceList(Context context) {
        List<SourceBean> beans = new ArrayList<>();
        beans.addAll(bingSourceBeans);
        beans.addAll(getPhoneResolutionsSource(context));
        beans.addAll(directSourceBeans);
        beans.addAll(getAddedSourceList(context));
        return Collections.unmodifiableList(beans);
    }

    private static List<SourceBean> getPhoneResolutionsSource(Context context) {
        List<SourceBean> beans = new ArrayList<>();
        int w = ScreenUtils.getScreenWidth(context);
        int h = ScreenUtils.getScreenHeight(context);
        beans.add(new SourceBean("Unsplash Source Random", "https://source.unsplash.com/random/" + w + "x" + h, "[推荐]The most powerful photo engine in the world.", true));
        beans.add(new SourceBean("Lorem Picsum", "https://picsum.photos/" + w + "/" + h, "[推荐]The Lorem Ipsum for photos.", true));
        return beans;
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
            String brand = Build.BRAND;
            if (brand != null) {
                switch (brand) {
                    case HONOR:
                    case HUAWEI:
                        return bingSourceBeans.get(0);
                    default:
                        return bingSourceBeans.get(1);
                }
            } else {
                return bingSourceBeans.get(1);
            }
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
