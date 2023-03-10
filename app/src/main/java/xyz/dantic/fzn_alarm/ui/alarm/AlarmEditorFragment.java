package xyz.dantic.fzn_alarm.ui.alarm;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import xyz.dantic.fzn_alarm.R;
import xyz.dantic.fzn_alarm.databinding.FragmentAlarmEditorBinding;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class AlarmEditorFragment extends Fragment {

    private FragmentAlarmEditorBinding binding;
    AlarmViewModel alarmViewModel;
    Alarm oldAlarm, newAlarm;
    ImageView daysOfWeekIconImageViews[];

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentAlarmEditorBinding.inflate(inflater, container, false);

        alarmViewModel =
                new ViewModelProvider(requireActivity()).get(AlarmViewModel.class);

        // Copy alarm (or create new one)
        oldAlarm = alarmViewModel.getAlarmSelected();
        if (oldAlarm == null) {
            newAlarm = new Alarm(15, 0);
        } else {
            newAlarm = oldAlarm.clone();
        }

        // Initialise number pickers
        binding.numberpickerHour.setMinValue(0);
        binding.numberpickerHour.setMaxValue(23);
        binding.numberpickerHour.setOnLongPressUpdateInterval(50);
        increaseMaxFlingSpeed(binding.numberpickerHour, 1.4);
        binding.numberpickerHour.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int i) {
                return String.format("%02d", i);
            }
        });
        binding.numberpickerMinute.setMinValue(0);
        binding.numberpickerMinute.setMaxValue(59);
        binding.numberpickerMinute.setOnLongPressUpdateInterval(20);
        increaseMaxFlingSpeed(binding.numberpickerMinute, 2.3);
        binding.numberpickerMinute.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int i) {
                return String.format("%02d", i);
            }
        });


        // Set up sound icons for each day + weekdays and weekends to make working with them easier
        daysOfWeekIconImageViews = new ImageView[9];
        daysOfWeekIconImageViews[0] = binding.imageviewMondayIcon;
        daysOfWeekIconImageViews[1] = binding.imageviewTuesdayIcon;
        daysOfWeekIconImageViews[2] = binding.imageviewWednesdayIcon;
        daysOfWeekIconImageViews[3] = binding.imageviewThursdayIcon;
        daysOfWeekIconImageViews[4] = binding.imageviewFridayIcon;
        daysOfWeekIconImageViews[5] = binding.imageviewSaturdayIcon;
        daysOfWeekIconImageViews[6] = binding.imageviewSundayIcon;
        daysOfWeekIconImageViews[7] = binding.imageviewWeekdaysIcon;
        daysOfWeekIconImageViews[8] = binding.imageviewWeekendsIcon;

        // For each day
        for (int i = 0; i < 7; i++) {
            switch (newAlarm.getSoundOnDay(i)) {
                case OFF:
                    daysOfWeekIconImageViews[i].setImageResource(R.drawable.ic_no_sound);
                    daysOfWeekIconImageViews[i].setTag(R.drawable.ic_no_sound);
                    break;
                case GONG:
                    daysOfWeekIconImageViews[i].setImageResource(R.drawable.ic_gong);
                    daysOfWeekIconImageViews[i].setTag(R.drawable.ic_gong);
                    break;
                case NOTIF:
                    daysOfWeekIconImageViews[i].setImageResource(R.drawable.ic_notif);
                    daysOfWeekIconImageViews[i].setTag(R.drawable.ic_notif);
                    break;
            }
        }

        try {
            updateWeekdaysOrWeekendsIcon(0); // For weekdays
            updateWeekdaysOrWeekendsIcon(5); // For weekends
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Spin spinner up to current time
        spinSpinner(binding.numberpickerHour, newAlarm.getHour(), 100);
        spinSpinner(binding.numberpickerMinute, newAlarm.getMinute(), 200);

        // Set delete listener
        binding.fabDeleteAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (oldAlarm != null) {
                    alarmViewModel.deleteCurrentAlarm();
                }
                ((AlarmContainerFragment) getParentFragment()).insertAlarmViewerFragment();
            }
        });

        // Set up event listeners
        LinearLayout dayLayouts[] = new LinearLayout[9];
        dayLayouts[0] = binding.linearlayoutMonday;
        dayLayouts[1] = binding.linearlayoutTuesday;
        dayLayouts[2] = binding.linearlayoutWednesday;
        dayLayouts[3] = binding.linearlayoutThursday;
        dayLayouts[4] = binding.linearlayoutFriday;
        dayLayouts[5] = binding.linearlayoutSaturday;
        dayLayouts[6] = binding.linearlayoutSunday;
        dayLayouts[7] = binding.linearlayoutWeekdays;
        dayLayouts[8] = binding.linearlayoutWeekends;

        // Listeners for each day of week
        for (int i = 0; i < 7; i++) {
            final ImageView iconImageView = daysOfWeekIconImageViews[i];
            final int day = i;
            dayLayouts[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (newAlarm.getSoundOnDay(day)) {
                        case OFF:
                            iconImageView.setImageResource(R.drawable.ic_gong);
                            iconImageView.setTag(R.drawable.ic_gong);
                            newAlarm.setSoundOnDay(day, Alarm.Sound.GONG);
                            try {
                                updateWeekdaysOrWeekendsIcon(day);
                            } catch (InvocationTargetException | IllegalAccessException e) {
                                e.printStackTrace();
                            }
                            break;
                        case GONG:
                            iconImageView.setImageResource(R.drawable.ic_notif);
                            iconImageView.setTag(R.drawable.ic_notif);
                            newAlarm.setSoundOnDay(day, Alarm.Sound.NOTIF);
                            try {
                                updateWeekdaysOrWeekendsIcon(day);
                            } catch (InvocationTargetException | IllegalAccessException e) {
                                e.printStackTrace();
                            }
                            break;
                        case NOTIF:
                            iconImageView.setImageResource(R.drawable.ic_no_sound);
                            iconImageView.setTag(R.drawable.ic_no_sound);
                            newAlarm.setSoundOnDay(day, Alarm.Sound.OFF);
                            try {
                                updateWeekdaysOrWeekendsIcon(day);
                            } catch (InvocationTargetException | IllegalAccessException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            });
        }

        // Listener for all weekdays shortcut
        dayLayouts[7].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch ((int) daysOfWeekIconImageViews[7].getTag()) {
                    case R.drawable.ic_no_sound:
                    case R.drawable.ic_gong_and_notif:
                        daysOfWeekIconImageViews[7].setImageResource(R.drawable.ic_gong);
                        daysOfWeekIconImageViews[7].setTag(R.drawable.ic_gong);
                        setDaysAsIcon(Alarm.Sound.GONG, 5);
                        break;
                    case R.drawable.ic_gong:
                        daysOfWeekIconImageViews[7].setImageResource(R.drawable.ic_notif);
                        daysOfWeekIconImageViews[7].setTag(R.drawable.ic_notif);
                        setDaysAsIcon(Alarm.Sound.NOTIF, 5);
                        break;
                    case R.drawable.ic_notif:
                        daysOfWeekIconImageViews[7].setImageResource(R.drawable.ic_no_sound);
                        daysOfWeekIconImageViews[7].setTag(R.drawable.ic_no_sound);
                        setDaysAsIcon(Alarm.Sound.OFF, 5);
                        break;
                    }
                }
        });

        // Listener for all weekends shortcut
        dayLayouts[8].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch ((int) daysOfWeekIconImageViews[8].getTag()) {
                    case R.drawable.ic_no_sound:
                    case R.drawable.ic_gong_and_notif:
                        daysOfWeekIconImageViews[8].setImageResource(R.drawable.ic_gong);
                        daysOfWeekIconImageViews[8].setTag(R.drawable.ic_gong);
                        setDaysAsIcon(Alarm.Sound.GONG, 5, 7);
                        break;
                    case R.drawable.ic_gong:
                        daysOfWeekIconImageViews[8].setImageResource(R.drawable.ic_notif);
                        daysOfWeekIconImageViews[8].setTag(R.drawable.ic_notif);
                        setDaysAsIcon(Alarm.Sound.NOTIF, 5, 7);
                        break;
                    case R.drawable.ic_notif:
                        daysOfWeekIconImageViews[8].setImageResource(R.drawable.ic_no_sound);
                        daysOfWeekIconImageViews[8].setTag(R.drawable.ic_no_sound);
                        setDaysAsIcon(Alarm.Sound.OFF, 5, 7);
                        break;
                }
            }
        });
    }

    /**
     * Helper method to set all days between a range to a sound
     * @param sound the sound to set the days to
     * @param dayStart the first day in the range (inclusive)
     * @param dayEnd the last day in the range (exclusive)
     */
    private void setDaysAsIcon(Alarm.Sound sound, int dayStart, int dayEnd) {
        int iconID = -1;
        switch (sound) {
            case GONG:
                iconID = R.drawable.ic_gong;
                break;
            case NOTIF:
                iconID = R.drawable.ic_notif;
                break;
            case OFF:
                iconID = R.drawable.ic_no_sound;
                break;
        }

        for (int day = dayStart; day < dayEnd; day++) {
            daysOfWeekIconImageViews[day].setImageResource(iconID);
            daysOfWeekIconImageViews[day].setTag(iconID);
            newAlarm.setSoundOnDay(day, sound);
        }
    }

    /**
     * Helper method to set all days leading up to a day to a sound
     * @param sound the sound to set the days to
     * @param dayEnd the last day in the range (exclusive)
     */
    private void setDaysAsIcon(Alarm.Sound sound, int dayEnd) {
        setDaysAsIcon(sound, 0, dayEnd);
    }

    /**
     * Helper method to update the main weekdays or weekends icon
     * @param day day of the week to update for (0 <= day < 5 for weekdays update, or 5 <= day < 7 for weekends)
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    // TODO can we put try catches within this method?
    private void updateWeekdaysOrWeekendsIcon(int day) throws InvocationTargetException, IllegalAccessException {
        ImageView viewType;
        Method methodType = null;
        // Utilising reflection to remove code duplication
        if (day < 5) {
            viewType = daysOfWeekIconImageViews[7];
            try {
                methodType = newAlarm.getClass().getMethod("isSoundOnWeekdays", Alarm.Sound.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        } else {
            viewType = daysOfWeekIconImageViews[8];
            try {
                methodType = newAlarm.getClass().getMethod("isSoundOnWeekends", Alarm.Sound.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        switch ((int) daysOfWeekIconImageViews[day].getTag()) {
            case R.drawable.ic_gong:
                if ((boolean) methodType.invoke(newAlarm, Alarm.Sound.GONG)) {
                    viewType.setImageResource(R.drawable.ic_gong);
                    viewType.setTag(R.drawable.ic_gong);
                } else {
                    viewType.setImageResource(R.drawable.ic_gong_and_notif);
                    viewType.setTag(R.drawable.ic_gong_and_notif);
                }
                break;
            case R.drawable.ic_notif:
                if ((boolean) methodType.invoke(newAlarm, Alarm.Sound.NOTIF)) {
                    viewType.setImageResource(R.drawable.ic_notif);
                    viewType.setTag(R.drawable.ic_notif);
                } else {
                    viewType.setImageResource(R.drawable.ic_gong_and_notif);
                    viewType.setTag(R.drawable.ic_gong_and_notif);
                }
                break;
            case R.drawable.ic_no_sound:
                if ((boolean) methodType.invoke(newAlarm, Alarm.Sound.OFF)) {
                    viewType.setImageResource(R.drawable.ic_no_sound);
                    viewType.setTag(R.drawable.ic_no_sound);
                } else {
                    viewType.setImageResource(R.drawable.ic_gong_and_notif);
                    viewType.setTag(R.drawable.ic_gong_and_notif);
                }
                break;
        }
    }

    /**
     * Animates a NumberPicker
     * @param spinner The NumberPicker
     * @param value Which value to spin up to
     * @param initial_delay Initial delay before starting animation
     */
    private void spinSpinner(NumberPicker spinner, final int value, int initial_delay) {
        if (value >= 0) {
            final int delayBetweenIncrement = 1;

            // Make spinner consistently start TOTAL_SPINS less than value
            final int TOTAL_SPINS = 10;
            spinner.setValue(value - TOTAL_SPINS);

            Method method;
            try {
                // Must use reflection because method is private by default
                method = spinner.getClass().getDeclaredMethod("changeValueByOne", boolean.class);
                method.setAccessible(true);

                Handler handler = new Handler();
                Runnable runnable = new Runnable() {
                    int spins = 0;

                    @Override
                    public void run() {
                        try {
                            method.invoke(spinner, true);
                            spins++;
                            if (spins < TOTAL_SPINS) handler.postDelayed(this, spins * spins * delayBetweenIncrement);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                };
                handler.postDelayed(runnable, initial_delay);
            }
            catch (final NoSuchMethodException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Increases a NumberPicker's max fling velocity
     * @param numberPicker The NumberPicker
     * @param factor By which factor to multiply the default velocity
     */
    @SuppressLint("SoonBlockedPrivateApi")
    private void increaseMaxFlingSpeed(NumberPicker numberPicker, double factor) {
        try {
            // Using reflection
            Field field = numberPicker.getClass().getDeclaredField("mMaximumFlingVelocity");
            field.setAccessible(true);
            field.setInt(numberPicker, (int) ((int) field.get(numberPicker) * factor));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save the currently edited alarm
     */
    public boolean saveEditorSettings() {
        newAlarm.setHour(binding.numberpickerHour.getValue());
        newAlarm.setMinute(binding.numberpickerMinute.getValue());

        // Don't allow user to set alarm that clashes with another
//        for (Alarm alarm : alarmViewModel.getAlarmList()) {
//            if (alarmsTooClose(newAlarm, alarm) && oldAlarm != alarm) {
//                Toast.makeText(requireContext(), "Clashes with existing alarm at " + alarm.getReadableTime(), Toast.LENGTH_SHORT).show(); // TODO make language independent
//                return false;
//            }
//        }

        // Edge case where user turns off alarms for each day manually
        if (newAlarm.getGongDaysAsString(getContext()).isEmpty() && newAlarm.getNotifDaysAsString(getContext()).isEmpty())
            newAlarm.setOn(false);
        else newAlarm.setOn(true);

        if (oldAlarm == null) {
            alarmViewModel.addNewAlarm(newAlarm);
        } else {
            alarmViewModel.replaceCurrentAlarm(newAlarm);
        }
        return true;
    }

    /**
     * Determines whether 2 alarms clash with each other
     * @param alarm1 First alarm
     * @param alarm2 Second alarm
     * @return Whether 2 alarms clash with each other
     */
    private boolean alarmsTooClose(Alarm alarm1, Alarm alarm2) {
        if (alarm1.getTimeInMinutes() > alarm2.getTimeInMinutes()) {
            Alarm swap = alarm1;
            alarm1 = alarm2;
            alarm2 = swap;
        }

        if ((alarm1.getTimeInMinutes() + alarm1.getDuration()) > alarm2.getTimeInMinutes()) {
            return true;
        }

        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        alarmViewModel = null;
    }
}
