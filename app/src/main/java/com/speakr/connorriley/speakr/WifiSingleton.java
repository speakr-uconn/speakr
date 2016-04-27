package com.speakr.connorriley.speakr;

import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * Created by connorriley on 2/22/16.
 */
public class WifiSingleton {

    private static WifiSingleton instance = null;
    private String TAG = "WifiSingleton";
    private WifiP2pInfo info = null;
    private String memberIP;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    protected WifiSingleton() {

    }
    public static WifiSingleton getInstance() {
        if(instance == null) {
            instance = new WifiSingleton();
        }
        return instance;
    }



    public void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "removeGroup onSuccess -");
                }
                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "removeGroup onFailure -" + reason);
                }
            });
        }
    }


    public WifiP2pInfo getInfo() {
        return info;
    }
    public void setInfo(WifiP2pInfo info) {
        this.info = info;
    }

    public void setManager(WifiP2pManager manager) {
        mManager = manager;
    }
    public void setChannel(WifiP2pManager.Channel channel) {
        mChannel = channel;
    }
    public void setMemberIP(String ip) {
        memberIP = ip;
        Log.d(TAG, "member ip: " + memberIP);
    }

    public String getMemberIP(){
        return memberIP;
    }

}
