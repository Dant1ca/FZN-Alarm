package xyz.dantic.fzn_alarm.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import xyz.dantic.fzn_alarm.ui.alarm.Alarm;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmFileSaver;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmScheduler;
import xyz.dantic.fzn_alarm.ui.settings.SettingsViewModel;

/**
 * Updates and reschedules alarms on TimeZone change
 */
public class TimeZoneChangedReceiver extends BroadcastReceiver {

    public static final String ACTION_TIMEZONE_UPDATE_COMPLETE = "xyz.dantic.fzn_alarm.services.action.TIMEZONE_UPDATE_COMPLETE";
    public static final String ACTION_MANUAL_UPDATE_TIMEZONE = "xyz.dantic.fzn_alarm.action.MANUAL_UPDATE_TIMEZONE";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settingsPreferences =
                context.getApplicationContext().getSharedPreferences(SettingsViewModel.SettingsFileName, Context.MODE_PRIVATE);
        boolean autoTimezoneUpdate = settingsPreferences.getBoolean(SettingsViewModel.FILE_NAME_AUTO_TIMEZONE_UPDATE, true);

        String oldTimeZoneId = settingsPreferences.getString(SettingsViewModel.FILE_NAME_TIMEZONE_ID, TimeZone.getDefault().getID());
        String newTimeZoneId = TimeZone.getDefault().getID();

        // Only update if (auto update is on OR manual update has been called) AND the timezone has changed
        if ((autoTimezoneUpdate || intent.getAction().equals(ACTION_MANUAL_UPDATE_TIMEZONE))
            && !oldTimeZoneId.equals(newTimeZoneId)) {

            // Save new timezone
            settingsPreferences.edit().putString(SettingsViewModel.FILE_NAME_TIMEZONE_ID, newTimeZoneId).apply();

            // Get alarm list from file
            AlarmFileSaver alarmFileSaver = new AlarmFileSaver(context);
            ArrayList<Alarm> alarmList = alarmFileSaver.getAlarmList();

            // Convert each alarm time into the new timezone
            for (Alarm alarm : alarmList) {
                GregorianCalendar alarmAsCalendar = TimeZoneHelper.generateCalendar(alarm.getHour(), alarm.getMinute());
                TimeZoneHelper.convertTimeZone(alarmAsCalendar, oldTimeZoneId, newTimeZoneId);
                alarm.setHour(alarmAsCalendar.get(Calendar.HOUR_OF_DAY));
                alarm.setMinute(alarmAsCalendar.get(Calendar.MINUTE));
            }

            // Save the alarm list to file
            Collections.sort(alarmList);
            alarmFileSaver.saveAlarmList(alarmList);

            // Reschedule the alarms
            AlarmScheduler alarmScheduler = new AlarmScheduler(context.getApplicationContext());
            alarmScheduler.initializeOutsideOfApplication(); // Necessary

            // Needed for edge case when changing timezone
            // i.e. following scenario: 5:55pm (off) and 11.55pm (on) alarms in Perth become
            // 1:55am and 7:55pm alarms in Sydney.
            // When changing back to Perth, the first alarm position never gets cancelled
            alarmScheduler.cancelAllAlarms(alarmList.size(), true);

            alarmScheduler.scheduleAlarmList(alarmList);

            // Broadcast that the alarms have been updated, which will be received by
            // SettingsViewModel, SettingsFragment, and AlarmViewModel
            Intent forceUpdateAlarmsIntent = new Intent(ACTION_TIMEZONE_UPDATE_COMPLETE);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
            localBroadcastManager.sendBroadcast(forceUpdateAlarmsIntent);
        }
    }

}