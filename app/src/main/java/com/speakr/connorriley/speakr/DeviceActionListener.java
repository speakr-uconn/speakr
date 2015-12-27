package com.speakr.connorriley.speakr;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by connorriley on 12/27/15.
 */
public interface DeviceActionListener {
    void showDetails(WifiP2pDevice device);
    void cancelDisconnect();
    void connect(WifiP2pConfig config);
    void disconnect();
}
