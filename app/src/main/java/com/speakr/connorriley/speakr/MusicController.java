package com.speakr.connorriley.speakr;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.MediaController;
import android.widget.TextView;

/**
 * Created by Michael on 10/28/2015.
 */
public class MusicController extends MediaController {
    private String curTime = "0:00";
    private String endTime = "0:00";


    public MusicController(Context c){
        super(c);
    }

    public MusicController(Context c, boolean isFfwd){
        super(c, isFfwd);
    }

    public void hide(){}

    public void holdup(){
        final TextView mEndTime = (TextView) this.findViewById(Resources.getSystem().getIdentifier("time", "id", "android"));
        final TextView mCurrentTime = (TextView) this.findViewById(Resources.getSystem().getIdentifier("time_current", "id", "android"));
        if(mEndTime != null)
            endTime = mEndTime.getText().toString();
        if(mCurrentTime != null)
            curTime = mCurrentTime.getText().toString();

        //-- THIS WOULD BE MUCH BETTER WITH A CALLBACK FUNCTION FOR WHEN THE PLAYER IS PAUSED
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        setPausedProgress();
                    }
                },
                500);

    }

    public void setPauseTime(String time){
        TextView mCurrentTime = (TextView) this.findViewById(Resources.getSystem().getIdentifier("time_current", "id", "android"));
        if(mCurrentTime != null)
            curTime = time;
    }

    public void setPausedProgress(){
        TextView mEndTime = (TextView) this.findViewById(Resources.getSystem().getIdentifier("time", "id", "android"));
        TextView mCurrentTime = (TextView) this.findViewById(Resources.getSystem().getIdentifier("time_current", "id", "android"));

        Log.d("MusicController", "PLS " + mEndTime.getText().toString());
        //-- When paused, update the times
         if (mEndTime != null)
            mEndTime.setText(endTime);
        if (mCurrentTime != null)
            mCurrentTime.setText(curTime);
    }

}