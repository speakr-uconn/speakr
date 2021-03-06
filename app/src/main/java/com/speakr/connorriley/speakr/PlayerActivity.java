package com.speakr.connorriley.speakr;

import android.Manifest;
import android.support.v7.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.os.Looper;
import android.os.Bundle;
import android.os.Handler;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
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
import android.view.Menu;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends HamburgerActivity implements View.OnClickListener, MediaPlayerControl {
    //-- Referenced the following websites:
    //-- Pulling song list: http://code.tutsplus.com/tutorials/create-a-music-player-on-android-project-setup--mobile-22764
    //-- MediaPlayer itself: http://code.tutsplus.com/tutorials/create-a-music-player-on-android-song-playback--mobile-22778
    //-- User controls: http://code.tutsplus.com/tutorials/create-a-music-player-on-android-user-controls--mobile-22787

    private ArrayList<Song> songList, songQueue;
    private ListView songQueueView, songListView;
    ImageView album_art;
    private MusicService musicSrv;
    private int upIndex, downIndex, removeIndex;
    private String SynchronizationMode = "Local";     // could be NTPServer, LocalAverage, Local
    private Intent playIntent;
    final private long ACTION_DELAY = 5000;
    private boolean musicBound = false;
    private MusicController controller;
    private PlayerActivityReceiver receiver;
    private boolean paused = false, playbackPaused = false;     // playbackpuased is not updated correctly
    ProgressDialog progressDialog = null;
    DrawerLayout drawerLayout;
    Toolbar toolbar;
    private Thread serverthread;
    private String TAG = PlayerActivity.class.getSimpleName();
    private Long starttime;
    private ServerRunnable serverRunnable;

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
        serverRunnable = new ServerRunnable(getApplicationContext());
        setContentView(R.layout.activity_player);
        setupNavigationView();
        setupToolbar();

        songQueueView = (ListView) findViewById(R.id.song_queue);
        configureSongQueueView(songQueueView);
        songListView = (ListView) findViewById(R.id.song_list);
        songQueue = new ArrayList<>();
        config();
        setController();
        getPermissions();
    }

    private void configureSongQueueView(ListView songQueueView) {
        songQueueView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.v("LongClick", "longclicked");
                final int index = position; //needed to pass position variable to alert dialog

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_NEGATIVE:
                                //if user says no, do nothing
                                break;
                            case DialogInterface.BUTTON_POSITIVE:
                                if (index != -1) {
                                    Song songToRemove = songQueue.get(index);
                                    songQueue.remove(songToRemove);
                                    sendQueueAction("Remove", Integer.toString(index));
                                    updateSongAdapters();
                                }
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setMessage("Remove song from queue?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(WifiSingleton.getInstance().getInfo() != null)
            getMenuInflater().inflate(R.menu.menu_in_jam, menu);
        else
            getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(serverRunnable == null) {
            serverRunnable = new ServerRunnable(getApplicationContext());
        }
        if(serverthread == null) {
            serverthread = new Thread(serverRunnable);
        }
        if(!serverthread.isAlive()) {
            serverthread.start();
        }
        IntentFilter filter = new IntentFilter(PlayerActivityReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new PlayerActivityReceiver();
        registerReceiver(receiver, filter);
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
        // Set up receiver for media player onPrepared broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(onPrepareReceiver,
                new IntentFilter("MEDIA_PLAYER_PREPARED"));
        config();
        if (paused) {
            if(playbackPaused) {
                setController();
            }
            paused = false;
        }
        WifiSingleton.getInstance().setPlayerActivity(this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "On Pause");
        try {
            WifiSingleton.getInstance().disconnect();
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        //controller.hide();
        paused = true;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(musicConnection);
        unregisterReceiver(receiver);
        stopService(playIntent);
    }

    private void setUpTimeStamp(Long receivedTime, String actionstring) {
        Log.d(TAG, "timeStampReceived: " + receivedTime);
        new SongActionAtTimeStamp(getApplicationContext()).execute(
                receivedTime.toString(), actionstring);
    }

    private void showProgressDialog(String s){
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = ProgressDialog.show(this, s,
                "Please wait");
    }

    private void setUpReceivedSong(String songpath) {
        Log.d(TAG, "setupreceivedsong");
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(getApplicationContext(), Uri.parse(songpath));
            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            byte[] art = mmr.getEmbeddedPicture();
            Bitmap albumArt;
            if( art != null ){
                albumArt = BitmapFactory.decodeByteArray(art, 0,art.length);
            }
            else{
                albumArt = null;
            }

            if(title == null) {
                title = "NULL";
            }
            if(artist == null) {
                artist = "NULL";
            }
            Log.d(TAG, "Title: " + title);
            Log.d(TAG, "Artist: " + artist);

            final Song receivedSong = new Song(songpath, title, artist, 0, albumArt);
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
        songList = WifiSingleton.getInstance().getSongList();
        updateSongAdapters();
        //setController();
    }

    public void openPlayerActivity() {
        //-- Mike 10/28/15
        Intent intent = new Intent(this, PlayerActivity.class);
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
    @Deprecated
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
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Albums.ALBUM_ID);

            //add songs to list

            //-- TODO: Make sure we're looking at a file with a valid type
            //-- At the moment I've tested MP3 files and find that they play. MPEG-4, for example, do not.
            //-- Use this link to determine compatible file types: http://developer.android.com/guide/appendix/media-formats.html

            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                long albumID = musicCursor.getLong(albumColumn);
                Bitmap bm = null;
                try
                {
                    final Uri sArtworkUri = Uri
                            .parse("content://media/external/audio/albumart");

                    Uri uri = ContentUris.withAppendedId(sArtworkUri, albumID);

                    ParcelFileDescriptor pfd = getContentResolver()
                            .openFileDescriptor(uri, "r");

                    if (pfd != null)
                    {
                        FileDescriptor fd = pfd.getFileDescriptor();
                        bm = BitmapFactory.decodeFileDescriptor(fd);
                    }
                } catch (Exception e) {
                }
                if (thisArtist.contains("<unknown>") || thisTitle.contains("<unknown>")) //-- Temporary fix to some unwanted items coming up as songs
                    continue;
                songList.add(new Song(thisId, thisTitle, thisArtist, 0, bm));
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
            Log.d(TAG, "Intent----------- " + uri);
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
            if(!wifiSingleton.getInfo().isGroupOwner){
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getInfo().groupOwnerAddress.getHostAddress());
            }
            else{
                Log.d(TAG, "This device is a group owner.");

                Log.d(TAG, "MemberIP: " + WifiSingleton.getInstance().getMemberIP());
                Log.d(TAG, "Host Address:  " + WifiSingleton.getInstance().getInfo().groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getMemberIP());
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, 8990);
            Log.d("DeviceDetailFragment", "startService about to be called");
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            showProgressDialog("Sending Music file");
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
                if (receivedSong != null && songList.get(i) != null &&
                        songCompare(receivedSong, songList.get(i))) {
                    Log.d(TAG, "Song found in list, adding to queue");
                    songQueue.add(songList.get(i));
                    addedSong = true;
                    break;
                }
            }
            if (!addedSong) {
                // couldn't find song in list, add to list and queue
                Log.d(TAG, "Couldn't find song in list, added to list and queue");
                //songList.add(receivedSong);
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

    @Deprecated
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
        if (index > 0) {
            Collections.swap(songQueue, index, index - 1);
            sendQueueAction("MoveUp", Integer.toString(index));
        }
        updateSongAdapters();
    }

    public void moveDown(View view) {
        int index = getQueueRowIndex(view);
        if (index < (songQueue.size() - 1)) {
            Collections.swap(songQueue, index, index + 1);
            sendQueueAction("MoveDown", Integer.toString(index));
        }
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
        if(WifiSingleton.getInstance().isConnected()) {
            switch (SynchronizationMode) {
                case "NTPServer":
                    new SendTimeStamp(getApplicationContext()).execute("Play");
                    break;
                case "Local":
                    try {
                        sendSetSong("SetSong", view.getTag().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else {
            musicSrv.playSong();
        }
        playbackPaused = false;
    }
    private void sendSetSong(String message, String index) {
        WifiSingleton wifiSingleton = WifiSingleton.getInstance();
        if (wifiSingleton.getInfo() != null) {
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_SETSONG);
            serviceIntent.putExtra(FileTransferService.EXTRAS_SONG, index);
            serviceIntent.putExtra("Action", message);
            if (!wifiSingleton.getInfo().isGroupOwner) {
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getInfo().groupOwnerAddress.getHostAddress());
            } else {
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getMemberIP());
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, 8990);
            startService(serviceIntent);
        }
    }
    private void sendQueueAction(String message, String index) {
        WifiSingleton wifiSingleton = WifiSingleton.getInstance();
        if (wifiSingleton.getInfo() != null) {
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_MOVEQUEUE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_MOVEINDEX, index);
            serviceIntent.putExtra("Action", message);
            if (!wifiSingleton.getInfo().isGroupOwner) {
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getInfo().groupOwnerAddress.getHostAddress());
            } else {
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getMemberIP());
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, 8990);
            startService(serviceIntent);
        }
    }
    public void sendMessage(String message, String extra) {
        //IT"S TIME TO SEND THE TIME :)
        WifiSingleton wifiSingleton = WifiSingleton.getInstance();
        if (wifiSingleton.getInfo() != null) {
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_TIMESTAMP);
            serviceIntent.putExtra("Action", message);
            if(!wifiSingleton.getInfo().isGroupOwner){
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getInfo().groupOwnerAddress.getHostAddress());
            } else {
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getMemberIP());
            }
            if(message.equals("LocalPause_1") || message.equals("LocalPause_2")){
                serviceIntent.putExtra(FileTransferService.EXTRAS_PAUSETIME,
                        extra);
                controller.setPauseTime(extra);
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, 8990);
            Log.d("PlayerActivity", "startService about to be called for sending timestamp");
            if(extra != null && extra.equals("connorhack")) {
                starttime = System.currentTimeMillis();
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_TIMESTAMP, "" + starttime);
            startService(serviceIntent);
        }
    }
    private void sendTimeStamp(long serverPlayTime, String actionString) {
        Log.d(TAG, "SendTimeStamp");

        //IT"S TIME TO SEND THE TIME :)
        WifiSingleton wifiSingleton = WifiSingleton.getInstance();
        if (wifiSingleton.getInfo() != null) {
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_TIMESTAMP);
            serviceIntent.putExtra("Action", actionString);
            serviceIntent.putExtra(FileTransferService.EXTRAS_TIMESTAMP, "" + serverPlayTime);
            Log.d("PlayerActivity", "string version of long timestamp to be sent: " + serverPlayTime);
            if(!wifiSingleton.getInfo().isGroupOwner){
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getInfo().groupOwnerAddress.getHostAddress());
            }

            else{
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS,
                        wifiSingleton.getMemberIP());
            }



            serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, 8990);
            Log.d("PlayerActivity", "startService about to be called for sending timestamp");
            //getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            //        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            startService(serviceIntent);
        }
    }

    private void setController() {
        //-- Method to set up the controller. This is the object controlling the playback options, of Skip, Play/Pause, and Seek.
        boolean isConnected = WifiSingleton.getInstance().isConnected();
        if (controller == null) {
            controller = new MusicController(this, !isConnected); //-- only make a new controller if it's null
        }

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
        controller.setAnchorView(findViewById(R.id.playerLayoutBottom));
        controller.setEnabled(true);
    }

    private void playNext() {
        if(musicSrv.isLastSong()) //-- if we're at the last song in the queue, don't do it
            return;

        if (WifiSingleton.getInstance().isConnected()) {
            switch (SynchronizationMode) {
                case "NTPServer":
                    new SendTimeStamp(getApplicationContext()).execute("Next");
                    break;
                case "Local":
                    try {
                        starttime = System.currentTimeMillis();
                        sendMessage("LocalNext_1", null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else {
            musicSrv.playNext();
        }
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }

    }

    private void playPrev() {
        if(musicSrv.isFirstSong())
            //-- if we're at the first song in the queue, don't do it
            return;


        if(WifiSingleton.getInstance().isConnected()) {
            switch (SynchronizationMode) {
                case "NTPServer":
                    new SendTimeStamp(getApplicationContext()).execute("Previous");
                    break;
                case "Local":
                    try {
                        starttime = System.currentTimeMillis();
                        sendMessage("LocalPrevious_1", null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else {
            musicSrv.playPrev();
        }
        if (playbackPaused) {
            setController();
            playbackPaused = false;
        }
    }

    @Override
    public void start() {
        // restart the music player after a pause.
        if(WifiSingleton.getInstance().isConnected()) {
            switch (SynchronizationMode) {
                case "NTPServer":
                    new SendTimeStamp(getApplicationContext()).execute("Resume");
                    break;
                case "Local":
                    try {
                        starttime = System.currentTimeMillis();
                        sendMessage("LocalResume_1", null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else {
            musicSrv.go();
        }
        playbackPaused = false;
    }

    @Override
    public void seekTo(int pos) {
        if(WifiSingleton.getInstance().isConnected()) {
            Toast.makeText(PlayerActivity.this, "Seeking Disabled",
                    Toast.LENGTH_SHORT).show();
        } else {
            musicSrv.seek(pos);
        }
    }

    @Override
    public void pause() {
        // Pause music player
        if(WifiSingleton.getInstance().isConnected()) {
            switch (SynchronizationMode) {
                case "NTPServer":
                    playbackPaused = true;
                    new SendTimeStamp(getApplicationContext()).execute("Pause");
                    break;
                case "Local":
                    try {
                        int currPos = musicSrv.getPosn();
                        long pausePos;
                        if ((currPos + ACTION_DELAY) < musicSrv.getDur())
                            pausePos = (currPos + ACTION_DELAY);
                        else
                            pausePos = (long) currPos;

                        starttime = System.currentTimeMillis();
                        sendMessage("LocalPause_1", "" + pausePos);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        } else {
            musicSrv.pausePlayer();
        }
        //musicSrv.go(); //-- calling this has the thing doing it right

        Log.d(TAG, "SHOW CONTROLLER PROPERLY PLS");
        controller.holdup();
        //controller.show(0);
        //playbackPaused = true;
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
        if(WifiSingleton.getInstance().isConnected()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean canSeekForward() {
        if(WifiSingleton.getInstance().isConnected()) {
            return false;
        } else {
            return true;
        }
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
            case R.id.action_disconnect:
                WifiSingleton.getInstance().setConnected(false);
                WifiSingleton.getInstance().disconnect();
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
    class SongActionAtTimeStamp extends AsyncTask<String, String, Long> {
        private String actionstring = null;
        private Context context;

        public SongActionAtTimeStamp(Context c) {
            context = c;
        }

        private long getAverageNTPOffset(TimeSync timeSync, int n) {
            long offsetAcc = 0;
            for (int i = 0; i < n; i++) {
                long offset = timeSync.getNTPOffset();
                Log.d(TAG, "Offset " + i + ": " + offset);
                offsetAcc += offset;
            }
            return  (offsetAcc/n);  //NOTE: this will round the value
        }

        @Override
        protected Long doInBackground(String... voids) {
            try {
                String receivedtimestampstring = voids[0];
                Long receivedtimestamp = Long.parseLong(receivedtimestampstring);
                actionstring = voids[1];

                publishProgress(actionstring);
                // get current time offset
                TimeSync timeSync = new TimeSync();
                //long offset = timeSync.getNTPOffset();
                long offset = getAverageNTPOffset(timeSync, 5);
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
        protected void onProgressUpdate(String... strings) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            showProgressDialog("Preparing '" + strings[0] + "' request.");
        }

        @Override
        protected void onPostExecute(Long localPlayTime) {
            Log.d(TAG, "OnPostExecute");
            progressDialog.dismiss();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            Toast.makeText(PlayerActivity.this, "Request processed. One moment.",
                    Toast.LENGTH_SHORT).show();
            SongTimer songtimer = new SongTimer(localPlayTime, musicSrv, controller, actionstring,
                    context, false, null);
        }
    }

    /*
    This class is used after a play or pause command has been issued and sends a time stamp to
     another device and then sets a timer for the action.
     */
    class SendTimeStamp extends AsyncTask<String, String, Long> {
        private String actionstring = null;
        private String TAG = "SendTimeStamp";
        private Context context;

        public SendTimeStamp(Context c) {
            this.context = c;
        }

        //ping the NTP server n times for an offset and calculate the
        //average of those values
        private long getAverageNTPOffset(TimeSync timeSync, int n) {
            long offsetAcc = 0;
            for (int i = 0; i < n; i++) {
                long offset = timeSync.getNTPOffset();
                Log.d(TAG, "Offset " + i + ": " + offset);
                offsetAcc += offset;
            }
            return  (offsetAcc/n);  //NOTE: this will round the value
        }

        @Override
        protected Long doInBackground(String... voids) {
            actionstring = voids[0];
            publishProgress(actionstring);
            long result = 0;
            Log.d(TAG, "doInBackground");
            try {
                // get current time offset
                TimeSync timeSync = new TimeSync();
                //long offset = timeSync.getNTPOffset();
                long offset = getAverageNTPOffset(timeSync, 10); //get 5 offset values
                Log.d(TAG, "Average Offset: " + offset);
                long localPlayTime = System.currentTimeMillis() + ACTION_DELAY; // take action in 15 seconds
                Log.d(TAG, "LocalPlayTime: " + localPlayTime);
                long internetPlayTime = timeSync.setServerPlayTime(offset, localPlayTime);
                Log.d(TAG, "ServerPlayTime: " + internetPlayTime);
                sendTimeStamp(internetPlayTime, actionstring);
                result = localPlayTime;
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(String... strings) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            showProgressDialog("Preparing '" + strings[0] + "' request.");
        }
        @Override
        protected void onPostExecute(Long localPlayTime) {
            Log.d(TAG, "OnPostExecute");
            progressDialog.dismiss();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            Toast.makeText(PlayerActivity.this, "Play/Pause request successfully sent.",
                    Toast.LENGTH_SHORT).show();
            SongTimer songtimer = new SongTimer(localPlayTime, musicSrv, controller, actionstring,
                    context, false, null);
        }
    }

    public class ServerRunnable implements Runnable {

        private Context context;
        private String TAG = "ServerThread";
        private String dataType = null;
        private Handler mainHandler;

        /**
         * @param context
         */
        public ServerRunnable(Context context) {
            this.context = context;
            mainHandler = new Handler(context.getMainLooper());
            if(Looper.myLooper() == null) {
                Looper.prepare();
            }
        }

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = null;
                serverSocket = new ServerSocket(8990);
                while(true) {
                    Socket client = serverSocket.accept();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            showProgressDialog("Receiving data.");
                        }
                    });
                    // receive data type string
                    DataInputStream is = new DataInputStream(client.getInputStream());
                    dataType = is.readUTF();
                    String timestamp, pauseTime;
                    switch (dataType) {
                        case "LocalPlay_1":
                            // set timer for five seconds
                            receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Play request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            sendMessage("LocalPlay_2", null);
                            new SongTimer(ACTION_DELAY, musicSrv, controller, "Play", context, true, null);
                            break;
                        case "LocalPlay_2":
                            receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Play request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            Long playlatency = (System.currentTimeMillis() - starttime)/2;
                            new SongTimer(ACTION_DELAY - playlatency, musicSrv, controller, "Play",
                                    context, true, null);
                            break;
                        case "LocalPause_1":
                            pauseTime = receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Pause request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            sendMessage("LocalPause_2", pauseTime);
                            new SongTimer(ACTION_DELAY, musicSrv, controller, "Pause", context, true, pauseTime);
                            break;
                        case "LocalPause_2":
                            pauseTime = receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Pause request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            long pauselatency = (System.currentTimeMillis() - starttime)/2;
                            new SongTimer(ACTION_DELAY - pauselatency, musicSrv, controller, "Pause",
                                    context, true, pauseTime);
                            break;
                        case "LocalNext_1":
                            receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Next request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            sendMessage("LocalNext_2", null);
                            new SongTimer(ACTION_DELAY, musicSrv, controller, "Next", context, true, null);
                            break;
                        case "LocalNext_2":
                            receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Next request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            long nextlatency = (System.currentTimeMillis() - starttime)/2;
                            new SongTimer(ACTION_DELAY - nextlatency, musicSrv, controller, "Next",
                                    context, true, null);
                            break;
                        case "LocalPrevious_1":
                            receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Previous request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            sendMessage("LocalPrevious_2", null);
                            new SongTimer(ACTION_DELAY, musicSrv, controller, "Previous", context, true, null);
                            break;
                        case "LocalPrevious_2":
                            receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Previous request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            long previousLatency = (System.currentTimeMillis() - starttime)/2;
                            new SongTimer(ACTION_DELAY - previousLatency, musicSrv, controller, "Previous",
                                    context, true, null);
                            break;
                        case "LocalResume_1":
                            receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Resume request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            sendMessage("LocalResume_2", null);
                            new SongTimer(ACTION_DELAY, musicSrv, controller, "Resume", context, true, null);
                            break;
                        case "LocalResume_2":
                            receiveTimeStamp(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Resume request received",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            long resumeLatency = (System.currentTimeMillis() - starttime)/2;
                            new SongTimer(ACTION_DELAY - resumeLatency, musicSrv, controller, "Resume",
                                    context, true, null);
                            break;
                        case "MoveUp":
                            upIndex = receiveInt(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Move Song Up in Queue",
                                            Toast.LENGTH_SHORT).show();
                                    Collections.swap(songQueue, upIndex, upIndex - 1);
                                    updateSongAdapters();
                                    musicSrv.updateSongPos();
                                }
                            });
                            break;
                        case "MoveDown":
                            downIndex = receiveInt(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Move Song Down in Queue",
                                            Toast.LENGTH_SHORT).show();
                                    Collections.swap(songQueue, downIndex, downIndex + 1);
                                    updateSongAdapters();
                                    musicSrv.updateSongPos();
                                }
                            });
                            break;
                        case "Remove":
                            removeIndex = receiveInt(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Remove song from Queue",
                                            Toast.LENGTH_SHORT).show();
                                    Song songToRemove = songQueue.get(removeIndex);
                                    songQueue.remove(songToRemove);
                                    updateSongAdapters();
                                }
                            });
                            break;
                        case "SetSong":
                            int songIndex = receiveInt(client);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    Toast.makeText(PlayerActivity.this, "Set Song for Playback",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                            musicSrv.setSong(songIndex);
                            starttime = System.currentTimeMillis();
                            sendMessage("LocalPlay_1", null);
                            break;
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
                        case "Resume":
                            timestamp = receiveTimeStamp(client);
                            receivedCommunication(timestamp);
                            break;
                        case "Next":
                            timestamp = receiveTimeStamp(client);
                            receivedCommunication(timestamp);
                            break;
                        case "Previous":
                            timestamp = receiveTimeStamp(client);
                            receivedCommunication(timestamp);
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
        private int receiveInt(Socket client) {
            DataInputStream is = null;
            try {
                is = new DataInputStream(client.getInputStream());
                String index = is.readUTF();
                return Integer.parseInt(index);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }

        private String receiveFile(Socket client) {
            try {
                DataInputStream is = new DataInputStream(client.getInputStream());
                String mimeType = is.readUTF();
                String fileExtention = null;
                try {
                    fileExtention = getFileExtention(mimeType);
                } catch (MimeTypeException e) {
                    e.printStackTrace();
                }
                final File f = new File(context.getFilesDir().getParent() + "/"
                        + "/wifip2pshared-" + System.currentTimeMillis()
                        + fileExtention);
                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
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
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                        Toast.makeText(PlayerActivity.this, "File received",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                                setUpReceivedSong(path);
                            }
                        });
                        break;
                    case "IP":
                        dataType = null;
                        WifiSingleton.getInstance().setMemberIP(result);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(PlayerActivity.this, "IP received",
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case "Play":
                        musicaction = dataType;
                        dataType = null;
                        Long playtime = Long.parseLong(result);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(PlayerActivity.this, "Play request received",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        //create
                        setUpTimeStamp(playtime, musicaction);
                        break;
                    case "Pause":
                        musicaction = dataType;
                        dataType = null;
                        Long pausetime = Long.parseLong(result);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(PlayerActivity.this, "Pause request received",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        setUpTimeStamp(pausetime, musicaction);
                        break;
                    case "Resume":
                        musicaction = dataType;
                        dataType = null;
                        Long resumetime = Long.parseLong(result);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(PlayerActivity.this, "Resume request received",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        setUpTimeStamp(resumetime, musicaction);
                        break;
                    case "Next":
                        musicaction = dataType;
                        dataType = null;
                        Long nextPlayTime = Long.parseLong(result);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(PlayerActivity.this, "Next request received",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        setUpTimeStamp(nextPlayTime, musicaction);
                        break;
                    case "Previous":
                        musicaction = dataType;
                        dataType = null;
                        Long previousPlayTime = Long.parseLong(result);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(PlayerActivity.this, "Previous request received",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                        setUpTimeStamp(previousPlayTime, musicaction);
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
            try {
                while ((len = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.flush();
                out.close();
                inputStream.close();
                long endTime = System.currentTimeMillis() - startTime;

            } catch (IOException e) {
                return false;
            }
            return true;
        }
    }

    public class PlayerActivityReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "idk";
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra(FileTransferService.PARAM_OUT_MSG);
            progressDialog.dismiss();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            if(text.equals("Sent IP")){
                Toast.makeText(PlayerActivity.this, "IP successfully sent",
                        Toast.LENGTH_SHORT).show();
            }

            else if(text.equals("Sent File")){
                Toast.makeText(PlayerActivity.this, "File successfully sent",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
