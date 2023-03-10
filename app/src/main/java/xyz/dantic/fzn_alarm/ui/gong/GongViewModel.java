package xyz.dantic.fzn_alarm.ui.gong;

import android.app.Application;
import android.content.ContentResolver;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;

import xyz.dantic.fzn_alarm.services.GongPlayerService;

public class GongViewModel extends AndroidViewModel {
    // This class essentially acts as a local copy of GongPlayerService data, like track time, track ID, etc.

    private int trackId;    // Track IDs are 0 for 15min, 1 for 30min, etc
    private int trackTime;  // The time the track is up to
    private boolean currentlyPlaying;

    private int trackLengths[];
    private final String GONG_TRACK = GongPlayerService.GONG_TRACK;

    public GongViewModel(Application application) {
        super(application);
        trackId = 0;
        trackTime = 0;
        currentlyPlaying = false;

        // Get the track time from file metadata
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        String currentTrackFile = ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + getApplication().getPackageName() + GONG_TRACK;
        metaRetriever.setDataSource(getApplication(), Uri.parse(currentTrackFile));
        String durationString = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        metaRetriever.release();

        int durationInMillis = Integer.parseInt(durationString);
        // Set track durations
        trackLengths = new int[4];
        for (int i = 0; i < 4; i++) {
            trackLengths[i] = durationInMillis + i * GongPlayerService.FIFTEEN_MINUTES_IN_MILLIS;
        }
    }

    public int getCurrentTrackId() {
        return trackId;
    }

    public void setCurrentTrackId(int trackId) {
        this.trackId = trackId;
    }

    public int getCurrentTrackLength() {
        return trackLengths[trackId];
    }

    public int getCurrentTrackTime() {
        return trackTime;
    }

    public void setCurrentTrackTime(int trackTime) { this.trackTime = trackTime; }

    public boolean isCurrentlyPlaying() {
        return currentlyPlaying;
    }

    public void setCurrentlyPlaying(boolean currentlyPlaying) {
        this.currentlyPlaying = currentlyPlaying;
    }
}