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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * Created by connorriley on 12/27/15.
 */
public class FileTransferService extends IntentService {
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String ACTION_SEND_TIMESTAMP = "send_timestamp";
    public static final String EXTRAS_FILE_PATH = "file.url";
    public static final String EXTRAS_TIMESTAMP = "timestamp";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

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
        if(intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(WiFiDirectActivity.TAG, "Client scoket - " + socket.isConnected());

                // send mime type
                DataOutputStream datastream = new DataOutputStream(socket.getOutputStream());
                ContentResolver cr = context.getContentResolver();
                String type = cr.getType(Uri.parse(fileUri));
                Log.e("String", "type:  " + type);
                datastream.writeUTF(type);

                OutputStream stream = socket.getOutputStream();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                }
                DeviceDetailFragment.copyFile(is, stream);
                Log.d(WiFiDirectActivity.TAG, "Client: Data written");
            } catch(IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            }
        }

        else if(intent.getAction().equals(ACTION_SEND_TIMESTAMP)) {
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket for timestamp- ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());

                // send timestamp
                DataOutputStream datastream = new DataOutputStream(socket.getOutputStream());
                String timestamp = intent.getExtras().getString(EXTRAS_TIMESTAMP);

                Log.e("String", "timestamp:  " + timestamp);
                datastream.writeUTF(timestamp);

            } catch(IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            }
        }
    }
}
