package xyz.dantic.fzn_alarm.services;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import xyz.dantic.fzn_alarm.ui.alarm.Alarm;

/**
 *  Static helper class for dealing with timezone conversions
 */
public class TimeZoneHelper {

    private static final String CHINA_TIME_ZONE_ID = "Asia/Shanghai";

    /**
     * Creates a calendar for a given time of day
     * @param hour The hour of the day
     * @param minute The minute of the day
     * @return GregorianCalendar representation of the time
     */
    public static GregorianCalendar generateCalendar(int hour, int minute) {
        GregorianCalendar calendar = (GregorianCalendar) GregorianCalendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);
        return calendar;
    }

    /**
     * Generates an offset between a timezone and China e.g. "+8:00" or "-3:30"
     * @param timeZoneId The timezone ID, as defined by the TimeZone class
     * @return
     */
    public static String getStringOffsetWithChina(String timeZoneId) {
        GregorianCalendar localTime = (GregorianCalendar) GregorianCalendar.getInstance();
        TimeZone currentTimeZone = TimeZone.getTimeZone(timeZoneId);
        TimeZone chinaTimeZone = TimeZone.getTimeZone(CHINA_TIME_ZONE_ID);
        int timeZoneDifference =
                currentTimeZone.getOffset(localTime.getTimeInMillis()) - chinaTimeZone.getOffset(localTime.getTimeInMillis());

        timeZoneDifference = timeZoneDifference / 60000; // convert to minutes
        int hours = timeZoneDifference / 60;
        int minutes = timeZoneDifference % 60;

        String add = "+";
        if (timeZoneDifference < 0) {
            add = "";
            minutes = -minutes;
        }

        return String.format(add + "%02d:%02d", hours, minutes);
    }

    /**
     * Converts a calendar to a new timezone
     * @param originalTime The original calendar to be converted
     * @param oldTimeZoneId The original timezone ID to be converted from
     * @param newTimeZoneId The new timezone ID to be converted to
     */
    public static void convertTimeZone(GregorianCalendar originalTime, String oldTimeZoneId, String newTimeZoneId) {
        TimeZone oldTimeZone = TimeZone.getTimeZone(oldTimeZoneId);
        TimeZone newTimeZone = TimeZone.getTimeZone(newTimeZoneId);
        GregorianCalendar localTime = (GregorianCalendar) GregorianCalendar.getInstance();

        // Local time only needed for getOffset to determine whether daylight savings apply
        int timeZoneDifference =
                newTimeZone.getOffset(localTime.getTimeInMillis()) - oldTimeZone.getOffset(localTime.getTimeInMillis());
        // An alternative that doesn't use local time is getRawOffset, but this doesn't take daylight savings into account

        originalTime.setTimeInMillis(originalTime.getTimeInMillis() + timeZoneDifference);
    }

    /**
     * Converts a calendar from China timezone to the system's default timezone
     * @param originalTime The original calendar to be converted
     */
    public static void convertTimeZoneFromChina(GregorianCalendar originalTime) {
        convertTimeZone(originalTime, CHINA_TIME_ZONE_ID, TimeZone.getDefault().getID());
    }
}
