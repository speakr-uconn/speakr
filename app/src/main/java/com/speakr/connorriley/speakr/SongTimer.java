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
    public SongTimer(long localPlayTime, MusicService m, MusicController c, String action) {
        Log.d(TAG, "New SongTimer");
        controller = c;
        musicSrv = m;
        timer = new Timer();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(localPlayTime);
        Date time = calendar.getTime();
        timer.schedule(new SongTask(action), time);
    }

    class SongTask extends TimerTask {
        private String action;
        public SongTask(String action) {
            this.action = action;
        }
        public void run() {
            Log.d(TAG, action);
            Looper.prepare();
            if(action.equals("Play")) {
                musicSrv.playSong();
            } else if (action.equals("Pause")) {
                musicSrv.pausePlayer();
            }
            controller.show(0);
        }
    }
}
