package xyz.liut.bingwallpaper.v3.schedule;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

/**
 * 定时管理测试。
 */
public class ScheduleManagerTest {

    @Test
    public void nextDelayMinutesReturnsTargetLaterToday() {
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.JUNE, 14, 8, 0, 1);
        now.set(Calendar.MILLISECOND, 0);

        long delayMinutes = ScheduleManager.nextDelayMinutes(now, 9, 0);

        Assert.assertEquals(60, delayMinutes);
    }

    @Test
    public void nextDelayMinutesReturnsTargetTomorrowWhenAlreadyPassedToday() {
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.JUNE, 14, 9, 0, 1);
        now.set(Calendar.MILLISECOND, 0);

        long delayMinutes = ScheduleManager.nextDelayMinutes(now, 9, 0);

        Assert.assertEquals(24 * 60, delayMinutes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nextDelayMinutesRejectsInvalidHour() {
        Calendar now = Calendar.getInstance();

        ScheduleManager.nextDelayMinutes(now, 24, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nextDelayMinutesRejectsInvalidMinute() {
        Calendar now = Calendar.getInstance();

        ScheduleManager.nextDelayMinutes(now, 8, 60);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateScheduleOptionsRejectsNegativeDelayMinute() {
        ScheduleManager.validateScheduleOptions(8, 30, -1);
    }
}
