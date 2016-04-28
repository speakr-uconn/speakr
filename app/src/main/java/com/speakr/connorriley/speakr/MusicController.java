package com.speakr.connorriley.speakr;

import android.content.Context;
import android.widget.MediaController;

/**
 * Created by Michael on 10/28/2015.
 */
public class MusicController extends MediaController {

    public MusicController(Context c){
        super(c);
    }

    public MusicController(Context c, boolean isFfwd){
        super(c, isFfwd);
    }

    public void hide(){}

    public void doSomething(){

    }

}