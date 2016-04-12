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
public class TimeSyncTask extends AsyncTask<TimeSync, Void, Long> {

    private final String LOG_TAG = TimeSyncTask.class.getSimpleName();


    @Override
    protected Long doInBackground(TimeSync... params) {
        TimeSync timeSync = params[0];
        long time = timeSync.getNTPTime();
        if (time != -1) {
            Log.d(LOG_TAG, "Time received: " + time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
            DateFormat df = new SimpleDateFormat("dd:MM:yy:HH:mm:ss.SSS");

            Log.d(LOG_TAG, "Server time: " + df.format(calendar.getTime()));
            Log.d(LOG_TAG, "System time: " + df.format(System.currentTimeMillis()));
        } else {
            Log.e(LOG_TAG, "ERROR: time received was " + time);
        }
        return time;
    }

    @Override
    protected void onPostExecute(Long aLong) {
    }
}
