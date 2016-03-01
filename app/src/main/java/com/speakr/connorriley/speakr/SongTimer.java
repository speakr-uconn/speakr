package com.speakr.connorriley.speakr;

/**
 * Created by connorriley on 3/1/16.
 */
import java.util.Timer;
import java.util.TimerTask;

public class SongTimer {
    Timer timer;
    MusicService musicSrv;

    public SongTimer(long offset, MusicService m) {
        musicSrv = m;
        timer = new Timer();
        timer.schedule(new SongTask(), offset);
    }

    class SongTask extends TimerTask {
        public void run() {
            musicSrv.playSong();
        }
    }
}
