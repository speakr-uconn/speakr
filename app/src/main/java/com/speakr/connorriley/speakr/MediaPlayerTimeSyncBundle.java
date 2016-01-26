package com.speakr.connorriley.speakr;

import android.media.MediaPlayer;

/**
 * Created by Viren on 1/26/2016.
 */
public class MediaPlayerTimeSyncBundle {

    private final MediaPlayer mMediaPlayer;
    private final TimeSync mTimeSync;

    public MediaPlayerTimeSyncBundle(MediaPlayer m, TimeSync t) {
        mMediaPlayer = m;
        mTimeSync = t;
    }

    public MediaPlayer getmMediaPlayer() {
        return mMediaPlayer;
    }

    public TimeSync getmTimeSync() {
        return mTimeSync;
    }

}
