package com.speakr.connorriley.speakr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, JamListActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        WifiSingleton.getInstance().setMusicResolver(getContentResolver());
        WifiSingleton.getInstance().makeSongList();
        startActivity(intent);
        finish();
    }
}
