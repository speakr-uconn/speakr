package com.speakr.connorriley.speakr;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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
        } else {
            Log.e(LOG_TAG, "ERROR: time received was " + time);
        }
        return time;
    }
}
