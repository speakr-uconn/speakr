package com.speakr.connorriley.speakr;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;

/**
 * Created by connorriley on 12/27/15.
 */
/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    private static String TAG = "DeviceDetialFragment";
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("audio/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });

        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("devicedetailfragment:", "onActivityResult");

        // User has picked a song. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        Log.d("DeviceDetailFragment", "startService about to be called");
        getActivity().startService(serviceIntent);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        WifiSingleton instance = WifiSingleton.getInstance();
        instance.setInfo(info);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());


        // set both up as clients and servers
        /*new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                .execute();
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
        ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                .getString(R.string.client_text)); */
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.

        if (info.groupFormed && info.isGroupOwner) {
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                    .execute();
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        //TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        //view.setText(device.deviceAddress);
        //view = (TextView) mContentView.findViewById(R.id.device_info);
        //view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                // receive mime type
                DataInputStream is = new DataInputStream(client.getInputStream());

                String mimeType = is.readUTF();
                //check if mimeType is all numbers or not

                if(mimeType.matches("[0-9]+"))    /* mimetype is all numbers --> assume it's timestamp*/
                {
                    return mimeType;
                }

                else {  //mimteype is not only numbers - assume it's a mime type and get the file
                    Log.d("String", "type: " + mimeType);
                    String fileExtention = null;
                    try {
                        fileExtention = getFileExtention(mimeType);
                    } catch (MimeTypeException e) {
                        e.printStackTrace();
                    }
                    Log.d(WiFiDirectActivity.TAG, "File Extention: " + fileExtention);
                        /*final File f = new File(Environment.getExternalStorageDirectory() + "/"
                                + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                                + fileExtention);
                        */
                    final File f = new File(context.getFilesDir().getParent() + "/"
                            + "/wifip2pshared-" + System.currentTimeMillis()
                            + fileExtention);
                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();

                    Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                    InputStream inputstream = client.getInputStream();
                    copyFile(inputstream, new FileOutputStream(f));
                    serverSocket.close();
                    return f.getAbsolutePath();
                }
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null && result.matches("[0-9]+") == false)    //result is not only numbers - assume it's a file path
            {
                Log.d("DeviceDeatilFrag", "File copied - " + result);
                // send a broadcast to add the file to the media store
                File resultFile = new File(result);
                Uri resultUri = Uri.fromFile(resultFile);

                Intent mediaIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, resultUri);
                try {
                    context.sendBroadcast(mediaIntent);
                    //context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    //        resultUri));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                // scan the media store for the file, return its path and uri
                File file = new File(result);
                MediaScannerConnection.scanFile(context, new String[]{
                        file.getAbsolutePath()
                }, null, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        // when scan completes, bundle the path and uri and launch player
                        Log.v(TAG,
                                "Scan completed: file " + path + " was scanned successfully: " + uri);
                        Log.d("DeviceDetailFrag", "start music player intent");
                        Intent playerIntent = new Intent(context, PlayerActivity.class);
                        // this adds a flag to clear the intent if its running and create a new one
                        playerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        Bundle bundle = new Bundle();
                        bundle.putString("SongPath", path);
                        playerIntent.putExtras(bundle);
                        context.startActivity(playerIntent);
                    }
                });

                /*Log.d("DeviceDetailFrag", "start music player intent");
                Intent playerIntent = new Intent(context, PlayerActivity.class);
                // this adds a flag to clear the intent if its running and create a new one
                playerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                Bundle bundle = new Bundle();
                bundle.putString("SongPath", result);
                bundle.putString("SongURI", resultUri.toString());
                //Log.d(TAG, "URI PATH: " + uri.getEncodedPath());
                //Log.d(TAG, "URI PATH: " + uri.getPath());

                playerIntent.putExtras(bundle);
                context.startActivity(playerIntent);*/
            }

            else if(result != null && result.matches("[0-9]+")) //result is a timestamp
            {
                WifiSingleton wifiSingleton = WifiSingleton.getInstance();
                wifiSingleton.setTimestamp(Long.valueOf(result).longValue());
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime=System.currentTimeMillis();
        Log.d(WiFiDirectActivity.TAG, "starting tranfser of file in copy file");
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            inputStream.close();
            long endTime=System.currentTimeMillis()-startTime;
            Log.v("","Time taken to transfer all bytes is : "+endTime);

        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    public static String getFileExtention(String mime) throws MimeTypeException {
        switch (mime) {
            case "audio/mpeg":
                return ".mp3";
            case "audio/mp4":
                return ".m4a";
            case "audio/flac":
                return ".flac";
        }
        throw new MimeTypeException("No Mime Type Found");
    }
}
