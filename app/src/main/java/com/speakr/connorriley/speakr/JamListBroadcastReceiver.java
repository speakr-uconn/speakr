package com.speakr.connorriley.speakr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * Created by connorriley on 12/27/15.
 */
public class JamListBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private JamListActivity mActivity;
    public JamListBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       JamListActivity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                // UI update to indicate wifi p2p status.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // Wifi Direct mode is enabled
                    mActivity.setIsWifiP2pEnabled(true);
                } else {
                    mActivity.setIsWifiP2pEnabled(false);
                    mActivity.resetData();

                }
                Log.d(WiFiDirectActivity.TAG, "P2P state changed - " + state);
                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                // call wifip2pmanager to request a list of current peers
                mManager.requestPeers(mChannel, (WifiP2pManager.PeerListListener) mActivity.getFragmentManager()
                        .findFragmentById(R.id.frag_list));     // not sure what frag_list is supposed to be
                Log.d(WiFiDirectActivity.TAG, "P2P peers changed");
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                if (mManager == null) {
                    return;
                }

                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {

                    // we are connected with the other device, request connection
                    // info to find group owner IP

                    DeviceDetailFragment fragment = (DeviceDetailFragment) mActivity
                            .getFragmentManager().findFragmentById(R.id.frag_detail);
                    mManager.requestConnectionInfo(mChannel, fragment);
                } else {
                    // It's a disconnect
                    mActivity.resetData();
                }
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                DeviceListFragment fragment = (DeviceListFragment) mActivity.getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
                break;
        }
    }
}
