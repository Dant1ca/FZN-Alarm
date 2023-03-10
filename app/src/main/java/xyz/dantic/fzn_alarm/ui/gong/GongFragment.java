package xyz.dantic.fzn_alarm.ui.gong;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import xyz.dantic.fzn_alarm.R;
import xyz.dantic.fzn_alarm.databinding.FragmentGongBinding;
import xyz.dantic.fzn_alarm.services.GongPlayerService;

//TODO check if user notifications are muted
public class GongFragment extends Fragment {

    private final int SHORT_SKIP = 10 * 1000; // Duration of a short skip, in milliseconds

    private Context context;
    private FragmentGongBinding binding;
    private GongViewModel gongViewModel;
    private GongPlayerService gongPlayerService;
    boolean serviceBound = false;

    private boolean seekMode = false;   // Whether the user is currently dragging the slider
    private String[] audioTracks;       // String list of audio track names

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        context = requireContext();
        gongViewModel =
                new ViewModelProvider(this).get(GongViewModel.class);

        binding = FragmentGongBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Loads the data if service is already running (such as when the app is reopened)
        if (GongPlayerService.isRunning) {
            gongViewModel.setCurrentTrackId(GongPlayerService.currentId);
            gongViewModel.setCurrentlyPlaying(GongPlayerService.mediaPlayer.isPlaying());
            gongViewModel.setCurrentTrackTime(GongPlayerService.elapsedTime);
        }
        initialiseViewsFromViewModel();

        // Dropdown menu
        audioTracks = requireContext().getResources().getStringArray(R.array.audio_tracks);
        binding.menuAudioTrack.setText(audioTracks[gongViewModel.getCurrentTrackId()], false);

