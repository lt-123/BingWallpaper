package xyz.liut.bingwallpaper;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

/**
 * RobolectricTestRunner
 * <p>
 * Create by liut on 2020/11/4
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public abstract class BaseTestCase {

    protected final Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
    }

}
