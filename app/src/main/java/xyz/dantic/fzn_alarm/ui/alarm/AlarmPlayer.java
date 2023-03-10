package xyz.dantic.fzn_alarm.ui.alarm;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;

import xyz.dantic.fzn_alarm.ui.settings.SettingsViewModel;

/**
 * Responsible for running alarm sounds (built on MediaPlayer, but no real need to extend
 * as it only uses a few of its methods)
 */
public class AlarmPlayer {

    private static final String GONG_TRACK = "/raw/fzn_gong";
    private static final String CUSTOM_NOTIFICATION_TRACK = "/raw/pristine";
    public static int MAX_VOLUME = 100;

    private Context context;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private float userVolume;
    private static volatile int oldAlarmStreamVolume = -1;

    public AlarmPlayer(Context context, MediaPlayer mediaPlayer) {
        this.context = context;
        this.mediaPlayer = mediaPlayer;

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Get the existing alarm stream volume to be set back to at the end
        // Usually this will always be -1 on first use, however, in the case where a user
        // is testing the alarm volume exactly when a gong alarm is going off, this will
        // void overwriting the system's original default alarm volume
        if (oldAlarmStreamVolume == -1)
            oldAlarmStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);

        SharedPreferences settingsPreferences =
                context.getApplicationContext().getSharedPreferences(SettingsViewModel.SettingsFileName, Context.MODE_PRIVATE);
        userVolume = settingsPreferences.getFloat(SettingsViewModel.FILE_NAME_ALARM_VOLUME, 50f);
    }

    /**
     * Play an alarm sound
     * @param sound Sound to play
     */
    public void playSound(Alarm.Sound sound) {
        if (sound == Alarm.Sound.OFF) return;

        // Get the track Uri
        String track = (sound == Alarm.Sound.GONG) ? GONG_TRACK : CUSTOM_NOTIFICATION_TRACK;
        Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + context.getPackageName() + track);

        // Get duration of alarm, to be used when setting audio level back to what it was previously
        // as well as keeping the media player alive so it doesn't get garbage collected
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(context, alarmSound);
        int trackDuration = Integer.parseInt(
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        // Set to max by default because actual volume is adjusted by user volume
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(audioManager.STREAM_ALARM), 0);

        mediaPlayer.reset(); // Mainly in case it's already running
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        try {
            mediaPlayer.setDataSource(context, alarmSound);
            mediaPlayer.prepare();
            setVolume(userVolume);
        } catch (
                IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaPlayer mp = mediaPlayer; // Get a local reference so garbage doesn't collect
                SystemClock.sleep(trackDuration);
                // Set alarm volume back to what it was previously
                if (!isPlaying())
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, oldAlarmStreamVolume, 0);
            }
        }).start();

    }

    /**
     * Stops the alarm from playing
     */
    public void stop() {
        if (isPlaying()) mediaPlayer.stop();
    }

    /**
     * Checks whether the alarm is playing
     * @return
     */
    public boolean isPlaying() {
        return (mediaPlayer != null && mediaPlayer.isPlaying());
    }

    /**
     * Set the alarm volume
     * @param volume Volume level, between 0 and 100 inclusive
     */
    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            float logVolume = changeVolumeScale(volume);
            // Two logVolumes for both left and right ear channels
            mediaPlayer.setVolume(logVolume, logVolume);
            userVolume = volume;
        }
    }

    /**
     * Makes the volume log(log()) scaled
     * @param volume Volume between 0.0 and 100.0
     * @return The log(log()) scaled volume between 0.0 and 1.0
     */
    private static float changeVolumeScale(float volume) {
        // Log the volume scale, converting 0 to 100 volume into a double between 0 and 1
        double firstLog = 1 - Math.log(1 + MAX_VOLUME - volume) / Math.log(1 + MAX_VOLUME);

        // Log the scale again, converting 0 to 100 logged volume into a double between 0 and 1
        double secondLog = 1 - Math.log(1 + MAX_VOLUME - firstLog * MAX_VOLUME) / Math.log(1 + MAX_VOLUME);

        return (float) secondLog;
    }
}
