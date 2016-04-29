package com.speakr.connorriley.speakr;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Michael on 10/21/2015.
 */
public class SongListAdapter extends BaseAdapter {
    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongListAdapter(Context c, ArrayList<Song> theSongs){
        songs = theSongs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        if (songs != null) {
            return songs.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.listrow, parent ,false);
        }

        //-- Map to song layout
        //LinearLayout songLay = (LinearLayout) songInf.inflate(R.layout.song, parent, false);
        RelativeLayout songLay = (RelativeLayout) songInf.inflate(R.layout.listrow, parent, false);

        //-- Get title and artist views
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
        ImageView albumView = (ImageView)songLay.findViewById(R.id.album_art);
                //get song using position
        Song currSong = songs.get(position);
        //get title and artist strings



        if(currSong.getTitle() != null && currSong.getTitle().length() > 25) {
            songView.setText(currSong.getTitle().substring(0, 24) + "...");
        }
        else{
            songView.setText(currSong.getTitle());
        }

        artistView.setText(currSong.getArtist());
        Bitmap albumArt = currSong.getAlbumArt();
        if(albumArt != null){
            albumView.setImageBitmap(albumArt);
        }

        else{
            albumView.setImageResource(R.drawable.machine);
        }

        final Button b = (Button) convertView.findViewById(R.id.add);

        //set position as tag
        songLay.setTag(position);
        return songLay;
    }
}