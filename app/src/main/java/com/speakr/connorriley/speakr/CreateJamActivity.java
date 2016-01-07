package com.speakr.connorriley.speakr;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by connorriley on 1/7/16.
 */
public class CreateJamActivity extends HamburgerActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createjam);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Drew - 1/7/16
        final EditText nicknameEditText = (EditText) findViewById(R.id.nickname);
        final EditText jamnameEditText = (EditText) findViewById(R.id.jam_name);

        Button doneButton = (Button) findViewById(R.id.done);
        doneButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                nicknameEditText.setVisibility(View.INVISIBLE);
                jamnameEditText.setVisibility(View.INVISIBLE);
                if (nicknameEditText.getText().toString().length() == 0) {
                    Context context = getApplicationContext();
                    CharSequence text = "Please enter a nickname!";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }

                else if(jamnameEditText.getText().toString().length() == 0) {
                    Context context = getApplicationContext();
                    CharSequence text = "Please enter a jam name!";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }

                else {

                    /*Context context = getApplicationContext();
                    CharSequence text = "Both fields non-null";
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();*/

                    openPlayerActivity();
                }

                nicknameEditText.setVisibility(View.VISIBLE);
                jamnameEditText.setVisibility(View.VISIBLE);
            }
        });

        /*
        nicknameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            boolean handled = false;
            if (i == EditorInfo.IME_ACTION_NEXT) {
                //show toast for input
                String inputText = textView.getText().toString();
                Toast.makeText(CreateJamActivity.this, "Your nickname is: " + inputText, Toast.LENGTH_SHORT).show();
                handled = true;
            }
                return handled;
            }
        });*/
    }
    public void openPlayerActivity(){
        //-- Mike 10/28/15
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }
    public void openHomeAcitivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
    public void openWifiActivity(){
        //-- Mike 1/6/16
        Intent intent = new Intent(this, WiFiDirectActivity.class);
        startActivity(intent);
    }
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_jams:
                openHomeAcitivity();
                break;
            case R.id.nav_music_player:
                openPlayerActivity();
                break;
            case R.id.nav_wifi:
                openWifiActivity();
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}