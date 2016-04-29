package com.speakr.connorriley.speakr;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by connorriley on 2/22/16.
 */
public class WifiSingleton {

    private static WifiSingleton instance = null;
    private String TAG = "WifiSingleton";
    private WifiP2pInfo info = null;
    private String memberIP;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private boolean connected;
    private ArrayList<Song> songList;
    private ContentResolver musicResolver;
    private PlayerActivity playerActivity;
    protected WifiSingleton() {

    }
    public static WifiSingleton getInstance() {
        if(instance == null) {
            instance = new WifiSingleton();
        }
        return instance;
    }

    public void setPlayerActivity(PlayerActivity p) {
        playerActivity = p;
    }
    public PlayerActivity getPlayerActivity() {
        return playerActivity;
    }
    public void disconnect() {
        if (mManager != null && mChannel != null) {
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "removeGroup onSuccess -");
                }
                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "removeGroup onFailure -" + reason);
                }
            });
        }
    }


    public WifiP2pInfo getInfo() {
        return info;
    }
    public void setInfo(WifiP2pInfo info) {
        this.info = info;
    }

    public void setManager(WifiP2pManager manager) {
        mManager = manager;
    }
    public void setConnected(boolean x) {
        connected = x;
    }
    public ArrayList<Song> getSongList() {
        return songList;
    }
    public void setChannel(WifiP2pManager.Channel channel) {
        mChannel = channel;
    }
    public void setMemberIP(String ip) {
        memberIP = ip;
        Log.d(TAG, "member ip: " + memberIP);
    }
    public boolean isConnected() {
        return connected;
    }
    public String getMemberIP(){
        return memberIP;
    }

    public void setMusicResolver(ContentResolver x) {
        musicResolver = x;
    }
    public boolean isGroupOwner() {
        return info.isGroupOwner;
    }
    public void makeSongList() {
        //retrieve song info
        Log.d(TAG, "Get Song List");
        songList = new ArrayList<Song>();
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
                    ParcelFileDescriptor pfd = musicResolver.openFileDescriptor(uri, "r");
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
}
