package com.alexsykes.trackmonster.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.data.TrackData;
import com.alexsykes.trackmonster.data.TrackDbHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class TrackDialogActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "Info";
    TextInputLayout nameTextInputLayout;
    TextInputLayout descriptionTextInputLayout;
    CheckBox isVisibleCheckBox;
    CheckBox isCurrentCheckBox;
    String name;
    String task;
    String trackDescription;
    boolean isVisible, isCurrent;
    int trackID;
    int trackIdFromPrefs;
    TrackDbHelper trackDbHelper;
    Intent intent;
    SharedPreferences prefs;
    TrackData trackData;

    // Map components
    private View mLayout;
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_dialog);

        // Setup UI
        nameTextInputLayout = findViewById(R.id.track_name_text_input);
        descriptionTextInputLayout = findViewById(R.id.track_detail_text_input);
        isVisibleCheckBox = findViewById(R.id.track_visibility_checkBox);
        isCurrentCheckBox = findViewById(R.id.track_active_checkBox);
        mLayout = findViewById(R.id.trackMap);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.trackMap);
        mapFragment.getMapAsync((OnMapReadyCallback) this);

        // Get data
        trackDbHelper = new TrackDbHelper(this);
        intent = getIntent();
        trackID = Integer.parseInt(intent.getExtras().getString("trackid", "1"));
        task = intent.getExtras().getString("task");

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        trackIdFromPrefs = prefs.getInt("trackid", 1);

        // Get existing track details and show values
        if (task.equals("update")) {
            trackData = trackDbHelper.getTrackData(trackID);

            nameTextInputLayout.getEditText().setText(trackData.getName());
            descriptionTextInputLayout.getEditText().setText(trackData.getDescription());
            isVisible = trackData.isVisible();
            isVisibleCheckBox.setChecked(isVisible);
            isCurrentCheckBox.setChecked(trackIdFromPrefs == trackID);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveTrackDetails();
        Log.i(TAG, "onDestroy");
    }

    private void saveTrackDetails() {
        SharedPreferences.Editor editor = prefs.edit();
        name = nameTextInputLayout.getEditText().getText().toString();
        trackDescription = descriptionTextInputLayout.getEditText().getText().toString();
        isVisible = isVisibleCheckBox.isChecked();
        isCurrent = isCurrentCheckBox.isChecked();

        if (task.equals("new")) {
            trackID = trackDbHelper.insertNewTrack(isCurrent, name, trackDescription, isVisible);
        } else if (task.equals("update")) {
            trackDbHelper.updateTrack(trackID, name, trackDescription, isVisible);
        }

        if (isCurrent) {
            editor.putInt("trackid", trackID);
            editor.apply();
        }
        finish();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setAllGesturesEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        if (task.equals("update")) {
            // Display track on map
            displayTrack();
        }
        //
    }

    private void displayTrack() {
        LatLngBounds latLngBounds = trackData.getLatLngBounds();
        ArrayList<LatLng> latLngs = trackData.getLatLngs();
        PolylineOptions polylineOptions = new PolylineOptions();
        // Create polyline options with existing LatLng ArrayList
        polylineOptions.addAll(latLngs);
        polylineOptions
                .width(5)
                .color(Color.BLUE);

        map.addPolyline(polylineOptions);
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 1000, 1000, 3));


    }
}