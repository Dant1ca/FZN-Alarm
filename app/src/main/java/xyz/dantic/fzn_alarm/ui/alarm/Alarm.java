package xyz.dantic.fzn_alarm.ui.alarm;


import android.content.Context;

import xyz.dantic.fzn_alarm.R;

public class Alarm implements Comparable<Alarm> {

    public enum Sound { GONG, NOTIF, OFF }
    public static final int FIFTEEN = 15, THIRTY = 30, FORTYFIVE = 45, SIXTY = 60;
//    public enum Duration {Fifteen, Thirty, FortyFive, Sixty} // unused

    private boolean on;
    private int hour; // change int to byte?
    private int minute;
    private Sound[] soundOnDay;
    private String gongDaysAsString;
    private String notifDaysAsString;
    private int duration; // unused

    /**
     * Construct alarm object with all parameters
     * @param on whether alarm is enabled
     * @param hour hours
     * @param minute minutes
     * @param soundOnDay array of sounds corresponding to each day of the week, must have a length of 7
     */
    public Alarm(boolean on, int hour, int minute, Sound[] soundOnDay) {
        this.on = on;
        this.hour = hour;
        this.minute = minute;
        this.duration = FIFTEEN;

        if (soundOnDay.length != 7) {throw new IllegalArgumentException(); }
        this.soundOnDay = new Sound[7];
        for (int i = 0; i < 7; i++) {
            this.soundOnDay[i] = soundOnDay[i];
        }
    }

    /**
     * Construct alarm object with the sound for each day of the week set to gong by default
     * @param on whether alarm is enabled
     * @param hour hours
     * @param minute minute
     */
    public Alarm(boolean on, int hour, int minute) {
        this.on = on;
        this.hour = hour;
        this.minute = minute;
        this.duration = FIFTEEN;
        this.soundOnDay = new Sound[]{Sound.GONG, Sound.GONG, Sound.GONG, Sound.GONG, Sound.GONG, Sound.GONG, Sound.GONG};
    }

    /**
     * Construct alarm object that is enabled and has the sound for each day of the week set to gong by default
     * @param hour hours
     * @param minute minute
     */
    public Alarm(int hour, int minute) {
        this.on = true;
        this.hour = hour;
        this.minute = minute;
        this.duration = FIFTEEN;
        this.soundOnDay = new Sound[]{Sound.GONG, Sound.GONG, Sound.GONG, Sound.GONG, Sound.GONG, Sound.GONG, Sound.GONG};
    }

    /**
     * Checks whether alarm is enabled
     * @return alarm enabled status
     */
    public boolean isOn() {
        return on;
    }

    /**
     * Sets alarm as enabled or disabled
     * @param on whether alarm is enabled or not
     */
    public void setOn(boolean on) {
        this.on = on;
    }

    /**
     * Gets alarm's hour
     * @return hour
     */
    public int getHour() {
        return hour;
    }

    /**
     * Sets alarm's hour
     * @param hour hour
     */
    public void setHour(int hour) {
        this.hour = hour;
    }

    /**
     * Gets alarm's minute
     * @return minute
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Sets alarm's minute
     * @param minute hour
     */
    public void setMinute(int minute) {
        this.minute = minute;
    }

    /**
     * Gets alarm time in minutes
     * @return total minutes
     */
    public int getTimeInMinutes() {return (int) (60 * hour + minute); }

    /**
     * Gets alarm duration
     * @return the duration
     */
    public int getDuration() { return duration; }

    /**
     * Sets alarm duration
     * @param duration the new duration
     */
    public void setDuration(int duration) {this.duration = duration; }

    /**
     * Gets array of sounds for each day of week
     * @return array of sounds
     */
    public Sound[] getSoundOnDayArray() {
        return soundOnDay;
    }

    /**
     * Gets sounds on a specific day
     * @param day day of the week
     * @return sound
     */
    public Sound getSoundOnDay(int day) {
        return soundOnDay[day];
    }

    /**
     * Set the array of sounds corresponding to each day of the week
     * @param soundOnDay array of sounds with a length of 7
     */
    public void setSoundOnDay(Sound[] soundOnDay) {
        if (soundOnDay.length != 7) throw new IllegalArgumentException();
        this.soundOnDay = soundOnDay;
    }

    /**
     * Set the sound for a particular day
     * @param day day of the week
     * @param sound new sound for the day
     */
    public void setSoundOnDay(int day, Sound sound) {
        soundOnDay[day] = sound;
    }

    /**
     * Get the time as a string representation of "hour : minute"
     * @return string representation of the time
     */
    public String getTimeAsString() {
        String result = "";
        if (hour < 10) result += "0";
        result += hour + " : ";
        if (minute < 10) result += "0";
        return result + minute;
    }

    public String getGongDaysAsString(Context context, boolean forceGenerate) {
        if (forceGenerate) generateSoundDaysForString(context);
        return gongDaysAsString;
    }

