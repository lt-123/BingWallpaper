package xyz.liut.bingwallpaper.v3.permissions;

import org.junit.Assert;
import org.junit.Test;

/**
 * 通知权限判断测试。
 */
public class NotificationPermissionHelperTest {

    @Test
    public void postNotificationsIsRuntimePermissionFromAndroid13() {
        Assert.assertFalse(NotificationPermissionHelper.requiresRuntimePermission(32));
        Assert.assertTrue(NotificationPermissionHelper.requiresRuntimePermission(33));
        Assert.assertTrue(NotificationPermissionHelper.requiresRuntimePermission(36));
    }
}
