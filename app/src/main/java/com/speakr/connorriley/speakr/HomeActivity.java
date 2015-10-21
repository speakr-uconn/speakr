package com.speakr.connorriley.speakr;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class HomeActivity extends Activity {

    private ListView jamsListView;
    private ArrayAdapter arrayAdapter;
    private String[] jamsArray = {"test jam 1", "test jam 2", "bangers"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        jamsListView = (ListView) findViewById(R.id.jams_list);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, jamsArray);
        jamsListView.setAdapter(arrayAdapter);
    }

}
