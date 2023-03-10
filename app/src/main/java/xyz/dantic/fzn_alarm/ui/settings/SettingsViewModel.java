package xyz.dantic.fzn_alarm.ui.settings;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.sql.Time;
import java.util.TimeZone;

import xyz.dantic.fzn_alarm.R;
import xyz.dantic.fzn_alarm.services.TimeZoneChangedReceiver;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmFileSaver;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmViewerFragment;

public class SettingsViewModel extends AndroidViewModel {

    public final static String SettingsFileName = "fzn_alarm.settings";
    Application application;
    SharedPreferences settingsPreferences;

    public final static String FILE_NAME_REMINDER_TIME = "reminder_time";
    public final static String FILE_NAME_AUTO_TIMEZONE_UPDATE = "auto_timezone_update";
    public final static String FILE_NAME_TIMEZONE_ID = "auto_timezone_id";
    public final static String FILE_NAME_ALARM_VOLUME = "alarm_volume_new";

    private static volatile int reminderTimeId;
    public static volatile String[] REMINDER_TIME_VALUES;

    private static volatile float alarmVolume;
    private static volatile String currentTimezoneId;
    private static volatile boolean autoTimezoneUpdate;

    public SettingsViewModel(Application application) {
        super(application);
        this.application = application;
        settingsPreferences = application.getApplicationContext().getSharedPreferences(SettingsFileName, Context.MODE_PRIVATE);

        getSettingsFromFile();
        registerTimeZoneChangedReceiver();
    }

    private void getSettingsFromFile() {
//        settingsPreferences.edit().clear().apply(); //todo for testing

        reminderTimeId = settingsPreferences.getInt(FILE_NAME_REMINDER_TIME, 5);
        REMINDER_TIME_VALUES = application.getResources().getStringArray(R.array.reminder_times);

        currentTimezoneId = settingsPreferences.getString(FILE_NAME_TIMEZONE_ID, generateAndSaveDefaultTimeZone());
        autoTimezoneUpdate = settingsPreferences.getBoolean(FILE_NAME_AUTO_TIMEZONE_UPDATE, true);

        alarmVolume = settingsPreferences.getFloat(FILE_NAME_ALARM_VOLUME, 50f);
    }

    /**
     * Returns and saves the current timezone
     * (Otherwise never gets initialized)
     * @return Current timezone ID
     */
    private String generateAndSaveDefaultTimeZone() {
        String defaultTimeZoneId = TimeZone.getDefault().getID();
        settingsPreferences.edit().putString(FILE_NAME_TIMEZONE_ID, defaultTimeZoneId).apply();
        return defaultTimeZoneId;
    }

    public static boolean getAutoTimezoneUpdate() {
        return autoTimezoneUpdate;
    }

    public void toggleAutoTimezoneUpdate() {
        autoTimezoneUpdate = !autoTimezoneUpdate;
        settingsPreferences.edit().putBoolean(FILE_NAME_AUTO_TIMEZONE_UPDATE, autoTimezoneUpdate).apply();
    }

    public static String getCurrentTimezoneId() {
        return currentTimezoneId;
    }

    public void setCurrentTimezoneId(String newId) {
        currentTimezoneId = newId;
        settingsPreferences.edit().putString(FILE_NAME_TIMEZONE_ID, newId).apply(); }

    public static int getReminderTimeId() {
        return reminderTimeId;
    }

    public static String getReminderTime() {
        return REMINDER_TIME_VALUES[reminderTimeId];
    }

    public void setReminderTimeId(int newReminderTimeId) {
        reminderTimeId = newReminderTimeId;
        settingsPreferences.edit().putInt(FILE_NAME_REMINDER_TIME, newReminderTimeId).apply();
    }

    public static float getAlarmVolume() {
        return alarmVolume;
    }

    public void setAlarmVolume(float newAlarmVolume) {
        alarmVolume = newAlarmVolume;
        settingsPreferences.edit().putFloat(FILE_NAME_ALARM_VOLUME, newAlarmVolume).apply();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        unregisterTimeZoneChangedReceiver();
    }

    /**
     * Broadcast receiver for when timezone has changed
     */
    private BroadcastReceiver timeZoneChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentTimezoneId = TimeZone.getDefault().getID();
//            setCurrentTimezoneId(TimeZone.getDefault().getID());
        }
    };

    private void registerTimeZoneChangedReceiver() {
        IntentFilter filter = new IntentFilter(TimeZoneChangedReceiver.ACTION_TIMEZONE_UPDATE_COMPLETE);
        LocalBroadcastManager.getInstance(getApplication().getApplicationContext()).registerReceiver(timeZoneChangedReceiver, filter);
    }

    private void unregisterTimeZoneChangedReceiver() {
        LocalBroadcastManager.getInstance(getApplication().getApplicationContext()).unregisterReceiver(timeZoneChangedReceiver);
    }
}