package xyz.dantic.fzn_alarm.ui.alarm;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import xyz.dantic.fzn_alarm.R;
import xyz.dantic.fzn_alarm.services.TimeZoneChangedReceiver;


/**
 * AlarmViewModel handles the state of all alarms, including saving to file and scheduling
 */
public class AlarmViewModel extends AndroidViewModel {

    private ArrayList<Alarm> alarmList;
    private final MutableLiveData<Integer> currentAlarmPosition; // maybe LiveData not needed...

    private final AlarmFileSaver alarmFileSaver;
    private final AlarmScheduler alarmScheduler;

    public AlarmViewModel(Application application) { // change to context so that background thread can use too
        super(application);
        currentAlarmPosition = new MutableLiveData<>();
        currentAlarmPosition.setValue(-1);

        alarmFileSaver = new AlarmFileSaver(application);
        alarmScheduler = new AlarmScheduler(application.getApplicationContext());

        // Load up saved alarms
        alarmList = alarmFileSaver.getAlarmList();

        registerAlarmListUpdatedReceiver();
    }

    /**
     * Gets the list of alarms
     * @return List of alarms
     */
    public ArrayList<Alarm> getAlarmList() {
        return alarmList;
    }

    /**
     * Sets the currently selected alarm position
     * @param position The currently selected alarm position
     */
    public void setCurrentAlarmPosition(int position) {
        currentAlarmPosition.setValue(position);
    }

    /**
     * Gets the current alarm position
     */
    public int getCurrentAlarmPosition() {
        return currentAlarmPosition.getValue();
    }

    /**
     * Gets the currently selected alarm (setCurrentAlarmPosition must be called prior to this)
     * @return Currently selected alarm
     */
    public Alarm getAlarmSelected() {
        if (currentAlarmPosition.getValue() < 0) return null;
        return alarmList.get(currentAlarmPosition.getValue());
    }

    /**
     * Add a new alarm (gets automatically scheduled)
     * @param alarm Alarm to be added
     */
    public void addNewAlarm(Alarm alarm) {
        alarmList.add(alarm);
        Collections.sort(alarmList);
        alarmFileSaver.saveAlarmList(alarmList);
        alarmScheduler.scheduleAlarmList(alarmList);
    }

    /**
     * Replace an existing alarm at a position with a new one (used when editing an existing alarm)
     * @param alarm New alarm to be inserted into a position
     * @param position Position for the new alarm to be inserted into
     * @param withSort Whether the existing alarm list needs to be sorted
     */
    public void replaceAlarm(Alarm alarm, int position, boolean withSort) {
        alarmList.set(position, alarm);
        alarmScheduler.cancelAlarm(position); // Don't want old subalarms to continue playing

        if (withSort) {
            Collections.sort(alarmList);
            alarmFileSaver.saveAlarmList(alarmList);
            alarmScheduler.scheduleAlarmList(alarmList);
        } else {
            alarmFileSaver.saveAlarm(alarm, position);
            alarmScheduler.scheduleAlarm(position, alarm);
        }
    }

    /**
     * Replace alarm at current position
     * @param alarm
     */
    public void replaceCurrentAlarm(Alarm alarm) {
        replaceAlarm(alarm, getCurrentAlarmPosition(), true);
    }

    /**
     * Sets an alarm to be on or off
     * @param position Alarm at position to be set on or off
     * @param checked The alarm's new on or off status
     */
    public void toggleAlarm(int position, boolean checked) {
        Alarm alarm = alarmList.get(position);
        alarm.setOn(checked);
        alarmFileSaver.saveAlarmToggle(position, checked);
        if (checked) {
            alarmScheduler.scheduleAlarm(position, alarm);
        }
        else {
            alarmScheduler.cancelAlarm(position);
        }
    }

    /**
     * Delete an alarm
     * @param position Position of the alarm to be deleted
     */
    public void deleteAlarm(int position) {
        alarmFileSaver.deleteAlarm(position, alarmList);
        if (alarmList.get(position).isOn()) alarmScheduler.cancelAlarm(position);
        alarmList.remove(position);
        alarmScheduler.scheduleAlarmList(alarmList); // Reschedules alarms which will shift their positions one down
        alarmScheduler.cancelAlarm(alarmList.size()); // Everything moves down one, so previous alarm at last position must be cancelled
    }

    /**
     * Deleted currently selected alarm (setCurrentAlarmPosition must be called prior to this)
     */
    public void deleteCurrentAlarm() {
        deleteAlarm(currentAlarmPosition.getValue());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        unregisterAlarmListUpdatedReceiver();
    }

    /**
     * Broadcast receiver for when TimeZoneChangedReceiver has finished updating the alarm list,
     * and is sending a signal for fragments to update their views
     */
    private BroadcastReceiver alarmListUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AlarmFileSaver alarmFileSaver = new AlarmFileSaver(context);
            alarmList = alarmFileSaver.getAlarmList();

            // Broadcast for the AlarmViewerFragment to update its list
            Intent updateRecyclerView = new Intent(AlarmViewerFragment.ACTION_UPDATE_ALARM_VIEWER_LIST);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplication().getApplicationContext());
            localBroadcastManager.sendBroadcast(updateRecyclerView);
        }
    };

    private void registerAlarmListUpdatedReceiver() {
        IntentFilter filter = new IntentFilter(TimeZoneChangedReceiver.ACTION_TIMEZONE_UPDATE_COMPLETE);
        LocalBroadcastManager.getInstance(getApplication().getApplicationContext()).registerReceiver(alarmListUpdatedReceiver, filter);
    }

    private void unregisterAlarmListUpdatedReceiver() {
        LocalBroadcastManager.getInstance(getApplication().getApplicationContext()).unregisterReceiver(alarmListUpdatedReceiver);
    }
}