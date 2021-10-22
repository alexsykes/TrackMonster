package com.alexsykes.trackmonster.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;

import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.TrackDbHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class TrackListActivity extends AppCompatActivity {
    TrackDbHelper trackDbHelper;
    ArrayList<HashMap<String, String>> theTrackList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_list);

        trackDbHelper = new TrackDbHelper(this);
        populateTrackList();

    }

    private void populateTrackList() {
        theTrackList = trackDbHelper.getTrackList();
//        timeView = findViewById(R.id.timeView);
//
//        LinearLayoutManager llm = new LinearLayoutManager(this);
//        timeView.setLayoutManager(llm);
//        timeView.setHasFixedSize(true);
//        initializeAdapter();
    }
}