    /**
     * Get the days for which the gong is activated in readable form
     * E.g. "Weekdays", "Weekends", "Mon, Wed, Fri"
     * @param context context containing names defined for the language
     * @return string representation of the days on which the gong is enabled
     */
    public String getGongDaysAsString(Context context) {
        return getGongDaysAsString(context, true);
    }


    /**
     * Get the days for which the notification is activated in readable form
     * E.g. "Weekdays", "Weekends", "Mon, Wed, Fri"
     * @param context context containing names defined for the language
     * @return string representation of the days on which the notification is enabled
     */
    public String getNotifDaysAsString(Context context) {
        if (notifDaysAsString == null) generateSoundDaysForString(context);
        return notifDaysAsString;
    }

    /**
     * Checks whether the same sound is set for weekdays
     * @param sound the sound to check
     * @return whether the sound is set for each weekday
     */
    public boolean isSoundOnWeekdays(Sound sound) {
        return (soundOnDay[0] == sound &&
                soundOnDay[1] == sound &&
                soundOnDay[2] == sound &&
                soundOnDay[3] == sound &&
                soundOnDay[4] == sound);
    }

    /**
     * Checks whether the same sound is set for the weekend (Saturday and Sunday)
     * @param sound the sound to check
     * @return whether the sound is set for each weekend day
     */
    public boolean isSoundOnWeekends(Sound sound) {
        return (soundOnDay[5] == sound && soundOnDay[6] == sound);
    }

    /**
     * Creates duplicate alarm
     * @return New alarm with same settings
     */
    public Alarm clone() {
        return new Alarm(isOn(), getHour(), getMinute(), getSoundOnDayArray());
    }

    /**
     * Used primarily for sorting to indicate which alarm comes first time-wise
     * @param alarm Alarm to compare to
     * @return Positive if comparing alarm is smaller, 0 if equal, and negative if bigger
     */
    @Override
    public int compareTo(Alarm alarm) {
        return this.getTimeInMinutes() - alarm.getTimeInMinutes();
    }

    /**
     * Private helper method used to generate strings for which days the gong and notification sounds are active.
     * E.g. "Mon, Tue, Wed, Sat" or "Every day" or "Weekdays" or "Weekends"
     * @param context Application context required for knowing which language's strings to use
     */
    private void generateSoundDaysForString(Context context) {

        String[] daysOfWeek = context.getResources().getStringArray(R.array.daysOfWeekShort);

        String gongResult = "", notifResult = "";
        boolean gongWeekdays = true, gongWeekends = true, notifWeekdays = true, notifWeekends = true;
        boolean gongOnAWeekday = false, gongOnAWeekend = false, notifOnAWeekday = false, notifOnAWeekend = false;

        for (int i = 0; i < 7; i++) {
            Sound soundOfTheDay = soundOnDay[i];
            if (soundOfTheDay == Sound.GONG) {
                gongResult += daysOfWeek[i] + ", ";
                if (i < 5) {
                    notifWeekdays = false;
                    gongOnAWeekday = true;
                }
                if (i >= 5) {
                    notifWeekends = false;
                    gongOnAWeekend = true;
                }
            } else if (soundOfTheDay == Sound.NOTIF) {
                notifResult += daysOfWeek[i] + ", ";
                if (i < 5) {
                    gongWeekdays = false;
                    notifOnAWeekday = true;
                }
                if (i >= 5) {
                    gongWeekends = false;
                    notifOnAWeekend = true;
                }
            } else {
                if (i < 5) {
                    gongWeekdays = false;
                    notifWeekdays = false;
                }
                if (i >= 5) {
                    gongWeekends = false;
                    notifWeekends = false;
                }
            }
        }

        if (gongWeekdays && gongWeekends) gongResult = context.getResources().getString(R.string.everyday);
        else if (gongWeekdays && !gongOnAWeekend) gongResult = context.getResources().getString(R.string.weekdays);
        else if (!gongOnAWeekday && gongWeekends) gongResult = context.getResources().getString(R.string.weekends);
        else if (!gongResult.equals("")) gongResult = gongResult.substring(0, gongResult.length() - 2); // Chops off final ", " chars

        if (notifWeekdays && notifWeekends) notifResult = context.getResources().getString(R.string.everyday);
        else if (notifWeekdays && !notifOnAWeekend) notifResult = context.getResources().getString(R.string.weekdays);
        else if (!notifOnAWeekday && notifWeekends) notifResult = context.getResources().getString(R.string.weekends);
        else if (!notifResult.equals("")) notifResult = notifResult.substring(0, notifResult.length() - 2); // Chops off final ", " chars

        gongDaysAsString = gongResult;
        notifDaysAsString = notifResult;
    }

    public String getReadableTime() {
        return "" + hour + ":" + minute;
    }

}
