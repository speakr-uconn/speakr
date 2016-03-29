package com.speakr.connorriley.speakr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;


public class JamListActivity extends HamburgerActivity implements OnClickListener, WifiP2pManager.ChannelListener, DeviceActionListener{

    DrawerLayout drawerLayout;
    CollapsingToolbarLayout collapsingToolbarLayout;
    Toolbar toolbar;
    TabLayout tabLayout;
    FloatingActionButton fab, refresh_jams;

    DeviceListFragment frag_list;
    DeviceDetailFragment frag_detail;
    boolean isDiscovering = false;

    public static final String TAG = "wifidrect";
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private final IntentFilter intentFilter = new IntentFilter();
    private boolean retryChannel = false;
    private boolean isWifiP2pEnabled = false;
    private boolean onConnection = false;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    public void setConnected(boolean isConnected) {
        this.onConnection = isConnected;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jamlist);

        frag_list = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        frag_detail = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);

        setupNavigationView();
        setupToolbar();
        //setupTablayout();
        //setupCollapsingToolbarLayout();
        setupFab();
        //enable_atn_direct();
        startNetwork();
        //addDrawerItems();
    }

    public void enable_atn_direct(){
        if (manager != null && channel != null) {

            // Since this is the system wireless settings activity, it's
            // not going to send us a result. We will be notified by
            // WiFiDeviceBroadcastReceiver instead.

            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        } else {
            Log.e(TAG, "channel or manager is null");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                if(drawerLayout != null)
                    drawerLayout.openDrawer(GravityCompat.START);
                return true;

            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.

                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupNavigationView(){

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navView = (NavigationView) findViewById(R.id.navList);
        navView.setNavigationItemSelectedListener(this);
    }

    private void setupFab(){
        fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab != null) {
            fab.setOnClickListener(this);
        }
        setupRefresh();
    }

    private void setupRefresh(){
        refresh_jams = (FloatingActionButton) findViewById(R.id.refresh_jams);
        if(refresh_jams != null) {
            refresh_jams.setOnClickListener(this);
        }
    }

    private void setupToolbar(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
        }
        // Show menu icon
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.fab){

            Intent intent = new Intent(JamListActivity.this, CreateJamActivity.class);
            startActivity(intent);
        }
        else if(view.getId() == R.id.refresh_jams){
            if(!isDiscovering)
                discoverPeers();
        }
    }

    public void openPlayerActivity(){
        //-- Mike 10/28/15
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }

    public void openWifiActivity(){
        //-- Mike 1/6/16
        Intent intent = new Intent(this, WiFiDirectActivity.class);
        startActivity(intent);
    }

    public void startNetwork(){
        onConnection = false;
        isDiscovering = true;
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        if (!isWifiP2pEnabled) {
            //Toast.makeText(JamListActivity.this, R.string.p2p_off_warning,
            //        Toast.LENGTH_SHORT).show();

            //startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); //-- enable p2p on/off
            //return;
        }

        frag_list.onInitiateDiscovery();
        discoverPeers();
    }

    public void discoverPeers(){
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                frag_list.progressDialog.show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(JamListActivity.this, "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
                //cancelDisconnect();
            }
        });
        setTimeout();
    }

    public void setTimeout(){
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        if (!onConnection)
                            disconnect();
                        Toast.makeText(JamListActivity.this, "No devices found.",
                                Toast.LENGTH_SHORT).show();
                    }
                },
                10000);
    }

   @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_jams:
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

    @Override
    public void showDetails(WifiP2pDevice device) {
        if(frag_detail != null)
            frag_detail.showDetails(device);
    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(JamListActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        if((frag_list == null) || (frag_detail == null))
            return;
        frag_detail.resetViews();
        isDiscovering = false;

        frag_list.progressDialog.dismiss();
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {

                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
            }

            @Override
            public void onSuccess() {
                frag_detail.getView().setVisibility(View.GONE);
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            if(frag_list == null)
                return;
            if (frag_list.getDevice() == null
                    || frag_list.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (frag_list.getDevice().status == WifiP2pDevice.AVAILABLE
                    || frag_list.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(JamListActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(JamListActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    public void resetData() {
        if (frag_list != null) {
            frag_list.clearPeers();
        }
        if (frag_detail != null) {
            frag_detail.resetViews();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new JamListBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
        discoverPeers();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(receiver);
    }
}
