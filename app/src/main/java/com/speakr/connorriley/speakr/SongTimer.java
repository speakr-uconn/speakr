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
                     Context context1) {
        Log.d(TAG, "New SongTimer");
        controller = c;
        context = context1;
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
            } else if (action.equals("Resume")) {
                musicSrv.go();
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
