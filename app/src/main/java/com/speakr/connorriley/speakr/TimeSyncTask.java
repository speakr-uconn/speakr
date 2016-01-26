package com.speakr.connorriley.speakr;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Viren on 1/22/2016.
 */
public class TimeSyncTask extends AsyncTask<MediaPlayerTimeSyncBundle, Void, Long> {

    private final String LOG_TAG = TimeSyncTask.class.getSimpleName();


    @Override
    protected Long doInBackground(MediaPlayerTimeSyncBundle... params) {
        MediaPlayer m = params[0].getmMediaPlayer();
        TimeSync timeSync = params[0].getmTimeSync();
        long time = timeSync.getNTPTime();
        if (time != -1) {
            Log.d(LOG_TAG, "Time received: " + time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            DateFormat df = new SimpleDateFormat("dd:MM:yy:HH:mm:ss.SSS");

            Log.d(LOG_TAG, "Server time: " + df.format(calendar.getTime()));
            Log.d(LOG_TAG, "System time: " + df.format(System.currentTimeMillis()));

            while (System.currentTimeMillis() != (time + 1000)) {}
            m.prepareAsync();

        } else {
            Log.e(LOG_TAG, "ERROR: time received was " + time);
        }
        return time;
    }

    @Override
    protected void onPostExecute(Long aLong) {
    }
}
