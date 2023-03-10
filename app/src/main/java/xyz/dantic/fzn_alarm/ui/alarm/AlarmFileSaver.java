package xyz.dantic.fzn_alarm.ui.alarm;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;

import xyz.dantic.fzn_alarm.services.TimeZoneHelper;

/**
 * Responsible for saving and retrieving alarms from file
 */
public class AlarmFileSaver {

    private Context context;
    private SharedPreferences alarmPreferences;

    private boolean newLaunch = false;

    private final String
            alarmPreferencesFileName = "AlarmPreferences", // rename to fzn_alarm.alarm_preferences
            alarmNumberName = "alarm_number",
            onName = "on",
            hourName = "hour",
            minuteName = "minute";

    private final String[] dayNames = new String[]{
            "monday_sound",
            "tuesday_sound",
            "wednesday_sound",
            "thursday_sound",
            "friday_sound",
            "saturday_sound",
            "sunday_sound"};

    private final Alarm.Sound defaultSound = Alarm.Sound.GONG;

    public AlarmFileSaver(Application application) {
        context = application.getApplicationContext();
        alarmPreferences = context.getSharedPreferences(alarmPreferencesFileName, Context.MODE_PRIVATE);
    }

    public AlarmFileSaver(Context context) {
        this.context = context;
        alarmPreferences = context.getSharedPreferences(alarmPreferencesFileName, Context.MODE_PRIVATE);
    }

    /**
     * Saves alarm list to persistent storage and schedules the alarm with the alarm manager
     * @param alarmList List of alarms to save
     */
    public void saveAlarmList(ArrayList<Alarm> alarmList) {
        SharedPreferences.Editor editor = alarmPreferences.edit();
        int position = 0;
        for (Alarm alarm : alarmList) {
            saveAlarm(alarm, position, editor);
            position++;
        }
        editor.putInt(alarmNumberName, position).apply();
    }

    /**
     * Save alarm to file
     * @param alarm Alarm to save
     * @param position Position to save to
     * @param editor (Optional) shared preferences editor to save into
     */
    public void saveAlarm(Alarm alarm, int position, SharedPreferences.Editor editor) {
        boolean saveImmediately = false;
        if (editor == null) {
            editor = alarmPreferences.edit();
            saveImmediately = true;
        }
        String positionString = "" + position;

        editor.putBoolean(positionString + onName, alarm.isOn())
                .putInt(positionString + hourName, alarm.getHour())
                .putInt(positionString + minuteName, alarm.getMinute());

        for (int i = 0; i < 7; i++) {
            editor.putString(positionString + dayNames[i], alarm.getSoundOnDay(i).name());
        }

        if (saveImmediately) {
            editor.apply();
        }
    }

    /**
     * Save alarm to file
     * @param alarm Alarm to save
     * @param position Position to save to
     */
    public void saveAlarm(Alarm alarm, int position) {
        saveAlarm(alarm, position, null);
    }

    /**
     * Save an alarm toggle to file
     * @param position Position to save to
     * @param checked Whether the alarm is on
     */
    public void saveAlarmToggle(int position, boolean checked) {
        alarmPreferences.edit().putBoolean("" + position + onName, checked).apply();
    }

    /**
     * Delete an alarm from file
     * @param position Position to delete
     * @param alarmList List of alarms
     */
    public void deleteAlarm(int position, ArrayList<Alarm> alarmList) {
        SharedPreferences.Editor editor = alarmPreferences.edit();

        // Move all saved alarms one position down
        for (int i = position; i < alarmList.size() - 1; i++) {
            saveAlarm(alarmList.get(i + 1), i, editor);
        }
        editor.putInt(alarmNumberName, alarmList.size() - 1).apply();
    }

    /**
     * Gets the list of alarms saved in SharedPreferences
     * @return ArrayList of alarms
     */
    public ArrayList<Alarm> getAlarmList() {
        ArrayList<Alarm> alarmList = new ArrayList<>();

        int alarmNumber = alarmPreferences.getInt(alarmNumberName, 4); // default 4 alarms

        for (int i = 0; i < alarmNumber; i++) {
            alarmList.add(getAlarm(i));
        }

        if (newLaunch) Collections.sort(alarmList);

        return alarmList;
    }

    /**
     * Helper method to extract alarm data from SharedPreferences
     * @param position Alarm's ordered position
     * @return Alarm object with data from file
     */
    public Alarm getAlarm(int position) {
        String positionString = "" + position;
        boolean on;
        int hour, minute;
        int initializerHour = 15;
        int initializerMinute = 0;
        Alarm.Sound[] sound = new Alarm.Sound[7];

        on = alarmPreferences.getBoolean(positionString + onName, true);

        for (int i = 0; i < 7; i++) {
            sound[i] = Alarm.Sound.valueOf(alarmPreferences.getString(positionString + dayNames[i], defaultSound.name()));
        }

        hour = alarmPreferences.getInt(positionString + hourName, -1);
        if (hour == -1) { // empty value, need to initialise
            newLaunch = true;
            switch (position) { // TODO: need to set based on timezone
                case 0: initializerHour = 5; break;
                case 1: initializerHour = 11; break;
                case 2: initializerHour = 17; break;
                case 3: initializerHour = 23; break;
            }
            initializerMinute = 55;

            // Convert the expected Beijing time (e.g. 11:55) into current timezone
            GregorianCalendar defaultTime = TimeZoneHelper.generateCalendar(initializerHour, initializerMinute);
            TimeZoneHelper.convertTimeZoneFromChina(defaultTime);
            hour = defaultTime.get(Calendar.HOUR_OF_DAY);
            initializerMinute = defaultTime.get(Calendar.MINUTE);
        }
        minute = alarmPreferences.getInt(positionString + minuteName, initializerMinute);

        return new Alarm(on, hour, minute, sound);
    }
}
