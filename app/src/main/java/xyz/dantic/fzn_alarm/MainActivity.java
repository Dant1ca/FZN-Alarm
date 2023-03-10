package xyz.dantic.fzn_alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import xyz.dantic.fzn_alarm.databinding.ActivityMainBinding;
import xyz.dantic.fzn_alarm.services.GongPlayerService;
import xyz.dantic.fzn_alarm.services.TriggerAlarmReceiver;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmScheduler;
import xyz.dantic.fzn_alarm.ui.alarm.AlarmViewModel;
import xyz.dantic.fzn_alarm.ui.settings.SettingsViewModel;

// todo add comments in mainactivity
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_START_POINT = "xyz.dantic.fzn_alarm.extra.START_POINT";
    public static final String GONG_FRAGMENT_START_POINT = "gong_fragment";
    public static final String ALARM_FRAGMENT_START_POINT = "alarm_fragment";
    
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Calling this function is critical for "loading" the settings which are used later in AlarmScheduler
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        // Set up fragment NavController and starting point
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        String startPoint = getIntent().getStringExtra(EXTRA_START_POINT);
        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.mobile_navigation);
        if (GONG_FRAGMENT_START_POINT.equals(startPoint)) {
            navGraph.setStartDestination(R.id.navigation_gong);
        }
        else {
            navGraph.setStartDestination(R.id.navigation_alarm_container);
        }
        navController.setGraph(navGraph);
        // Link to bottom nav bar
        NavigationUI.setupWithNavController(binding.navView, navController);

        // If first launch, set up notification channels if required and schedule alarms immediately
        SharedPreferences activityPreferences = getPreferences(MODE_PRIVATE);
        boolean firstLaunch = activityPreferences.getBoolean("first_launch", true);
        if (firstLaunch) {
            activityPreferences.edit().putBoolean("first_launch", false).apply();

            // Notification channels required from version Oreo
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                createNotificationChannels();
            }

            // Schedule alarms (default alarms already created on initialization of AlarmViewModel)
            AlarmViewModel alarmViewModel =
                    new ViewModelProvider(MainActivity.this).get(AlarmViewModel.class);
            AlarmScheduler alarmScheduler = new AlarmScheduler(getApplication());
            alarmScheduler.scheduleAlarmList(alarmViewModel.getAlarmList());
        }
    }

    /**
     * Sets up notification channels
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannels() {
        /* Notification channel for alarms */
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // Configure the channel
        NotificationChannel alarmChannel =
                new NotificationChannel(TriggerAlarmReceiver.NOTIFICATION_CHANNEL_ID,
                        getString(R.string.alarms), NotificationManager.IMPORTANCE_HIGH);
//            alarmChannel.setDescription("FZN Alarm Notifications");
        // Remove sounds as I deal with sounds manually
        alarmChannel.setSound(null, null);
        // Register the channel with the notifications manager
        mNotificationManager.createNotificationChannel(alarmChannel);

        /* Notification channel for gong player */
        NotificationChannel gongPlayerChannel =
                new NotificationChannel(GongPlayerService.NOTIFICATION_CHANNEL_ID, getString(R.string.music_player), NotificationManager.IMPORTANCE_LOW);
        gongPlayerChannel.setSound(null, null);
//            gongPlayerChannel.setDescription("FZN Gong Track Player");
        mNotificationManager.createNotificationChannel(gongPlayerChannel);
        // TODO consider switching to ExoPlayer which handles proper notification as well
        // https://stackoverflow.com/questions/63501425/java-android-media-player-notification
    }
}