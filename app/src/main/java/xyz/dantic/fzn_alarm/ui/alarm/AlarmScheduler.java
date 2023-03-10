package xyz.dantic.fzn_alarm.ui.alarm;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import xyz.dantic.fzn_alarm.services.TriggerAlarmReceiver;
import xyz.dantic.fzn_alarm.ui.settings.SettingsViewModel;

/**
 * Class responsible for scheduling alarms
 */
public class AlarmScheduler {

    public static final int REMINDER_COUNTER = -1;
    public static final int GAP_BETWEEN_GONGS = 5;

    private Context context;
    private AlarmManager alarmManager;

    private int reminderTimeId;
    private boolean initializeOutsideOfApplication;
    private boolean remindersOnly;

    public AlarmScheduler(Application application) {
        this(application.getApplicationContext());
    }

    public AlarmScheduler(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        initializeOutsideOfApplication = false;
        remindersOnly = false;
    }

    /**
     * Necessary to be called when scheduling alarms outside of the normal application lifecycle,
     * such as in the timezone and boot receivers
     */
    public void initializeOutsideOfApplication() {
        SharedPreferences settingsPreferences = context.getApplicationContext().
                getSharedPreferences(SettingsViewModel.SettingsFileName, Context.MODE_PRIVATE);
        reminderTimeId = settingsPreferences.getInt(SettingsViewModel.FILE_NAME_REMINDER_TIME, 5);
        initializeOutsideOfApplication = true;
    }

    /**
     * Schedules all alarms in a list to the system's alarm manager (performed in separate thread)
     * @param alarmList List of alarms to be saved
     */
    public void scheduleAlarmList(ArrayList<Alarm> alarmList) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int position = 0;
                for (Alarm alarm : alarmList) {
                    if (alarm.isOn()) {
                        scheduleAlarm(position, alarm);
                    }
                    position++;
                }
            }
        }).start();
    }

    /**
     * Sets whether only reminder alarms should be scheduled during future scheduling
     * (This means currently playing alarms aren't rescheduled)
     * @param value The alarm schedule's schedule-alarms-only status
     */
    public void setRemindersOnly(boolean value) {
        remindersOnly = value;
    }

    /**
     * Schedule a snoozed alarm which will be set for the following day
     * (can be used when outside the normal application process)
     * @param position The position of the alarm to be rescheduled
     */
    public void scheduleSnoozedAlarm(int position) {
        initializeOutsideOfApplication();
        scheduleAlarm(position, null, true);
    }

    /**
     * Schedules an alarm - but if scheduling outside the application,
     * initializeOutsideOfApplication MUST be called first
     * @param position The ID of the alarm given by its ordered position in the list
     * @param alarm The alarm to schedule (can be left null, which will pull alarm from file)
     */
    public void scheduleAlarm(int position, Alarm alarm) {
        if (!initializeOutsideOfApplication) reminderTimeId = SettingsViewModel.getReminderTimeId();
        scheduleAlarm(position, alarm, false);
    }

    /**
     * Schedules an alarm - but if scheduling outside the application,
     * initializeOutsideOfApplication MUST be called first
     * @param position The ID of the alarm given by its ordered position in the list
     * @param alarm The alarm to schedule (can be left null, which will pull alarm from file)
     * @param snoozed Whether the alarm should be delayed by one day
     */
    private void scheduleAlarm(int position, Alarm alarm, boolean snoozed) {
        if (!remindersOnly) cancelAlarm(position);

        // If alarm is left null, the alarm will be pulled from file
        if (alarm == null) {
            alarm = (new AlarmFileSaver(context)).getAlarm(position);
            if (!alarm.isOn()) return; // Handles edge case where alarm added to an earlier position of another alarm that is currently playing
        }

        // Convert the alarm into calendar form
        int originalHour = alarm.getHour();
        int originalMinute = alarm.getMinute();
        GregorianCalendar calendar = (GregorianCalendar) GregorianCalendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, originalHour);
        calendar.set(Calendar.MINUTE, originalMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Delay by one day if required
        if (snoozed) calendar.add(Calendar.DATE, 1);

        // Convert the alarm's day of week into index form (0 for Monday and 6 for Sunday)
        int todayAsIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        // If alarm is before the current time today, set to next day (otherwise will instantly play)
        if (calendar.before(GregorianCalendar.getInstance())) {
            todayAsIndex++;
            calendar.add(Calendar.DATE, 1);
        }

        // Find next day/time with a valid alarm set (i.e. not "off")
        Alarm.Sound sound = Alarm.Sound.OFF;
        for (int i = 0; i < 7; i++) {
            sound = alarm.getSoundOnDay((todayAsIndex + i) % 7);
            if (sound != Alarm.Sound.OFF) break;
            else calendar.add(Calendar.DATE, 1);
        }
        // If didn't find a valid alarm
        if (sound == Alarm.Sound.OFF) {
            return;
        }

        // Create the intent encapsulating key data for alarm scheduling
        Intent intent = new Intent(context, TriggerAlarmReceiver.class);
        intent.setAction(TriggerAlarmReceiver.ACTION_SCHEDULE_ALARM);
        intent.putExtra(TriggerAlarmReceiver.EXTRA_POSITION, position);
        intent.putExtra(TriggerAlarmReceiver.EXTRA_SOUND_ON_DAY, sound.name());
        intent.putExtra(TriggerAlarmReceiver.EXTRA_TIME_IN_MILLIS, calendar.getTimeInMillis());
        intent.putExtra(TriggerAlarmReceiver.EXTRA_ORIGINAL_HOUR, originalHour);
        intent.putExtra(TriggerAlarmReceiver.EXTRA_ORIGINAL_MINUTE, originalMinute);

        // If a reminder is set, but that reminder is in the past and the alarm is upcoming,
        // then go straight to setting the first alarm rather than setting a reminder
        calendar.add(Calendar.MINUTE, -reminderTimeId);
        if (reminderTimeId > 0 && calendar.after(GregorianCalendar.getInstance())) {
//        if (reminderTimeId > 0 && calendar.after(GregorianCalendar.getInstance()) && !alarmRightBefore) {
            intent.putExtra(TriggerAlarmReceiver.EXTRA_COUNTER, REMINDER_COUNTER);
        }
        else {
            if (remindersOnly) return;
            intent.putExtra(TriggerAlarmReceiver.EXTRA_COUNTER, 0);
        }

        scheduleNextAlarmStage(intent);
    }

    /**
     * Schedules an alarm's specific stage
     * @param intent Intent containing:
     *               EXTRA_POSITION Alarm position
     *               EXTRA_COUNTER Counter representing the alarm's stage to be scheduled
     *               (-1 for the reminder stage, or 0, 1, 2, and 3 for gong stages)
     *               EXTRA_ORIGINAL_HOUR Alarm hour
     *               EXTRA_ORIGINAL_MINUTE Alarm minute
     *               EXTRA_SOUND_ON_DAY Sound set for the day
     *               EXTRA_TIME_IN_MILLIS Alarm time in milliseconds
     */
    public void scheduleNextAlarmStage(Intent intent) {
        int counter = intent.getIntExtra(TriggerAlarmReceiver.EXTRA_COUNTER, -1);
        // TODO this will change if custom alarm duration is implemented
        if (counter > 3) return; // Don't continue if all gongs have finished

        int position = intent.getIntExtra(TriggerAlarmReceiver.EXTRA_POSITION, -1);
//        Alarm.Sound sound = Alarm.Sound.valueOf(intent.getStringExtra(TriggerAlarmReceiver.EXTRA_SOUND_ON_DAY));
        long timeInMillis = intent.getLongExtra(TriggerAlarmReceiver.EXTRA_TIME_IN_MILLIS, -1);

        // Calculate the minutes offset from the alarm's original set time
        int minutesOffset = 0;
        if (counter == REMINDER_COUNTER) {
            minutesOffset = -reminderTimeId;
        } else if (counter >= 0) {
            minutesOffset = counter * GAP_BETWEEN_GONGS;
        }

        // Calculate the new time in milliseconds factoring in the minutes offset
        timeInMillis += minutesOffset * 60 * 1000; // Convert minutes to milliseconds

        // Each alarm has 5 request codes allotted to it
        int requestCode = position * 5 + counter + 1;
        final PendingIntent pIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE); // could/should be immutable?

        // For testing