        return root; // Allows root view to be called by getView() later on
    }

    /**
     * Initialises all the views based on current settings in GongViewModel
     */
    private void initialiseViewsFromViewModel() {
        binding.sliderElapsedTime.setValueFrom(0);
        binding.sliderElapsedTime.setValueTo(gongViewModel.getCurrentTrackLength());
        binding.sliderElapsedTime.setValue(gongViewModel.getCurrentTrackTime());
        binding.textElapsedTime.setText(getReadableTimeFromMillis(gongViewModel.getCurrentTrackTime()));
        binding.textTotalTrackLength.setText(getReadableTimeFromMillis(gongViewModel.getCurrentTrackLength()));
        if (gongViewModel.isCurrentlyPlaying()) binding.buttonPlay.setImageResource(R.drawable.ic_pause);
        else binding.buttonPlay.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Is not playing, so must resume or start service
                if (!gongViewModel.isCurrentlyPlaying()) {
                    gongViewModel.setCurrentlyPlaying(true);
                    binding.buttonPlay.setImageResource(R.drawable.ic_pause);

                    // Edge case where user presses play and the track is already finished
                    if (gongViewModel.getCurrentTrackTime() == gongViewModel.getCurrentTrackLength()) {
                        gongViewModel.setCurrentTrackTime(0);
                    }

                    if (GongPlayerService.isRunning) {
                        gongPlayerService.setTrack(gongViewModel.getCurrentTrackId());
                        gongPlayerService.playTrackAtTime(gongViewModel.getCurrentTrackTime());
                    }
                    else {
                        startService();
                    }
                } else {
                    if (!GongPlayerService.isRunning) Log.i("MYDEBUG", "PROBLEM: service should be running but isn't");
                    gongViewModel.setCurrentlyPlaying(false);
                    binding.buttonPlay.setImageResource(R.drawable.ic_play);
                    gongPlayerService.pauseTrack();
                }
            }
        });

        binding.sliderElapsedTime.setLabelFormatter(new LabelFormatter() {
            @NonNull
            @Override
            public String getFormattedValue(float value) {
                return getReadableTimeFromMillis((int) value);
            }
        });

        binding.sliderElapsedTime.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                seekMode = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                int sliderValue = (int) slider.getValue();
                binding.textElapsedTime.setText(getReadableTimeFromMillis(sliderValue));
                gongViewModel.setCurrentTrackTime(sliderValue);
                if (GongPlayerService.isRunning) gongPlayerService.setTrackTime(sliderValue);
                seekMode = false;
            }
        });

        binding.buttonBackLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                longSkip(-1);
            }
        });

        binding.buttonBackShort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shortSkip(-1);
            }
        });

        binding.buttonForwardShort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shortSkip(1);
            }
        });

        binding.buttonForwardLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                longSkip(1);
            }
        });

        binding.menuAudioTrack.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != gongViewModel.getCurrentTrackId()) {
                    gongViewModel.setCurrentlyPlaying(false);
                    gongViewModel.setCurrentTrackId(i);
                    gongViewModel.setCurrentTrackTime(0);
                    initialiseViewsFromViewModel();

                    if (GongPlayerService.isRunning) {
                        gongPlayerService.pauseTrack();
                        gongPlayerService.setTrackTime(0);
                        gongPlayerService.setTrack(i);
                    }
                }
            }
        });
    }

    /**
     * Perform a short skip and change the track time by SHORT_SKIP milliseconds
     * @param direction The direction to skip to, where -1 is backwards and 1 is forwards
     */
    private void shortSkip(int direction) {
        int newTime = gongViewModel.getCurrentTrackTime() + SHORT_SKIP * direction;
        skip(newTime);
    }

    /**
     * Perform a long skip and move the track time to the nearest 5 minute marker
     * @param direction The direction to skip to, where -1 is backwards and 1 is forwards
     */
    private void longSkip(int direction) {
        final int FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000;
        int fiveMinCycleNum = gongViewModel.getCurrentTrackTime() / FIVE_MINUTES_IN_MILLIS;
        int newTime = (fiveMinCycleNum + direction) * FIVE_MINUTES_IN_MILLIS;
        if (direction == -1 && gongViewModel.getCurrentTrackTime() % FIVE_MINUTES_IN_MILLIS > 1000)
            newTime += FIVE_MINUTES_IN_MILLIS;
        skip(newTime);
    }

    /**
     * Skips the track to a new time
     * @param newTime
     */
    private void skip(int newTime) {
        if (newTime < 0) newTime = 0;
        else if (newTime > gongViewModel.getCurrentTrackLength()) {
            newTime = gongViewModel.getCurrentTrackLength();
            gongViewModel.setCurrentlyPlaying(false);
            binding.buttonPlay.setImageResource(R.drawable.ic_play);
        }

        gongViewModel.setCurrentTrackTime(newTime);
        binding.textElapsedTime.setText(getReadableTimeFromMillis(newTime));
        binding.sliderElapsedTime.setValue(newTime);
        if (GongPlayerService.isRunning) gongPlayerService.setTrackTime(newTime);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerGongTimeReceiver();
        bindService();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), R.layout.dropdown_item, audioTracks);
        binding.menuAudioTrack.setAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unRegisterGongTimeReceiver();
        unbindService();
    }

    /**
     * Helper method to convert milliseconds into readable time
     * @param millis Time in milliseconds
     * @return The time in HH:mm format
     */
    private String getReadableTimeFromMillis(int millis) {
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * BroadcastReceiver which will receive intents from the GongPlayerService every second
     * to update the track time
     */
    private BroadcastReceiver gongTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int currentGongTrackTime = intent.getIntExtra(GongPlayerService.EXTRA_CURRENT_GONG_PLAYER_TIME, 0);

            if (currentGongTrackTime == -1) { // A completion
                currentGongTrackTime = gongViewModel.getCurrentTrackLength();
                gongViewModel.setCurrentlyPlaying(false);
                ((ImageButton) getView().findViewById(R.id.button_play)).setImageResource(R.drawable.ic_play);
            }
//            else {
//                Log.i("MYDEBUG", "Received broadcasted currentGongTrackTime: " + getReadableTimeFromMillis(currentGongTrackTime));
//            }

            gongViewModel.setCurrentTrackTime(currentGongTrackTime);
            ((TextView) getView().findViewById(R.id.text_elapsed_time)).setText(getReadableTimeFromMillis(currentGongTrackTime));

            if (!seekMode) ((Slider) getView().findViewById(R.id.slider_elapsed_time)).setValue(currentGongTrackTime);
        }
    };

    /**
     * Register the gongTimeReceiver
     */
    private void registerGongTimeReceiver() {
        IntentFilter filter = new IntentFilter(GongPlayerService.ACTION_BROADCAST_GONG_PLAYER_TIME);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(gongTimeReceiver, filter);
    }
    /**
     * Unregister the gongTimeReceiver
     */
    private void unRegisterGongTimeReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(gongTimeReceiver);
        // TODO maybe could put playGongService.stop() here?
    }


    /**
     * Starts the GongPlayerService
     */
    private void startService() {
        Intent startGongPlayerServiceIntent = new Intent(requireActivity(), GongPlayerService.class);
        startGongPlayerServiceIntent.putExtra(GongPlayerService.EXTRA_START_TIME, gongViewModel.getCurrentTrackTime());
        startGongPlayerServiceIntent.putExtra(GongPlayerService.EXTRA_TRACK_ID, gongViewModel.getCurrentTrackId());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().startForegroundService(startGongPlayerServiceIntent);
        } else {
            requireActivity().startService(startGongPlayerServiceIntent); // TODO test on below Oreo versions
        }

        bindService();
    }

    /**
     * Binds the service, which allows for this fragment to use GongPlayerService methods
     */
    private void bindService() {
        serviceBound = true;
        Intent intent = new Intent(requireContext(), GongPlayerService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    /**
     * Unbinds the service
     */
    private void unbindService() {
        if (serviceBound) {
            serviceBound = false;
            context.unbindService(mConnection);
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            serviceBound = true;
            GongPlayerService.GongPlayerBinder binder = (GongPlayerService.GongPlayerBinder) service;
            gongPlayerService = binder.getServerInstance();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
            gongPlayerService = null;
        }
    };
}