package com.speakr.connorriley.speakr;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Michael on 10/21/2015.
 */
public class SongQueueAdapter extends BaseAdapter {
    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongQueueAdapter(Context c, ArrayList<Song> theSongs){
        songs = theSongs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
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
            convertView = inflater.inflate(R.layout.queuerow, parent ,false);
        }

        //-- Map to song layout
        RelativeLayout songLay = (RelativeLayout) songInf.inflate(R.layout.queuerow, parent, false);
//        songLay.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                Log.v("LongClick", "Longclicked");
//
//                return true;
//            }
//        });

        //-- Get title and artist views
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
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

//        final ImageButton up = (ImageButton) convertView.findViewById(R.id.up);
//        final ImageButton down = (ImageButton) convertView.findViewById(R.id.down);
//        final Button remove = (Button) convertView.findViewById(R.id.remove);

        //set position as tag
        songLay.setTag(position);
        return songLay;
    }
}