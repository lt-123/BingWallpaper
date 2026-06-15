package xyz.liut.bingwallpaper.v3.schedule;

import android.app.job.JobInfo;
import android.content.ComponentName;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import xyz.liut.bingwallpaper.BaseTestCase;

/**
 * 定时管理测试。
 */
public class ScheduleManagerTest extends BaseTestCase {

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

    @Test
    public void parseTimedJobsSkipsInvalidTimesAndKeepsValidOnes() {
        List<int[]> jobs = ScheduleManager.parseTimedJobs(Arrays.asList("07:30", "bad", "24:00", "18:05"));

        Assert.assertEquals(2, jobs.size());
        Assert.assertArrayEquals(new int[]{7, 30}, jobs.get(0));
        Assert.assertArrayEquals(new int[]{18, 5}, jobs.get(1));
    }

    @Test
    public void dailyJobIdsStayInsideOwnedRange() {
        Assert.assertEquals(30000, ScheduleManager.dailyJobIdForTest(0, 0));
        Assert.assertEquals(31439, ScheduleManager.dailyJobIdForTest(23, 59));
    }

    @Test
    public void scheduledJobWithDeadlineCanBeBuilt() {
        JobInfo jobInfo = ScheduleManager.createJobInfoForTest(
                new ComponentName("xyz.liut.bingwallpaper", "xyz.liut.bingwallpaper.AlarmJob"),
                30000,
                60,
                90);

        Assert.assertNotNull(jobInfo);
    }
}
