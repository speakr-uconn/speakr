package com.speakr.connorriley.speakr;

import android.os.SystemClock;
import android.util.Log;

/**
 * Created by connorriley on 1/19/16.
 */

public class TimeSync {
    private String TAG = "TimeSync";
    SntpClient client = new SntpClient();

    // Step 1: Get the time from the server (test by printing)
    // Step 2: Send a message to the server that says, print the time at x:xx:xx...
    // Step 3: Ensure that the message prints on the server.
    // Step 4: Set the client to print the message at the same time as the time sent in the message to the server
    // Step 5: Ensure that the client and server both print the message at the same time
    // Step 6: Instead of printing a message, play the song.
    public long getNTPTime() {
        long now = -1;
        if (client.requestTime("0.north-america.pool.ntp.org", 5000)) // server path
        {
            Log.d(TAG, "Entered If");
            now = client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference();
        }
        return now;
    }
    public long getNTPOffset() {
        long offset = 0;
        if (client.requestTime("0.north-america.pool.ntp.org", 5000)) // server path
        {
            Log.d(TAG, "Entered If");
            offset = client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference() - System.currentTimeMillis();
        }
        Log.d(TAG, "Offset: " + offset);
        return offset;
    }

    public long setServerPlayTime(long offset, long playtime) {
        // systime + offset = servertime
        return playtime + offset;
    }
}
