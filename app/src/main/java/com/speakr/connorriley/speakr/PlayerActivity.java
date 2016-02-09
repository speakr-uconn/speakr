package com.speakr.connorriley.speakr;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;

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
    private boolean musicBound=false;
    private MusicController controller;
    private boolean paused = false, playbackPaused = false;
    DrawerLayout drawerLayout;
    Toolbar toolbar;
    private String songUri;

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
        songUri = null;
        setContentView(R.layout.activity_player);
        setupNavigationView();
        setupToolbar();
        songQueueView = (ListView) findViewById(R.id.song_queue);
        songListView = (ListView) findViewById(R.id.song_list);
        songQueue = new ArrayList<Song>();
        getPermissions();
        TimeSyncTask timeSyncTask = new TimeSyncTask();
        timeSyncTask.execute(new TimeSync());
        // to start playing a song
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            songUri = bundle.getString("Song");
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Set up receiver for media player onPrepared broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(onPrepareReceiver,
                new IntentFilter("MEDIA_PLAYER_PREPARED"));
        getSongList();           // might not be config, might just need to get song list
        if(paused){
            setController();
            paused=false;
        }
    }

    private void setUpReceivedSong(String songUri) {
        Log.d("PlayerActivity", "setupreceivedsong");
        if(musicSrv != null) {
            Log.d("PlayerActivity","musicserv not null");
            musicSrv.playReceivedSong(songUri);
        }
    }
    // start UI set up -- Connor
    private void setupNavigationView(){

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navView = (NavigationView) findViewById(R.id.navList);
        navView.setNavigationItemSelectedListener(this);
    }
    private void setupToolbar(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
        }
        // Show menu icon
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }
    public void config(){
        getSongList();
        updateSongAdapters();
        setController();
    }
    public void openPlayerActivity(){
        //-- Mike 10/28/15
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }

    public void openWifiActivity(){
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
    public void getPermissions(){
        if (ContextCompat.checkSelfPermission(PlayerActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(PlayerActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! (that's not done yet - MM)
                ActivityCompat.requestPermissions(PlayerActivity.this, //-- try again to ask permission
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        55);
            }
            else {
                // Request the permission the first time we ask
                ActivityCompat.requestPermissions(PlayerActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        55);
                //-- Resulting method is onRequestPermissionsResult
            }
        }
        else
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
                }
                else { // Permission denied
                }
                return;
            }
        }
    }

    //-- TODO: Reorder the methods in this and MusicService.java to have a sensible ordering, to make it easier to find stuff
    public void getSongList() {
        //retrieve song info
        songList = new ArrayList<Song>();
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
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
                if(thisArtist.contains("<unknown>") || thisTitle.contains("<unknown>")) //-- Temporary fix to some unwanted items coming up as songs
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

    //-- Song Manipulation between queues - Mike - 12-6
    public void addToQueue(View view){
        Song songToAdd = songList.get(getListRowIndex(view));
        songList.remove(songToAdd);
        songQueue.add(songToAdd);

        updateSongAdapters();
    }

    public void removeFromQueue(View view){
        int index = getQueueRowIndex(view);
        if(index != -1) {
            Song songToRemove = songQueue.get(index);
            songList.add(songToRemove);
            songQueue.remove(songToRemove);
            updateSongAdapters();
        }
    }

    public void moveUp(View view){
        int index = getQueueRowIndex(view);
        if(index > 0)
            Collections.swap(songQueue, index, index-1);
        updateSongAdapters();
    }

    public void moveDown(View view){
        int index = getQueueRowIndex(view);
        if(index < (songQueue.size() - 1))
            Collections.swap(songQueue, index, index+1);
        updateSongAdapters();
    }

    public int getListRowIndex(View view){
        View parentRow = (View) view.getParent();
        if(parentRow.findViewById(R.id.thisLayout) == null)
            parentRow = (View) parentRow.getParent();
        return songListView.getPositionForView(parentRow);
    }

    public int getQueueRowIndex(View view){
        View parentRow = (View) view.getParent();
        if(parentRow.findViewById(R.id.thisLayout) == null)
            parentRow = (View) parentRow.getParent();
        return songQueueView.getPositionForView(parentRow);
    }
    //-- End song manipulation between queues

    public void updateSongAdapters(){
        SongQueueAdapter songAdt = new SongQueueAdapter(this, songQueue);
        songQueueView.setAdapter(songAdt);

        SongListAdapter songAdt2 = new SongListAdapter(this, songList);
        songListView.setAdapter(songAdt2);

        if(musicSrv != null)
            musicSrv.setList(songQueue);
    }

    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void setController(){
        //-- Method to set up the controller. This is the object controlling the playback options, of Skip, Play/Pause, and Seek.
        if(controller == null)
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

    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(1);
    }

    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
        if (songUri != null) {
            Log.d("PlayerActivity", "SongURI: " + songUri);
            setUpReceivedSong(songUri);
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
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
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if((musicSrv!=null) && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if((musicSrv!=null) &&  musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else
            return 0;
    }

    @Override
    public boolean isPlaying() {
        if((musicSrv!=null) && musicBound)
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

    public void openJamsActivity(){
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
                musicSrv=null;
                System.exit(0);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
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
}
