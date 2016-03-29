package com.speakr.connorriley.speakr;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;

public class PlayerActivity extends HamburgerActivity implements View.OnClickListener, MediaPlayerControl {
    //-- Referenced the following websites:
    //-- Pulling song list: http://code.tutsplus.com/tutorials/create-a-music-player-on-android-project-setup--mobile-22764
    //-- MediaPlayer itself: http://code.tutsplus.com/tutorials/create-a-music-player-on-android-song-playback--mobile-22778
    //-- User controls: http://code.tutsplus.com/tutorials/create-a-music-player-on-android-user-controls--mobile-22787

    private ArrayList<Song> songList, songQueue;
    private ListView songQueueView, songListView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    private MusicController controller;
    private boolean paused = false, playbackPaused = false;
    DrawerLayout drawerLayout;
    Toolbar toolbar;
    private String TAG = "PlayerActivity";
    // Broadcast receiver to determine when music player has been prepared
    private BroadcastReceiver onPrepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            // When music player has been prepared, show controller
            controller.show(0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //-- TODO: Add useful items, such as the "Shuffle" icon, to the action bar ...
        //-- In menu_main.xml I had several app:showAsAction="always" calls, but it didn't fix it. Haven't gone back to fix that yet.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        setupNavigationView();
        setupToolbar();
        songQueueView = (ListView) findViewById(R.id.song_queue);
        songListView = (ListView) findViewById(R.id.song_list);
        songQueue = new ArrayList<Song>();
        getPermissions();
        ServerRunnable serverRunnable = new ServerRunnable(getApplicationContext());
        Thread thread = new Thread(serverRunnable);
        thread.start();
        config();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Set up receiver for media player onPrepared broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(onPrepareReceiver,
                new IntentFilter("MEDIA_PLAYER_PREPARED"));
        config();
        //getSongList();           // might not be config, might just need to get song list
        if (paused) {
            setController();
            paused = false;
        }
    }

    private void setUpTimeStamp(Long receivedTime, String action) {
        Log.d(TAG, "timeStampReceived: " + receivedTime);
        if (action.equals("Play")) {
            new SongActionAtTimeStamp(getApplicationContext()).execute(
                    receivedTime.toString(), "Play");
        } else if (action.equals("Pause")) {
            new SongActionAtTimeStamp(getApplicationContext()).execute(
                    receivedTime.toString(), "Pause");
        } else {   //problem
            Log.d(TAG, "setuptimestamp method issue");
        }
    }

    private void setUpReceivedSong(String songpath) {
        Log.d(TAG, "setupreceivedsong");
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(getApplicationContext(), Uri.parse(songpath));
            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Artist: " + artist);
            final Song receivedSong = new Song(songpath, title, artist, 0);
            Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    addSongToQueue(receivedSong);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // start UI set up -- Connor
    private void setupNavigationView() {

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navView = (NavigationView) findViewById(R.id.navList);
        navView.setNavigationItemSelectedListener(this);
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        // Show menu icon
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }

    public void config() {
        getSongList();
        updateSongAdapters();
        setController();
    }

    public void openPlayerActivity() {
        //-- Mike 10/28/15
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }

    public void openWifiActivity() {
        //-- Mike 1/6/16
        Intent intent = new Intent(this, WiFiDirectActivity.class);
        startActivity(intent);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_jams:
                openJamsActivity();
                break;
            case R.id.nav_music_player:
                break;
            case R.id.nav_wifi:
                openWifiActivity();
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View v) {

    }

    // end UI setup
    public void getPermissions() {
        if (ContextCompat.checkSelfPermission(PlayerActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(PlayerActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! (that's not done yet - MM)
                ActivityCompat.requestPermissions(PlayerActivity.this, //-- try again to ask permission
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        55);
            } else {
                // Request the permission the first time we ask
                ActivityCompat.requestPermissions(PlayerActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        55);
                //-- Resulting method is onRequestPermissionsResult
            }
        } else
            config();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 55: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //-- Permission granted
                    config();
                } else { // Permission denied
                }
                return;
            }
        }
    }

