package xyz.dantic.fzn_alarm.ui.settings;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import java.util.TimeZone;

import xyz.dantic.fzn_alarm.R;
import xyz.dantic.fzn_alarm.databinding.FragmentSettingsBinding;
import xyz.dantic.fzn_alarm.services.TimeZoneChangedReceiver;
import xyz.dantic.fzn_alarm.services.TimeZoneHelper;
import xyz.dantic.fzn_alarm.services.TriggerAlarmReceiver;
import xyz.dantic.fzn_alarm.ui.alarm.Alarm;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmPlayer;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmScheduler;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmViewModel;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    SettingsViewModel settingsViewModel;
    AlarmViewModel alarmViewModel;
    AlarmPlayer testAlarmPlayer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        settingsViewModel =
                new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
        alarmViewModel =
                new ViewModelProvider(requireActivity()).get(AlarmViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.textAlarmReminderTime.setText(SettingsViewModel.getReminderTime());

        setTimezoneUpdateText();
        String currentTimeZoneId = SettingsViewModel.getCurrentTimezoneId();
        binding.textCity.setText(getTimezoneCity(currentTimeZoneId));
        binding.textTimezone.setText("(" + getResources().getString(R.string.beijing) + " "
                + TimeZoneHelper.getStringOffsetWithChina(currentTimeZoneId) + ")");

        binding.sliderVolume.setValueFrom(0);
        binding.sliderVolume.setValueTo(AlarmPlayer.MAX_VOLUME);
        binding.sliderVolume.setValue(SettingsViewModel.getAlarmVolume());

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Timezone listener
        binding.layoutTimezone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsViewModel.toggleAutoTimezoneUpdate();
                setTimezoneUpdateText();

                // If timezone auto update is now toggled on, and if the timezone has actually changed,
                // then must do a manual broadcast to run TimeZoneChangedReceiver
                String newTimeZoneId = TimeZone.getDefault().getID();
                if (SettingsViewModel.getAutoTimezoneUpdate() &&
                    !SettingsViewModel.getCurrentTimezoneId().equals(newTimeZoneId)) {
                    Intent manualUpdateTimezoneIntent = new Intent(TimeZoneChangedReceiver.ACTION_MANUAL_UPDATE_TIMEZONE);
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(requireContext());
                    localBroadcastManager.sendBroadcast(manualUpdateTimezoneIntent);
                    // Note: SettingsViewModel timezone id is not updated here because that is done by the
                    // TimeZoneChangerReceiver which needs the old value for conversion
                }
            }
        });

        // Set up test alarm player
        testAlarmPlayer = new AlarmPlayer(requireContext(), new MediaPlayer());

        binding.sliderVolume.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                // When slider released, save the value
                settingsViewModel.setAlarmVolume(slider.getValue());
            }
        });

        binding.sliderVolume.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                // Edge case where alarm happens to be playing right now
                if (TriggerAlarmReceiver.alarmPlayer != null) {
                    TriggerAlarmReceiver.alarmPlayer.setVolume(value);
                }
                testAlarmPlayer.setVolume(value);
            }
        });

        binding.sliderVolume.setLabelFormatter(new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return getResources().getString(R.string.muted);
                return ("" + (int) value + "%");
            }
        });

        binding.buttonTestAlarmVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testAlarmPlayer.playSound(Alarm.Sound.GONG);
            }
        });

        binding.layoutAlarmReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAlarmReminderDialog();
            }
        });

    }

    /**
     * Sets the timezone text to On or Off based on settings
     */
    private void setTimezoneUpdateText() {
        if (SettingsViewModel.getAutoTimezoneUpdate()) binding.textTimezoneUpdate.setText(getResources().getString(R.string.enabled));
        else binding.textTimezoneUpdate.setText(getResources().getString(R.string.disabled));
    }

    /**
     * Gets the city from the timezone ID
     * @param timeZoneId TimeZone ID
     * @return City
     */
    private String getTimezoneCity(String timeZoneId) {
        String city;

        int indexOfFirstCityChar = 0;
        for (int i = 0; i < timeZoneId.length(); i++) {
            if (timeZoneId.charAt(i) == '/') {
                indexOfFirstCityChar = i + 1;
                break;
            }
        }
        city = timeZoneId.substring(indexOfFirstCityChar);

        return city;
    }

    /**
     * Opens the diologue for adjusting reminder time
     */
    public void openAlarmReminderDialog() {
        final ConstraintLayout layout = (ConstraintLayout) getLayoutInflater().inflate(R.layout.alertdialog_alarm_reminder, null);
        NumberPicker numberPicker = (NumberPicker) layout.findViewById(R.id.numberpicker_reminder);

        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(SettingsViewModel.REMINDER_TIME_VALUES.length - 1);
        numberPicker.setDisplayedValues(SettingsViewModel.REMINDER_TIME_VALUES);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setValue(SettingsViewModel.getReminderTimeId());
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // Stops ability to select and type value

        final AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setPositiveButton(getResources().getString(R.string.save), null)
                .setNeutralButton(getResources().getString(R.string.cancel), null)
                .setView(layout)
                .create();
        alertDialog.show();

        // Set custom width
        int desiredWidthDP = 270;
        int widthPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, desiredWidthDP, getResources().getDisplayMetrics());
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(alertDialog.getWindow().getAttributes());
        layoutParams.width = widthPixels;
        alertDialog.getWindow().setAttributes(layoutParams);

        // Play with button visuals
        Button saveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button cancelButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        saveButton.setScaleX(1.1f);
        saveButton.setScaleY(1.1f);
        cancelButton.setScaleX(1.1f);
        cancelButton.setScaleY(1.1f);

        // Set button margins
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int desiredMarginDP = 5;
        int marginPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, desiredMarginDP, getResources().getDisplayMetrics());
        buttonParams.setMargins(marginPixels, desiredMarginDP, marginPixels, desiredMarginDP);
        saveButton.setLayoutParams(buttonParams);
        cancelButton.setLayoutParams(buttonParams);

        // Set up listeners
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsViewModel.setReminderTimeId(numberPicker.getValue());
                AlarmScheduler alarmScheduler = new AlarmScheduler(requireContext());
                alarmScheduler.setRemindersOnly(true); // Reschedule reminders only
                alarmScheduler.scheduleAlarmList(alarmViewModel.getAlarmList());
                ((TextView) getView().findViewById(R.id.text_alarm_reminder_time)).setText(SettingsViewModel.getReminderTime());
                // Sleep before dismissing for better UI feel
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(120);
                        alertDialog.dismiss();
                    }
                }).start();
            }
        });

        alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Sleep before dismissing for better UI feel
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(120);
                        alertDialog.dismiss();
                    }
                }).start();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        registerTimeZoneChangedReceiver();
        registerManualTimeZoneUpdateReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterTimeZoneChangedReceiver();
        unregisterManualTimeZoneUpdateReceiver();
    }

    /**
     * Must register TimeZoneChangedReceiver here for it to be triggered when broadcasting a manual update
     */
    private TimeZoneChangedReceiver timeZoneChangedReceiver = new TimeZoneChangedReceiver();

    private void registerManualTimeZoneUpdateReceiver() {
        IntentFilter filter = new IntentFilter(TimeZoneChangedReceiver.ACTION_MANUAL_UPDATE_TIMEZONE);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(timeZoneChangedReceiver, filter);
    }

    private void unregisterManualTimeZoneUpdateReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(timeZoneChangedReceiver);
    }

    /**
     * Broadcast receiver for when TimeZoneChangedReceiver has finished doing it's thing
     */
    private BroadcastReceiver updateTimeZone = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String currentTimeZoneId = TimeZone.getDefault().getID();
            ((TextView) getView().findViewById(R.id.text_city)).setText(getTimezoneCity(currentTimeZoneId));
            ((TextView) getView().findViewById(R.id.text_timezone))
                    .setText("(" + getResources().getString(R.string.beijing) + " "
                            + TimeZoneHelper.getStringOffsetWithChina(currentTimeZoneId) + ")");
        }
    };

    private void registerTimeZoneChangedReceiver() {
        IntentFilter filter = new IntentFilter(TimeZoneChangedReceiver.ACTION_TIMEZONE_UPDATE_COMPLETE);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(updateTimeZone, filter);
    }

    private void unregisterTimeZoneChangedReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(updateTimeZone);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}