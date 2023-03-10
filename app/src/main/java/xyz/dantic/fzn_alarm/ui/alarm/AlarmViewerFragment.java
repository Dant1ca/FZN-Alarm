package xyz.dantic.fzn_alarm.ui.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;

import xyz.dantic.fzn_alarm.R;
import xyz.dantic.fzn_alarm.databinding.FragmentAlarmViewerBinding;
import xyz.dantic.fzn_alarm.services.GongPlayerService;
import xyz.dantic.fzn_alarm.services.TimeZoneChangedReceiver;
import xyz.dantic.fzn_alarm.ui.settings.SettingsViewModel;

public class AlarmViewerFragment extends Fragment {

    private FragmentAlarmViewerBinding binding;
    AlarmViewModel alarmViewModel;
    SettingsViewModel settingsViewModel;

    public static final String ACTION_UPDATE_ALARM_VIEWER_LIST = "xyz.dantic.fzn_alarm.action.UPDATE_ALARM_VIEWER_LIST";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlarmViewerBinding.inflate(inflater, container, false);

        alarmViewModel =
                new ViewModelProvider(requireActivity()).get(AlarmViewModel.class);
        settingsViewModel =
                new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        RecyclerView alarmRecyclerView = (RecyclerView) binding.recyclerViewAlarm;
        AlarmRecyclerViewAdapter adapter = new AlarmRecyclerViewAdapter(alarmViewModel, this);
        // Attach the adapter to the recyclerview to populate items
        alarmRecyclerView.setAdapter(adapter);
        // Set layout manager to position the items
        alarmRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Add alarm button listener
        binding.fabAddAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AlarmContainerFragment) getParentFragment()).insertAlarmEditorFragment(-1);
            }
        });

        // Click listener for RecyclerView items set within AlarmRecyclerViewAdapter
    }

    @Override
    public void onResume() {
        super.onResume();
        registerAlarmListUpdatedReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterAlarmListUpdatedReceiver();
    }

    /**
     * Broadcast receiver for when AlarmViewModel has finished updating its alarm list after
     * a timezone change was registered
     */
    private BroadcastReceiver alarmListUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            RecyclerView alarmRecyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view_alarm);
            // Essentially just recreating the recycler view
            AlarmRecyclerViewAdapter adapter = new AlarmRecyclerViewAdapter(alarmViewModel, AlarmViewerFragment.this);
            // Attach the adapter to the recyclerview to populate items
            alarmRecyclerView.setAdapter(adapter);
            // Set layout manager to position the items
            alarmRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
    };

    private void registerAlarmListUpdatedReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_UPDATE_ALARM_VIEWER_LIST);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(alarmListUpdatedReceiver, filter);
    }

    private void unregisterAlarmListUpdatedReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(alarmListUpdatedReceiver);
    }
}
