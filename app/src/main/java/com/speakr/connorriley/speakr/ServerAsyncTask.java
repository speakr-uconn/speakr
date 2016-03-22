package com.speakr.connorriley.speakr;

/**
 * Created by connorriley on 3/22/16.
 */

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A simple server socket that accepts connection and writes some data on
 * the stream.
 */
public class ServerAsyncTask extends AsyncTask<Void, Void, String> {

    private Context context;
    private String TAG = "ServerAsyncTask";
    private String timeStamp = null;
    private String dataType = null;
    /**
     * @param context
     */
    public ServerAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            ServerSocket serverSocket = null;
            serverSocket = new ServerSocket(8988);
            Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
            Socket client = serverSocket.accept();
            Log.d(WiFiDirectActivity.TAG, "Server: connection done");
            // receive data type string
            DataInputStream is = new DataInputStream(client.getInputStream());
            dataType = is.readUTF();
            //check if mimeType is all numbers or not
            switch (dataType) {
                case "Play":
                    timeStamp = receiveTimeStamp(client);
                    break;
                case "Pause":
                    timeStamp = receiveTimeStamp(client);
                    break;
                case "File":
                    String recievedPath = receiveFile(client);
                    return recievedPath;
                case "IP":
                    String receivedIP = receiveIP(client);

                    return receivedIP;
                default:
                    Log.e(TAG, "No case match");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String receiveIP(Socket client) {
        DataInputStream is = null;
        try {
            is = new DataInputStream(client.getInputStream());
            String ip = is.readUTF();
            return  ip;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private String receiveTimeStamp(Socket client) {
        DataInputStream is = null;
        try {
            is = new DataInputStream(client.getInputStream());
            String timeStamp = is.readUTF();
            return  timeStamp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String receiveFile(Socket client) {
        try {
            DataInputStream is = new DataInputStream(client.getInputStream());
            String mimeType = is.readUTF();
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
            return f.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /*
     * (non-Javadoc)
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(String result) {
        if (dataType.equals("File")) {
            dataType = null;
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
                    //context.startActivity(playerIntent);
                }
            });
        } else if(dataType.equals("IP")) {
          // do stuff with IP
        } else if(result == null) {
            if(timeStamp != null) {
                String localtimeStamp = timeStamp;
                timeStamp = null;
                String localMusicControl = dataType;
                dataType = null;
                Long receivedTime = Long.parseLong(localtimeStamp);
                //setUpTimeStamp(receivedTime, localMusicControl);
            }
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