package xyz.dantic.fzn_alarm.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import xyz.dantic.fzn_alarm.MainActivity;
import xyz.dantic.fzn_alarm.R;
import xyz.dantic.fzn_alarm.ui.alarm.Alarm;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmPlayer;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmScheduler;

// TODO allow snooze action from notification
public class TriggerAlarmReceiver extends BroadcastReceiver {

    public static final String NOTIFICATION_CHANNEL_ID = "xyz.dantic.fzn_alarm.alarm_channel_id";
    public static final String ACTION_SCHEDULE_ALARM = "xyz.dantic.fzn_alarm.services.action.SCHEDULE_ALARM";
    public static final String ACTION_SNOOZE = "xyz.dantic.fzn_alarm.services.action.SNOOZE";
    public static final String EXTRA_POSITION = "xyz.dantic.fzn_alarm.alarm.extra.POSITION";
    public static final String EXTRA_COUNTER = "xyz.dantic.fzn_alarm.alarm.extra.COUNTER";
    public static final String EXTRA_ORIGINAL_HOUR = "xyz.dantic.fzn_alarm.alarm.extra.ORIGINAL_HOUR";
    public static final String EXTRA_ORIGINAL_MINUTE = "xyz.dantic.fzn_alarm.alarm.extra.ORIGINAL_MINUTE";
    public static final String EXTRA_SOUND_ON_DAY = "xyz.dantic.fzn_alarm.alarm.extra.SOUND_ON_DAY";
    public static final String EXTRA_TIME_IN_MILLIS = "xyz.dantic.fzn_alarm.alarm.extra.TIME_IN_MILLIS";

    private Context context;
    private int position, originalHour, originalMinute, counter;
    private Alarm.Sound sound;

    public static volatile AlarmPlayer alarmPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        switch (intent.getAction()) {
            case ACTION_SCHEDULE_ALARM:
                position = intent.getIntExtra(EXTRA_POSITION, -1);
                counter = intent.getIntExtra(EXTRA_COUNTER, -1);
                originalHour = intent.getIntExtra(EXTRA_ORIGINAL_HOUR, -1);
                originalMinute = intent.getIntExtra(EXTRA_ORIGINAL_MINUTE, -1);
                sound = Alarm.Sound.valueOf(intent.getStringExtra(EXTRA_SOUND_ON_DAY));
                if (counter == AlarmScheduler.REMINDER_COUNTER) sound = Alarm.Sound.NOTIF;

                alarmPlayer = new AlarmPlayer(context, new MediaPlayer());
                alarmPlayer.playSound(sound);

                if (counter < 3) setupNotification();
                else clearNotification(context, position);

                intent.putExtra(EXTRA_COUNTER, counter + 1);
                // Schedule new alarm if counter hasn't exceeded the last gong hit
                if (counter < 3) {
                    (new AlarmScheduler(context)).scheduleNextAlarmStage(intent);
                }
                else if (counter == 3) {//(counter >= 3) { // reschedule original alarm the following day
                    (new AlarmScheduler(context)).scheduleAlarm(position, null);
                }
                break;

            case ACTION_SNOOZE:
                alarmPlayer.stop();
                position = intent.getIntExtra(EXTRA_POSITION, -1);
                // Remove notification
                clearNotification(context, position);
                // Remove alarm
                AlarmScheduler alarmScheduler = new AlarmScheduler(context);
                // Reschedule alarm
                alarmScheduler.scheduleSnoozedAlarm(position);
                break;

            default:
                try {
                    throw new Exception("Error: TriggerAlarmReceiver action not supplied");
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    /**
     * Helper method to play a notification/gong sound and display a notification
     */
    private void setupNotification() {
        // Build and display notification
        NotificationCompat.Builder mBuilder = null;
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Oreo onwards requires notification channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        }
        else {
            mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                    .setPriority(NotificationCompat.PRIORITY_MAX);
        }

        mBuilder.setDefaults(0); // Remove sound

        mBuilder.setSmallIcon(R.drawable.ic_gong_solid_color);

        Resources resources = context.getResources();
        if (counter == AlarmScheduler.REMINDER_COUNTER) {
            mBuilder.setContentTitle(resources.getString(R.string.reminder))
                    .setContentText(resources.getString(R.string.upcoming_alarm_set_for) + " "
                            + String.format("%02d", originalHour) + ":" + String.format("%02d", originalMinute));
        } else {
            int nextAlarmInMinutes = originalHour * 60 + originalMinute + (counter+1) * AlarmScheduler.GAP_BETWEEN_GONGS;
            int hour = (nextAlarmInMinutes / 60) % 24;
            int minute = nextAlarmInMinutes % 60;
            mBuilder.setContentTitle(resources.getString(R.string.alarm))
                    .setContentText(resources.getString(R.string.next_alarm_at) + " "
                            + String.format("%02d", hour) + ":" + String.format("%02d", minute));

        }

        // Set up intent to return to activity if notification is touched
        Intent returnToActivityIntent = new Intent(context, MainActivity.class);
        returnToActivityIntent.putExtra(MainActivity.EXTRA_START_POINT, MainActivity.ALARM_FRAGMENT_START_POINT);
        returnToActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, returnToActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        mBuilder.setContentIntent(pendingIntent);

        // Add button to ACTION_SNOOZE alarm
        Intent snoozeIntent = new Intent(context, TriggerAlarmReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra(EXTRA_POSITION, position);
        PendingIntent snoozePendingIntent =
                PendingIntent.getBroadcast(context, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mBuilder.addAction(R.drawable.ic_gong_solid_color, resources.getString(R.string.snooze_message), snoozePendingIntent);

        // Make notification ID equal to position
        mNotificationManager.notify(position, mBuilder.build());
    }

    public static void clearNotification(Context context, int position) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(ns);
        notificationManager.cancel(position);
    }

}


/**
 * My previous issue using JobIntentService:
 * "The Alarm Manager holds a CPU wake lock as long as the alarm receiver's onReceive() method is executing.
 * This guarantees that the phone will not sleep until you have finished handling the broadcast. Once onReceive()
 * returns, the Alarm Manager releases this wake lock. This means that the phone will in some cases sleep as soon
 * as your onReceive() method completes. If your alarm receiver called Context.startService(), it is possible that
 * the phone will sleep before the requested service is launched. To prevent this, your BroadcastReceiver and Service
 * will need to implement a separate wake lock policy to ensure that the phone continues running until the service becomes available."
 * https://developer.android.com/reference/android/app/AlarmManager
 * Conclusion: learn to read documentation
 */