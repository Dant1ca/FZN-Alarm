package xyz.dantic.fzn_alarm.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import xyz.dantic.fzn_alarm.ui.alarm.AlarmFileSaver;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmScheduler;

public class RebootReceiver extends BroadcastReceiver {
    // Reschedules alarms on system reboot

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmScheduler alarmScheduler = new AlarmScheduler(context.getApplicationContext());
        AlarmFileSaver alarmFileSaver = new AlarmFileSaver(context.getApplicationContext());
        alarmScheduler.initializeOutsideOfApplication();
        alarmScheduler.scheduleAlarmList(alarmFileSaver.getAlarmList());
    }
}