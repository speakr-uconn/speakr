package com.speakr.connorriley.speakr;

/**
 * Created by Michael on 10/21/2015.
 */
public class Song {
    //-- Use this class to instantiate a new song
    private long id;
    private String title;
    private String artist;
    private int ownerID;
    /*
        We will need something to point to a file to play, or something
     */

    public Song(long songID, String songTitle, String songArtist, int owner) {
        id=songID;
        title=songTitle;
        artist=songArtist;
        ownerID = owner;
    }

    public long getID(){
        return id;
    }
    public String getTitle(){
        return title;
    }
    public String getArtist(){
        return artist;
    }
}
