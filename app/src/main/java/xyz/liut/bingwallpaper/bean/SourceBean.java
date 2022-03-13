package xyz.liut.bingwallpaper.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Create by liut
 * 2020/10/31
 */
public class SourceBean implements Serializable {

    /**
     * 是否为内部源
     */
    private final boolean internal;

    private final String name;

    private final String url;

    private final String desc;

    public SourceBean(String name, String url, String desc, boolean internal) {
        this.name = name;
        this.url = url;
        this.desc = desc;
        this.internal = internal;
    }


    @NonNull
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getDesc() {
        if (TextUtils.isEmpty(desc))
            return url;
        return desc;
    }

    public boolean isInternal() {
        return internal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceBean bean = (SourceBean) o;
        return Objects.equals(name, bean.name) &&
                Objects.equals(url, bean.url) &&
                Objects.equals(desc, bean.desc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url, desc);
    }


    @NonNull
    @Override
    public String toString() {
        return "SourceBean{" +
                "internal=" + internal +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", desc='" + desc + '\'' +
                '}';
    }

}