//        Calendar testCalendar = new GregorianCalendar();
//        testCalendar.setTimeInMillis(timeInMillis);
//        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm");
//        String date = df.format(testCalendar.getTime());
//        Log.i("MYDEBUG", "scheduling " + intent.getStringExtra(TriggerAlarmReceiver.EXTRA_SOUND_ON_DAY)
//                + " alarm at " + date
//                + " at position " + intent.getIntExtra(TriggerAlarmReceiver.EXTRA_POSITION, -1)
//                + " counter " + intent.getIntExtra(TriggerAlarmReceiver.EXTRA_COUNTER, -1));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(timeInMillis, pIntent), pIntent);
        }
        else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pIntent);
        }
    }

    /**
     * Cancel an alarm from playing
     * @param position The ordered position of the alarm
     */
    public void cancelAlarm(int position) {
        for (int i = -1; i < 4; i++) {
            cancelAlarmStage(position, i);
        }
        // In case there is a notification for a currently playing alarm, clear it
        TriggerAlarmReceiver.clearNotification(context, position);
    }

    /**
     * Cancel a specific alarm stage from playing
     * @param position The ordered position of the alarm
     */
    public void cancelAlarmStage(int position, int stage) {
//        Log.i("MYDEBUG", "Cancelling alarm at position " + position + " stage " + stage);
        int requestCode = position * 5 + stage + 1;

        Intent intent = new Intent(context, TriggerAlarmReceiver.class);
        intent.setAction(TriggerAlarmReceiver.ACTION_SCHEDULE_ALARM);
        final PendingIntent pIntent = PendingIntent.getBroadcast(
                context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        alarmManager.cancel(pIntent);
    }

    public void cancelAllAlarms(int totalAlarms, boolean cancelRemindersOnly) {
        for (int i = 0; i < totalAlarms; i++) {
            if (cancelRemindersOnly) cancelAlarmStage(i, -1);
            else cancelAlarm(i);
        }
    }

    // TODO To check whether the permission is granted to your app, call canScheduleExactAlarms()
    // https://developer.android.com/training/scheduling/alarms#using-schedule-exact-permission
}