    //-- TODO: Reorder the methods in this and MusicService.java to have a sensible ordering, to make it easier to find stuff
    public void getSongList() {
        //retrieve song info
        Log.d(TAG, "Get Song List");
        songList = new ArrayList<Song>();
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);

            //add songs to list

            //-- TODO: Make sure we're looking at a file with a valid type
            //-- At the moment I've tested MP3 files and find that they play. MPEG-4, for example, do not.
            //-- Use this link to determine compatible file types: http://developer.android.com/guide/appendix/media-formats.html

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                if (thisArtist.contains("<unknown>") || thisTitle.contains("<unknown>")) //-- Temporary fix to some unwanted items coming up as songs
                    continue;
                songList.add(new Song(thisId, thisTitle, thisArtist, 0));
                //-- TODO: The last parameter corresponds to the ID of the owner
                //-- We will want to update this 0 to something useful, so we can list songs by person
            }
            while (musicCursor.moveToNext());
            musicCursor.close();
        }

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    public void transferQueueSongs(Song songToTransfer) {
        Log.d("devicedetailfragment:", "onActivityResult");
        // User has picked a song. Transfer it to group owner i.e peer using
        // FileTransferService.
        WifiSingleton wifiSingleton = WifiSingleton.getInstance();
        if (wifiSingleton.getInfo() != null) {
            //Uri uri = data.getData();
            long currSong = songToTransfer.getID();        //get id
            Uri uri = ContentUris.withAppendedId( //set uri
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currSong);
            Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    wifiSingleton.getInfo().groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8990);
            Log.d("DeviceDetailFragment", "startService about to be called");
            startService(serviceIntent);
        }
    }

    //-- Song Manipulation between queues - Mike - 12-6
    public void addToQueue(View view) {
        Song songToAdd = songList.get(getListRowIndex(view));
        //songList.remove(songToAdd);
        songQueue.add(songToAdd);
        updateSongAdapters();
        transferQueueSongs(songToAdd);
    }


    public void addSongToQueue(Song receivedSong) {
        try {
            boolean addedSong = false;
            for (int i = 0; i < songList.size(); i++) {
                if (songCompare(receivedSong, songList.get(i))) {
                    Log.d(TAG, "Song found in list, adding to queue");
                    songQueue.add(songList.get(i));
                    addedSong = true;
                    break;
                }
            }
            if (!addedSong) {
                // couldn't find song in list, add to list and queue
                Log.d(TAG, "Couldn't find song in list, added to list and queue");
                songList.add(receivedSong);
                songQueue.add(receivedSong);
            }
            updateSongAdapters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean songCompare(Song song1, Song song2) {
        if (song1.getArtist().equals(song2.getArtist()) && song1.getID() == song2.getID() &&
                song1.getTitle().equals(song2.getTitle())) {
            return true;
        }
        return false;
    }

    public void removeFromQueue(View view) {
        int index = getQueueRowIndex(view);
        if (index != -1) {
            Song songToRemove = songQueue.get(index);
            //songList.add(songToRemove);
            songQueue.remove(songToRemove);
            updateSongAdapters();
        }
    }

    public void moveUp(View view) {
        int index = getQueueRowIndex(view);
        if (index > 0)
            Collections.swap(songQueue, index, index - 1);
        updateSongAdapters();
    }

    public void moveDown(View view) {
        int index = getQueueRowIndex(view);
        if (index < (songQueue.size() - 1))
            Collections.swap(songQueue, index, index + 1);
        updateSongAdapters();
    }

    public int getListRowIndex(View view) {
        View parentRow = (View) view.getParent();
        if (parentRow.findViewById(R.id.thisLayout) == null)
            parentRow = (View) parentRow.getParent();
        return songListView.getPositionForView(parentRow);
    }

    public int getQueueRowIndex(View view) {
        View parentRow = (View) view.getParent();
        if (parentRow.findViewById(R.id.thisLayout) == null)
            parentRow = (View) parentRow.getParent();
        return songQueueView.getPositionForView(parentRow);
    }
    //-- End song manipulation between queues

    public void updateSongAdapters() {
        SongQueueAdapter songAdt = new SongQueueAdapter(this, songQueue);
        songQueueView.setAdapter(songAdt);

        SongListAdapter songAdt2 = new SongListAdapter(this, songList);
        songListView.setAdapter(songAdt2);

        if (musicSrv != null)
            musicSrv.setList(songQueue);
    }

    public void songPicked(View view) {

        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        Log.d(TAG, "execute offset class");
        try {
            new SendTimeStamp(getApplicationContext()).execute("Play");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendTimeStamp(long serverPlayTime) {
        Log.d(TAG, "SendTimeStamp");

        //IT"S TIME TO SEND THE TIME
        WifiSingleton wifiSingleton = WifiSingleton.getInstance();
        if (wifiSingleton.getInfo() != null) {
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_TIMESTAMP);
            serviceIntent.putExtra(FileTransferService.EXTRAS_TIMESTAMP, "" + serverPlayTime);
            Log.d("PlayerActivity", "string version of long timestamp to be sent: " + serverPlayTime);
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    wifiSingleton.getInfo().groupOwnerAddress.getHostAddress());

            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8990);
            Log.d("PlayerActivity", "startService about to be called for sending timestamp");
            startService(serviceIntent);
        }
    }

    private void setController() {
        //-- Method to set up the controller. This is the object controlling the playback options, of Skip, Play/Pause, and Seek.
        if (controller == null)
            controller = new MusicController(this); //-- only make a new controller if it's null

        controller.setPrevNextListeners(
                //-- "Next" button listener
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playNext();
                    }
                },
                //-- "Previous" button listener
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPrev();
                    }
                });
        controller.setMediaPlayer(this);
        //-- Place the controller, with the { |<, <<, play/pause, >>, >| } buttons, at the bottom (anchored to the bottom of the layout)
        controller.setAnchorView(findViewById(R.id.playerLayout));
        controller.setEnabled(true);
    }

    private void playNext() {
        musicSrv.playNext();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(1);
    }

    private void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        unbindService(musicConnection);
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if ((musicSrv != null) && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if ((musicSrv != null) && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else
            return 0;
    }

    @Override
    public boolean isPlaying() {
        if ((musicSrv != null) && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public void openJamsActivity() {
        //-- Mike 10/28/15
        Intent intent = new Intent(this, JamListActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
            /*if(timeStamp != null) {
                long timestamp = Long.getLong(timeStamp);
                timeStamp = null;
                setUpTimeStamp(timestamp);
            }*/
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    /*
    This class is used on the receiving end. After the song and time stamp have been received,
    set the receiving device to play or pause the song at a certain time.
     */
    class SongActionAtTimeStamp extends AsyncTask<String, Void, Long> {
        private String actionstring = null;
        private Context context;

        public SongActionAtTimeStamp(Context c) {
            context = c;
        }
        @Override
        protected Long doInBackground(String... voids) {
            try {
                String receivedtimestampstring = voids[0];
                Long receivedtimestamp = Long.parseLong(receivedtimestampstring);
                actionstring = voids[1];
                // get current time offset
                TimeSync timeSync = new TimeSync();
                long offset = timeSync.getNTPOffset();
                Log.d(TAG, "Offset: " + offset);
                long localPlayTime = receivedtimestamp - offset; // play song in 30 system seconds
                Log.d(TAG, "LocalPlayTime: " + localPlayTime);
                return localPlayTime;
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return Long.valueOf(-1);
        }

        @Override
        protected void onPostExecute(Long localPlayTime) {
            // TODO wait, and then play song
            Log.d(TAG, "OnPostExecute");
            SongTimer songtimer = new SongTimer(localPlayTime, musicSrv, controller, actionstring,
                    context);
        }
    }

    /*
    This class is used after a play or pause command has been issued and sends a time stamp to
     another device and then sets a timer for the action.
     */
    class SendTimeStamp extends AsyncTask<String, Void, Long> {
        private String actionstring = null;
        private String TAG = "SendTimeStamp";
        private Context context;
        public SendTimeStamp(Context c) {
            this.context = c;
        }

        @Override
        protected Long doInBackground(String... voids) {
            actionstring = voids[0];
            long result = 0;
            Log.d(TAG, "doInBackground");
            try {
                // get current time offset
                TimeSync timeSync = new TimeSync();
                long offset = timeSync.getNTPOffset();
                Log.d(TAG, "Offset: " + offset);
                long localPlayTime = System.currentTimeMillis() + 10000; // play song in 30 system seconds
                Log.d(TAG, "LocalPlayTime: " + localPlayTime);
                long internetPlayTime = timeSync.setServerPlayTime(offset, localPlayTime);
                Log.d(TAG, "ServerPlayTime: " + internetPlayTime);
                sendTimeStamp(internetPlayTime);
                result = localPlayTime;
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return result;
        }

        @Override
        protected void onPostExecute(Long localPlayTime) {
            // TODO wait, and then play song
            Log.d(TAG, "OnPostExecute");
            SongTimer songtimer = new SongTimer(localPlayTime, musicSrv, controller, actionstring,
                    context);
        }
    }

    public class ServerRunnable implements Runnable {

        private Context context;
        private String TAG = "ServerThread";
        private String dataType = null;

        /**
         * @param context
         */
        public ServerRunnable(Context context) {
            this.context = context;
        }
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = null;
                serverSocket = new ServerSocket(8990);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                while(true) {
                    Socket client = serverSocket.accept();
                    Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                    // receive data type string
                    DataInputStream is = new DataInputStream(client.getInputStream());
                    dataType = is.readUTF();
                    //check if mimeType is all numbers or not
                    String timestamp;
                    Log.d(TAG, "datatype: " + dataType);
                    switch (dataType) {
                        case "Play":
                            timestamp = receiveTimeStamp(client);
                            receivedCommunication(timestamp);
                            break;
                        case "Pause":
                            timestamp = receiveTimeStamp(client);
                            receivedCommunication(timestamp);
                            break;
                        case "File":
                            String receivedPath = receiveFile(client);
                            receivedCommunication(receivedPath);
                            break;
                        case "IP":
                            String receivedIP = receiveIP(client);
                            receivedCommunication(receivedIP);
                            break;
                        default:
                            Log.e(TAG, "No case match");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private String receiveIP(Socket client) {
            Log.d(TAG, "receiveIPMethod");
            DataInputStream is = null;
            try {
                is = new DataInputStream(client.getInputStream());
                String ip = is.readUTF();
                return ip;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String receiveTimeStamp(Socket client) {
            Log.d(TAG, "recieveTimeStamp");
            DataInputStream is = null;
            try {
                is = new DataInputStream(client.getInputStream());
                String timeStamp = is.readUTF();
                return timeStamp;
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

        public void receivedCommunication(String result) {
            String musicaction;
            if(dataType != null) {
                switch (dataType) {
                    case "File":
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
                                setUpReceivedSong(path);
                            }
                        });
                        break;
                    case "IP":
                        dataType = null;
                        // do stuff with IP
                        break;
                    case "Play":
                        musicaction = dataType;
                        dataType = null;
                        Long playtime = Long.parseLong(result);
                        //create
                        setUpTimeStamp(playtime, musicaction);
                        break;
                    case "Pause":
                        musicaction = dataType;
                        dataType = null;
                        Long pausetime = Long.parseLong(result);
                        setUpTimeStamp(pausetime, musicaction);
                        break;
                    default:
                        Log.e(TAG, "No case match");
                        break;
                }
            }
        }

        public String getFileExtention(String mime) throws MimeTypeException {
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

        public boolean copyFile(InputStream inputStream, OutputStream out) {
            byte buf[] = new byte[1024];
            int len;
            long startTime = System.currentTimeMillis();
            Log.d(WiFiDirectActivity.TAG, "starting tranfser of file in copy file");
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
                Log.d(WiFiDirectActivity.TAG, e.toString());
                return false;
            }
            return true;
        }
    }
}
