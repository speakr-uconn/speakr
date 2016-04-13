package com.speakr.connorriley.speakr;

import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by connorriley on 4/12/16.
 */
public class LocalSynchronization {
    private final long NUM_LATENCY_TESTS = 5;
    private ArrayList<Long> latencylist = new ArrayList<Long>();
    private PlayerActivity playerActivity;
    private String TAG = LocalSynchronization.class.getSimpleName();
    private long starttime = 0;
    private long computed_latency;
    private Long offset;

    public LocalSynchronization(PlayerActivity pa) {
        playerActivity = pa;
    }

    public LocalSynchronization resetLatencyList() {
        latencylist = new ArrayList<Long>();
        return this;
    }

    public void addLatencyToList(long latency) {
        latencylist.add(latency);
        if(latencylist.size() < 5) {
            sendtimestamp("CalcOffset_1");
        } else {
            // calculate offset and return
            computed_latency = calculatelatency();
            Log.d(TAG, "Average Latency: " + computed_latency);
            startoffsetcalculation();
        }
    }

    public void sendtimestamp(String action) {
        starttime = 0;
        Log.d(TAG, "SendTimeStamp -- " + action);
        //IT"S TIME TO SEND THE TIME :)
        WifiSingleton wifiSingleton = WifiSingleton.getInstance();
        if (wifiSingleton.getInfo() != null) {
            Intent serviceIntent = new Intent(playerActivity.getApplicationContext(),
                    FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_TIMESTAMP);
            serviceIntent.putExtra("Action", action);
            starttime = System.currentTimeMillis();
            serviceIntent.putExtra(FileTransferService.EXTRAS_TIMESTAMP, "" + starttime);
            Log.d(TAG, "string version of long timestamp to be sent: " + starttime);
            if(!wifiSingleton.getInfo().isGroupOwner){
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getInfo().groupOwnerAddress.getHostAddress());
            }

            else{
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getMemberIP());
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, 8990);
            Log.d(TAG, "startService about to be called for sending timestamp");
            playerActivity.getApplicationContext().startService(serviceIntent);
        }
    }

    public long calculatelatency() {
        long avglatency = 0;
        for(int i = 0; i < latencylist.size(); i++) {
            avglatency += latencylist.get(i);
        }
        return avglatency/latencylist.size();
    }

    public void startoffsetcalculation() {
        sendtimestamp("CalcOffset_3");
    }

    public long getLatency() {
        return computed_latency;
    }
    public Long getOffset() {
        return offset;
    }
    public void setOffset(Long l) {
        offset = l;
    }
    public Long getStartTime() {
        return starttime;
    }
}
