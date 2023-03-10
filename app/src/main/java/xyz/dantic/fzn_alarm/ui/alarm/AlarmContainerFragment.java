package xyz.dantic.fzn_alarm.ui.alarm;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import xyz.dantic.fzn_alarm.R;
import xyz.dantic.fzn_alarm.databinding.FragmentAlarmContainerBinding;
import xyz.dantic.fzn_alarm.ui.settings.SettingsViewModel;

public class AlarmContainerFragment extends Fragment {

    private FragmentAlarmContainerBinding binding;
    private AlarmViewModel alarmViewModel;
    private SettingsViewModel settingsViewModel; // TODO Unused
    private Fragment currentChild;

    private final int ANIMATION_SPEED = 200; // Time in ms for each bottom bar to transition

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentAlarmContainerBinding.inflate(inflater, container, false);

        alarmViewModel =
                new ViewModelProvider(requireActivity()).get(AlarmViewModel.class);
        settingsViewModel =
                new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Default fragment is the alarm viewer fragment
        currentChild = new AlarmViewerFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.alarm_container, currentChild)
                .commit();


        // Save button event listener
        binding.alarmEditorBottomBarOptions.getRoot().findViewById(R.id.text_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Should only be activated within the alarm AlarmEditor fragment
                if (currentChild instanceof AlarmEditorFragment) {
                    boolean successfulSave = ((AlarmEditorFragment) currentChild).saveEditorSettings();
                    if (successfulSave) insertAlarmViewerFragment();
                }

            }
        });

        // Cancel button event listener
        binding.alarmEditorBottomBarOptions.getRoot().findViewById(R.id.text_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertAlarmViewerFragment();
            }
        });

        // Capture back button in alarm alarmPreferencesEditor to return to alarm viewer
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if(keyCode == KeyEvent.KEYCODE_BACK && currentChild instanceof AlarmEditorFragment)
                {
                    insertAlarmViewerFragment();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Inserts alarm alarmPreferencesEditor fragment into container and animates exchange of bottom bars
     * @param alarmNum Existing alarm index, or -1 if new alarm
     */
    public void insertAlarmEditorFragment(int alarmNum) {
        alarmViewModel.setCurrentAlarmPosition(alarmNum);

        currentChild = new AlarmEditorFragment();
        getChildFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.alarm_container, currentChild)
                .commit();

        // Transition out bottom nav bar
        BottomNavigationView bottomNavBar = getActivity().findViewById(R.id.nav_view);
        ObjectAnimator navBarExitAnimation = ObjectAnimator.ofFloat(bottomNavBar, "translationY", bottomNavBar.getHeight());
        navBarExitAnimation.setDuration(ANIMATION_SPEED);

        //Bring in alarm edit bottom bar
        View alarmEditBottomBar = binding.alarmEditorBottomBar;
        ObjectAnimator editBarEnterAnimation = ObjectAnimator.ofFloat(alarmEditBottomBar, "translationY", 0f, -alarmEditBottomBar.getHeight());
        editBarEnterAnimation.setDuration(ANIMATION_SPEED);

        AnimatorSet s = new AnimatorSet();
        s.play(editBarEnterAnimation).after(0).after(navBarExitAnimation);
        s.start();
    }

    /**
     * Inserts alarm viewer fragment into container and animates exchange of bottom bars
     */
    public void insertAlarmViewerFragment() {
        currentChild = new AlarmViewerFragment();
        getChildFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .replace(R.id.alarm_container, currentChild)
                .commit();

        // Transition out alarm edit bottom bar
        View alarmEditBottomBar = binding.alarmEditorBottomBar;
        ObjectAnimator editBarExitAnimation = ObjectAnimator.ofFloat(alarmEditBottomBar, "translationY", 0f);
        editBarExitAnimation.setDuration(ANIMATION_SPEED);

        // Bring bottom nav bar back in
        BottomNavigationView bottomNavBar = getActivity().findViewById(R.id.nav_view);
        ObjectAnimator navBarEnterAnimation = ObjectAnimator.ofFloat(bottomNavBar, "translationY", bottomNavBar.getHeight(), 0f);
        navBarEnterAnimation.setDuration(ANIMATION_SPEED);

        AnimatorSet s = new AnimatorSet();
        s.play(navBarEnterAnimation).after(0).after(editBarExitAnimation);
        s.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        alarmViewModel = null;
        currentChild = null;
    }
}