package com.speakr.connorriley.speakr;

/**
 * Created by Michael on 10/21/2015.
 */
public class Song {
    //-- Use this class to instantiate a new song
    private long id = -1;
    private String title;
    private String artist;
    private int ownerID;
    private String path;
    private boolean hasPath;
    /*
        We will need something to point to a file to play, or something
     */

    public Song(long songID, String songTitle, String songArtist, int owner) {
        id=songID;
        title=songTitle;
        artist=songArtist;
        ownerID = owner;
        hasPath = false;
    }
    public Song(String songPath, String songTitle, String songArtist, int owner) {
        path = songPath;
        title=songTitle;
        artist=songArtist;
        ownerID = owner;
        hasPath = true;
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
    public String getPath() { return path; }
}
