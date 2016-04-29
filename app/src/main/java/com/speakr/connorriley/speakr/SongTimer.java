package com.speakr.connorriley.speakr;

/**
 * Created by connorriley on 3/1/16.
 */
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;

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
                     Context context1, boolean local, String pauseTime) {
        Log.d(TAG, "New SongTimer");
        controller = c;
        context = context1;
        musicSrv = m;
        timer = new Timer();
        if (local) {
            timer.schedule(new SongTask(action, pauseTime), localPlayTime);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(localPlayTime);
            Date time = calendar.getTime();
            timer.schedule(new SongTask(action, null), time);
        }
    }

    class SongTask extends TimerTask {
        private String action;
        private String pauseTime;
        public SongTask(String action, String t) {
            this.action = action;
            this.pauseTime = t;
        }
        public void run() {
            Log.d(TAG, action);
            Looper.prepare();
            if(WifiSingleton.getInstance().getPlayerActivity().progressDialog != null && WifiSingleton.getInstance().getPlayerActivity().progressDialog.isShowing()){
                WifiSingleton.getInstance().getPlayerActivity().progressDialog.dismiss();
            }
            WifiSingleton.getInstance().getPlayerActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            switch (action) {

                case "Play":
                    musicSrv.playSong();
                    break;
                case "Pause":
                    musicSrv.seek((int) Long.parseLong(pauseTime));
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
