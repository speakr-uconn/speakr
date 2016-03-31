package com.speakr.connorriley.speakr;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Created by connorriley on 12/27/15.
 */
public class FileTransferService extends IntentService {
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String ACTION_SEND_TIMESTAMP = "send_timestamp";
    public static final String ACTION_SEND_ADDRESS = "send_address";
    public static final String EXTRAS_FILE_PATH = "file.url";
    public static final String EXTRAS_TIMESTAMP = "timestamp";
    public static final String EXTRAS_ADDRESS = "go_host";
    public static final String EXTRAS_PORT = "go_port";
    public static final String PARAM_OUT_MSG = "output";
    private final String TAG = FileTransferService.class.getSimpleName();

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("FileTransferService", "onHandleIntent Started");
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_ADDRESS);
            // check this address
            Log.d(TAG, "host: " + host);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_PORT);
            try {
                Log.d(TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(TAG, "Client socket - " + socket.isConnected());


                DataOutputStream datastream = new DataOutputStream(socket.getOutputStream());
                ContentResolver cr = context.getContentResolver();

                //send "File" label
                datastream.writeUTF("File");

                // send mime type
                String type = cr.getType(Uri.parse(fileUri));
                Log.e("String", "type:  " + type);
                datastream.writeUTF(type);

                OutputStream stream = socket.getOutputStream();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d(TAG, e.toString());
                }
                copyFile(is, stream);
                Log.d(TAG, "Client: Data written");
                Intent broadcastIntent = new Intent();
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastIntent.setAction(PlayerActivity.PlayerActivityReceiver.ACTION_RESP);
                broadcastIntent.putExtra(PARAM_OUT_MSG, "Sent File");
                sendBroadcast(broadcastIntent);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        } else if (intent.getAction().equals(ACTION_SEND_TIMESTAMP)) {
            String actionString = intent.getExtras().getString("Action");
            String host = intent.getExtras().getString(EXTRAS_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_PORT);
            try {
                Log.d(TAG, "Opening client socket for timestamp- ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(TAG, "Client socket - " + socket.isConnected());


                DataOutputStream datastream = new DataOutputStream(socket.getOutputStream());

                //send string so they know to expect timestamp
                datastream.writeUTF(actionString);

                // send timestamp
                String timestamp = intent.getExtras().getString(EXTRAS_TIMESTAMP);
                Log.e("String", "timestamp:  " + timestamp);
                datastream.writeUTF(timestamp);

                /*Intent broadcastIntent = new Intent();
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastIntent.setAction(PlayerActivity.PlayerActivityReceiver.ACTION_RESP);
                broadcastIntent.putExtra(PARAM_OUT_MSG, "Sent request");
                sendBroadcast(broadcastIntent);*/

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        //send current device's address
        else if (intent.getAction().equals(ACTION_SEND_ADDRESS)) {
            String host = intent.getExtras().getString(EXTRAS_ADDRESS);
            Log.d(TAG, "send address: " + host);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_PORT);
            try {
                Log.d(TAG, "Opening client socket for address- ");

                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(TAG, "Client socket - " + socket.isConnected());

                DataOutputStream datastream = new DataOutputStream(socket.getOutputStream());

                //send "IP" label
                datastream.writeUTF("IP");

                ArrayList<String> localIPArray = getDottedDecimalIP(getLocalIPAddress());
                String localIP = getWifiDirectIP(localIPArray);
                Log.d(TAG, "Local IP: " + localIP);
                datastream.writeUTF(localIP);

                Intent broadcastIntent = new Intent();
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastIntent.setAction(PlayerActivity.PlayerActivityReceiver.ACTION_RESP);
                broadcastIntent.putExtra(PARAM_OUT_MSG, "Sent IP");
                sendBroadcast(broadcastIntent);

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
    private String getWifiDirectIP(ArrayList<String> localIPArray) {
        for(int i = 0; i < localIPArray.size(); i++) {
            String tempLocalIP = localIPArray.get(i);
            if(tempLocalIP.startsWith("192.168.49.",0)) {
                return tempLocalIP;
            }
        }
        return null;
    }
    public boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "starting tranfser of file in copy file");
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            inputStream.close();
            long endTime = System.currentTimeMillis() - startTime;
            Log.v("", "Time taken to transfer all bytes is : " + endTime);

        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }

    private ArrayList<byte[]> getLocalIPAddress() {
        ArrayList<byte[]> inetAddressArray = new ArrayList<byte[]>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) {
                            inetAddressArray.add(inetAddress.getAddress());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        }
        return inetAddressArray;
    }

    private ArrayList<String> getDottedDecimalIP(ArrayList<byte[]> inetAddressArray) {
        ArrayList<String> strAddr = new ArrayList<String>();
        for (int j = 0; j < inetAddressArray.size(); j++) {
            byte[] ipAddr = inetAddressArray.get(j);
            if (ipAddr != null) {
                String ipAddrStr = "";
                for (int i = 0; i < ipAddr.length; i++) {
                    if (i > 0) {
                        ipAddrStr += ".";
                    }
                    ipAddrStr += ipAddr[i] & 0xFF;
                }
                strAddr.add(ipAddrStr);
                Log.d(TAG, ipAddrStr);
            } //else {
            //return "null";
            //}
        }
        return strAddr;
    }
}
