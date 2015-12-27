package com.speakr.connorriley.speakr;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by connorriley on 12/27/15.
 */
public class DeviceDetailFragment extends Fragment implements WifiP2pManager.ConnectionInfoListener {
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime=System.currentTimeMillis();

        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
            long endTime=System.currentTimeMillis()-startTime;
            Log.v("", "Time taken to transfer all bytes is : " + endTime);

        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
}
