package com.speakr.connorriley.speakr;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;



/**
 * Created by Drew on 11/29/2015.

    based off this guide - http://mrbool.com/how-to-extract-meta-data-from-media-file-in-android/28130
 */


public class AlbumArtActivity extends Activity
{
    ImageView album_art;
    TextView album, artist, genre;
    MediaMetadataRetriever metaRetriver;
    byte[] art;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        getInit();
        // Ablum_art retrieval code //
        metaRetriver = new MediaMetadataRetriever();
        //different source for this?
        metaRetriver.setDataSource("/sdcard/audio.mp3");

        try {
            art = metaRetriver.getEmbeddedPicture();
            Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            album_art.setImageBitmap(songImage);
            album.setText(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
            artist.setText(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            genre.setText(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
        } catch (Exception e) {
            album_art.setBackgroundColor(Color.GRAY);
            album.setText("Unknown Album");
            artist.setText("Unknown Artist");
            genre.setText("Unknown Genre");
        }

    }

        // Fetch Id's form xml //

        public void getInit() {
            album_art = (ImageView) findViewById(R.id.album_art);
            album = (TextView) findViewById(R.id.Album);
            artist = (TextView) findViewById(R.id.artist_name);
        }
}
