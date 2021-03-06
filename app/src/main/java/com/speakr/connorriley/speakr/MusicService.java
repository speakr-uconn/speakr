package com.speakr.connorriley.speakr;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.IBinder;

import java.io.IOException;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;
import android.widget.TextView;

/**
 * Created by Michael on 10/28/2015.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private MediaPlayer player; //media player
    private ArrayList<Song> songs; //song list
    private int songPosn; //current position
    private final IBinder musicBind = new MusicBinder();
    private String songTitle= "";
    private static final int NOTIFY_ID=1;
    private boolean shuffle=false;
    private Random rand;

    public void onCreate(){ //create the service
        super.onCreate();
        songPosn=0; // initialize position
        player = new MediaPlayer(); //create player
        initMusicPlayer();
        rand = new Random();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        if(player.isPlaying()) {
            player.stop();
        }

        player.release();
        return false;
    }

    public void initMusicPlayer(){
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    @Deprecated
    public void playReceivedSong(String songUri) {
        player.reset();

        Uri trackUri = Uri.parse(songUri);
        try {
            player.setDataSource(getApplicationContext(), trackUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("MusicService", "Prepare Async");
        player.prepareAsync();

    }
    public void playSong(){
        //play a song
        player.reset();
        Song playSong = songs.get(songPosn);        //get song
        songTitle=playSong.getTitle();
        Uri trackUri = null;
        if(playSong.getID() != -1) {
            // song has ID
            long currSong = playSong.getID();        //get id
            trackUri = ContentUris.withAppendedId( //set uri
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currSong);
        } else {
            // song has path, not ID (Likely because song was sent over wifidirect
            trackUri = Uri.parse(playSong.getPath());
        }
        Log.d("PlayerActivity", "Uri: " + trackUri);
        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }

        player.prepareAsync();
    }

    @Deprecated
    public void setShuffle(){
        if(shuffle)
            shuffle=false;
        else shuffle=true;
    }

    public void setList(ArrayList<Song> theSongs){
        songs=theSongs;
        updateSongPos();
    }

    public void setSong(int songIndex){
        songPosn=songIndex;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(player.getCurrentPosition() > 0) {
            mp.reset();
            songPosn++;
            if(songPosn < songs.size() && WifiSingleton.getInstance().isGroupOwner()) {
                WifiSingleton.getInstance().getPlayerActivity().sendMessage("LocalPlay_1", "connorhack");
            }
            //playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();

        Intent notIntent = new Intent(this, SplashActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent onPreparedIntent = new Intent("MEDIA_PLAYER_PREPARED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(onPreparedIntent);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(pendInt)
                .setSmallIcon(R.mipmap.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playPrev(){
        songPosn--;
        //if(songPosn < 0) //-- go back to the last song in the list
        //    songPosn=songs.size()-1;
        if(songPosn >= 0)
            playSong();
    }

    //-- skip to next
    public void playNext(){
        //-- TODO: Update the icon bar to show the "Pause" icon, not the "Play" icon ...
        //-- At the moment, if you're playing a song and skip to the next, it'll show the "Play" icon
        //-- Even though it's already playing. In that instance, hitting "Play" does nothing.
        if(shuffle){
            int newSong = songPosn;
            while(newSong==songPosn){
                newSong=rand.nextInt(songs.size());
            }
            songPosn=newSong;
        }
        else{
            songPosn++;
            //if(songPosn >= songs.size())
            //    songPosn=0;
        }
        if(songPosn < songs.size())
            playSong();
    }

    public boolean isFirstSong(){
        return (songPosn == 0);
    }

    public boolean isLastSong(){
        return (songPosn == (songs.size() - 1));
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void updateSongPos(){
        for(int i = 0; i < songs.size(); i++){
            String curTitle = songs.get(i).getTitle();
            if(songTitle.equals(curTitle)) {
                songPosn = i;
            }
        }
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }
}
