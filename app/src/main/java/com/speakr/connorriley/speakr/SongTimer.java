package com.speakr.connorriley.speakr;

/**
 * Created by connorriley on 3/1/16.
 */
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SongTimer {
    private Timer timer;
    private MusicService musicSrv;
    private Context context;
    private MusicController controller;
    private String TAG = "SongTimer";
    public SongTimer(long localPlayTime, MusicService m, MusicController c, String action,
                     Context context1, boolean local) {
        Log.d(TAG, "New SongTimer");
        controller = c;
        context = context1;
        musicSrv = m;
        timer = new Timer();
        if (local) {
            timer.schedule(new SongTask(action), localPlayTime);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(localPlayTime);
            Date time = calendar.getTime();
            timer.schedule(new SongTask(action), time);
        }
    }

    class SongTask extends TimerTask {
        private String action;
        public SongTask(String action) {
            this.action = action;
        }
        public void run() {
            Log.d(TAG, action);
            Looper.prepare();
            switch (action) {
                case "Play":
                    musicSrv.playSong();
                    break;
                case "Pause":
                    musicSrv.pausePlayer();
                    break;
                case "Resume":
                    musicSrv.go();
                    break;
                case "Next":
                    musicSrv.playNext();
                    break;
                case "Previous":
                    musicSrv.playPrev();
                    break;
            }

            Handler mainHandler = new Handler(context.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    controller.show(0);
                }
            });
        }
    }
}
