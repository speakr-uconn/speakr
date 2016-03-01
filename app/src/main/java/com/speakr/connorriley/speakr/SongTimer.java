package com.speakr.connorriley.speakr;

/**
 * Created by connorriley on 3/1/16.
 */
import android.os.Looper;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SongTimer {
    Timer timer;
    MusicService musicSrv;
    MusicController controller;
    String TAG = "SongTimer";
    public SongTimer(long localPlayTime, MusicService m, MusicController c) {
        Log.d(TAG, "New SongTimer");
        controller = c;
        musicSrv = m;
        timer = new Timer();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(localPlayTime);
        Date time = calendar.getTime();
        timer.schedule(new SongTask(), time);
    }

    class SongTask extends TimerTask {
        public void run() {
            Log.d(TAG, "PlaySong");
            Looper.prepare();
            musicSrv.playSong();
            controller.show(0);
        }
    }
}
