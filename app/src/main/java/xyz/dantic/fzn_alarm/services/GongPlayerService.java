package xyz.dantic.fzn_alarm.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import xyz.dantic.fzn_alarm.MainActivity;
import xyz.dantic.fzn_alarm.R;

public class GongPlayerService extends Service {

    private Notification notification;
    public static volatile MediaPlayer mediaPlayer;
    public static final String GONG_TRACK = "/raw/fzn_15";

    public static int FIFTEEN_MINUTES_IN_MILLIS = 15 * 60 * 1000;
    private static final int FOREGROUND_SERVICE_ID = 3; // Arbitrary
    
    public static volatile int currentId = 0;           // ID of the current song (0 for 15min, 1 for 30min, etc)
    public static volatile int cycledThroughTimes = 0;  // How many 15 minute cycles have been completed
    public static volatile int elapsedTime = 0;         // Total elapsed time for current track
    public static volatile boolean isRunning = false;   // Whether the service is running
    
    public static final String NOTIFICATION_CHANNEL_ID = "xyz.dantic.fzn_alarm.gong_player_channel_id";
    public static final String ACTION_BROADCAST_GONG_PLAYER_TIME = "xyz.dantic.fzn_alarm.services.action.BROADCAST_GONG_PLAYER_TIME";
    public static final String EXTRA_CURRENT_GONG_PLAYER_TIME = "xyz.dantic.fzn_alarm.services.extra.CURRENT_GONG_PLAYER_TIME";
    public static final String EXTRA_START_TIME = "xyz.dantic.fzn_alarm.services.extra.START_TIME";
    public static final String EXTRA_TRACK_ID = "xyz.dantic.fzn_alarm.services.extra.TRACK_ID";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;

        Context context = getApplicationContext();
        mediaPlayer = MediaPlayer.create(context,
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + GONG_TRACK));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
        }
        currentId = 0;
        cycledThroughTimes = 0;
        int startTime = intent.getIntExtra(EXTRA_START_TIME, 0);
        int track = intent.getIntExtra(EXTRA_TRACK_ID, 0);
        setTrack(track);
        playTrackAtTime(startTime);

        return START_STICKY;
    }

    /**
     * Adds the service to the foreground so that it runs in a separate thread and continues
     * even when application is not running
     */
    private void addToForeground() {
        // A notification is compulsory for a foreground service otherwise the service will
        // be killed automatically after around one minute
        notification = setUpNotification();
        startForeground(FOREGROUND_SERVICE_ID, notification);
    }

    /**
     * Removes the service from foreground to allow the system to automatically kill the
     * service whenever it wants
     */
    private void removeFromForeground() {
        stopForeground(true);
    }

    /**
     * Sets up the notification showing that the service is playing
     * @return
     */
    private Notification setUpNotification() {
        Context context = getApplicationContext();
        NotificationCompat.Builder mBuilder = null;

        // Oreo onwards requires notification channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Settings are all outlined in the notification channel (defined in MainActivity)
            mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        }
        else {
            mBuilder = new NotificationCompat.Builder(context)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setDefaults(0); // Don't want sound to play
        }

        // Create intent which will return back to activity if notification pressed
        Intent returnToGongFragmentIntent = new Intent(context, MainActivity.class);
        returnToGongFragmentIntent.putExtra(MainActivity.EXTRA_START_POINT, MainActivity.GONG_FRAGMENT_START_POINT);
        returnToGongFragmentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        // TODO learn what the PendingIntent flags do
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2, returnToGongFragmentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        // TODO consider media notification?
        Notification notification = mBuilder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_gong_solid_color)
                .setContentTitle(getString(R.string.currently_playing))
                .setContentText(getResources().getStringArray(R.array.audio_tracks)[currentId])
                .build();
        return notification;
    }

    /**
     * Set the track time
     * @param millis New time to set to in milliseconds
     */
    public void setTrackTime(int millis) {
        if (millis >= (currentId+1) * FIFTEEN_MINUTES_IN_MILLIS) seekTo(millis);
        else {
            cycledThroughTimes = millis / FIFTEEN_MINUTES_IN_MILLIS; // 900000 = 15 min in ms
            seekTo(millis % FIFTEEN_MINUTES_IN_MILLIS);
        }
    }

    /**
     * Pause the track
     */
    public void pauseTrack() {
        mediaPlayer.pause();
    }

    /**
     * Set a new track to play
     * @param id The ID of the new track to play, where 0 is 15min, 1 is 30min, etc
     */
    public void setTrack(int id) {
        if (mediaPlayer.isPlaying()) mediaPlayer.pause();
        currentId = id;
    }

    /**
     * Private method used for seeking to a specific time in the MediaPlayer
     * @param millis The time to seek to
     */
    private void seekTo(int millis) {
        elapsedTime = millis + cycledThroughTimes * FIFTEEN_MINUTES_IN_MILLIS;
        mediaPlayer.seekTo(millis);
    }

    /**
     * Play the currently selected track at a specific time
     * @param startTime Start time in milliseconds
     */
    public void playTrackAtTime(int startTime) {
        // Add track to foreground, which makes the service run on a separate thread
        // so that it can continue playing if the app is closed
        addToForeground();

        setTrackTime(startTime);
        mediaPlayer.start();

        Intent intentWithCurrentGongTime = new Intent(ACTION_BROADCAST_GONG_PLAYER_TIME);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        // Set up unique broadcast when track finishes playing
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // Send a time of -1 to indicate completion
                intentWithCurrentGongTime.putExtra(EXTRA_CURRENT_GONG_PLAYER_TIME, -1);
                localBroadcastManager.sendBroadcast(intentWithCurrentGongTime);

            }
        });

        // Run in separate thread to avoid blocking, as sleeps are used
        // Sleeps used to broadcast current gong track time every second
        new Thread(new Runnable() {
            @Override
            public void run() {
                int currentTrackTime;

                while (mediaPlayer.isPlaying()) {
                    currentTrackTime = mediaPlayer.getCurrentPosition();

                    // The actual implementation of the player does not involve playing separate tracks
                    // for the 15, 30, 45, and 60 minute tracks. Instead, it uses the single 15 minute track
                    // but loops it when it exceeds 15 minutes. The looping stops when cycledThroughTimes
                    // becomes equal to currentId.
                    if (currentTrackTime > FIFTEEN_MINUTES_IN_MILLIS && cycledThroughTimes < currentId) {
                        cycledThroughTimes++;
                        currentTrackTime = currentTrackTime - FIFTEEN_MINUTES_IN_MILLIS;
                        seekTo(currentTrackTime);
                    }
                    // Update elapsed time
                    elapsedTime = currentTrackTime + cycledThroughTimes * FIFTEEN_MINUTES_IN_MILLIS;
                    intentWithCurrentGongTime.putExtra(EXTRA_CURRENT_GONG_PLAYER_TIME, elapsedTime);
                    localBroadcastManager.sendBroadcast(intentWithCurrentGongTime);

                    // Doesn't actually sleep for one second. Instead, sleeps so that it the next broadcast
                    // occurs one second after the last (effectively takes into account the processing time
                    // and removes it).
                    int timeToNearestSecond = 1000 - (currentTrackTime % 1000);
                    SystemClock.sleep(timeToNearestSecond);
                }
                // Automatically remove service from foreground when track finishes,
                // allowing for the system to kill the service when it desires
                removeFromForeground();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    // Binding used when wanting to access GongPlayerService methods in the GongFragment
    IBinder mBinder = new GongPlayerBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class GongPlayerBinder extends Binder {
        public GongPlayerService getServerInstance() {
            return GongPlayerService.this;
        }
    }
}