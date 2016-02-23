package com.speakr.connorriley.speakr;

import android.net.wifi.p2p.WifiP2pInfo;

/**
 * Created by connorriley on 2/22/16.
 */
public class WifiSingleton {

    private static WifiSingleton instance = null;
    private WifiP2pInfo info = null;
    protected WifiSingleton() {

    }
    public static WifiSingleton getInstance() {
        if(instance == null) {
            instance = new WifiSingleton();
        }
        return instance;
    }

    public WifiP2pInfo getInfo() {
        return info;
    }
    public void setInfo(WifiP2pInfo info) {
        this.info = info;
    }
}
