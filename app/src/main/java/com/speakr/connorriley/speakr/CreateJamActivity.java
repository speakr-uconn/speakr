package com.speakr.connorriley.speakr;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;

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
