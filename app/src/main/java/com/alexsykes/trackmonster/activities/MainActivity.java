package com.alexsykes.trackmonster.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alexsykes.trackmonster.MapStateManager;
import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.data.TrackData;
import com.alexsykes.trackmonster.data.TrackDbHelper;
import com.alexsykes.trackmonster.data.WaypointDbHelper;
import com.alexsykes.trackmonster.handlers.TrackListAdapter;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;

// See https://developers.google.com/maps/documentation/android-sdk/map-with-marker
// https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial
// https://developers.google.com/maps/documentation/android-sdk/map
// https://developer.android.com/training/location/retrieve-current
// https://www.journaldev.com/13325/android-location-api-tracking-gps
// https://www.youtube.com/watch?v=2ibBng2eJJA
// https://www.youtube.com/watch?v=_xUcYfbtfsI
// https://material.io/components/buttons-floating-action-button
// https://stackoverflow.com/questions/44862176/request-ignore-battery-optimizations-how-to-do-it-right
// https://stackoverflow.com/questions/11040851/android-intent-to-start-main-activity-of-application
// https://stackoverflow.com/questions/34636722/android-saving-map-state-in-google-map

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    int updateInterval;
    int trackid;
    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    RecyclerView trackListRecyclerView;

    // Flags
    private boolean isRecording;
    private boolean locationPermissionGranted;
    String statusText;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requestingLocationUpdates";


    // Google API
    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleApiClient mGoogleApiClient;

    // CONSTANTS
    private static final int DEFAULT_ZOOM = 14;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String KEY_IS_RECORDING = "isRecording";
    private static final String KEY_TRACKID = "trackid";
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    public static final int FASTEST_UPDATE_INTERVAL = 5;
    private static final int ACTIVITY_REQUEST_CODE = 0;
    private static final int PERMISSION_FINE_LOCATION = 99;
    private boolean requestingLocationUpdates;
    private static final String TAG = "Info";
    TrackData currentTrack;

    private Location lastKnownLocation;
    private CameraPosition cameraPosition;
    private MapStateManager mapStateManager;
    private TrackDbHelper trackDbHelper;
    private WaypointDbHelper waypointDbHelper;
    // UI
    private TextView statusTextView;
    private GoogleMap map;

    private LatLng cameraPositionLatLng;
    private ArrayList<HashMap<String, String>> theTrackList;

    // Lifecycle starts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupMapIfNeeded();
        Log.i(TAG, "onCreate: ");

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        getData();
        setupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupMapIfNeeded();
        Log.i(TAG, "MainActivity: onResume: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        Log.i(TAG, "MainActivity: onStart: ");
        getPrefs();
        // Check for zero = no current track
        trackid = trackDbHelper.getCurrentTrackID();
        if (trackid == 0) {
            statusText = statusText + "No track selected";
        }        // Set up FAB menu
        statusTextView.setText(statusText);
        populateTrackList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: isRecording: " + isRecording);

        trackDbHelper.closeDB();
        mapStateManager = new MapStateManager(this);
        mapStateManager.saveMapState(map);
        // Save current state 
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.apply();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
        outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        super.onSaveInstanceState(outState);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.main_menu, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                goSettings();
                return true;
            case R.id.tracks:
                goTrackList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // Lifecycle ends

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_FINE_LOCATION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions needed", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Setup
    private void getPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        isRecording = prefs.getBoolean("isRecording", false);
        editor.putBoolean("canConnect", canConnect());
        editor.apply();
        updateInterval = prefs.getInt("interval", DEFAULT_UPDATE_INTERVAL);
    }

    private void addWaypoint() {
    }

    protected boolean canConnect() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    // Activity Navigation starts
    private void goTrackList() {
        Intent intent = new Intent(this, TrackListActivity.class);
        startActivity(intent);
    }
    private void goSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    // Activity Navigation ends

    // Events starts
    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        mapStateManager = new MapStateManager(this);
        CameraPosition position = mapStateManager.getSavedCameraPosition();
        if (position != null) {
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
            map.moveCamera(update);
            map.setMapType(mapStateManager.getSavedMapType());
        }

        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                    displayAllVisibleTracks(map);
            }
        });
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setAllGesturesEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
    }



    // Overloaded methods returning LatLngBounds for TrackData
    private LatLngBounds calcBounds(TrackData[] trackDataArray) {
        int numTracks;
        LatLng northeast, southwest;

        numTracks = trackDataArray.length;
        TrackData currentTrackData;
        double westmost = trackDataArray[0].getWest();
        double eastmost = trackDataArray[0].getEast();
        double southmost = trackDataArray[0].getSouth();
        double northmost = trackDataArray[0].getNorth();

        for (int i = 0; i < numTracks; i++) {
            currentTrackData = trackDataArray[i];
            if (currentTrackData.getNorth() > northmost) {
                northmost = currentTrackData.getNorth();
            }
            if (currentTrackData.getSouth() < southmost) {
                southmost = currentTrackData.getSouth();
            }
            if (currentTrackData.getWest() < westmost) {
                westmost = currentTrackData.getWest();
            }
            if (currentTrackData.getEast() > eastmost) {
                eastmost = currentTrackData.getEast();
            }
        }

        northeast = new LatLng(northmost, eastmost);
        southwest = new LatLng(southmost, westmost);
        return new LatLngBounds(southwest, northeast);
    }
    private LatLngBounds calcBounds(ArrayList<LatLng> track) {
        double north = -88;
        double south = 88;
        double east = -180;
        double west = 180;

        // LatLng latLng = new LatLng(0,0);

        for (LatLng latLng : track) {
            if (latLng.latitude > north) {
                north = latLng.latitude;
            }
            if (latLng.longitude < west) {
                west = latLng.longitude;
            }
            if (latLng.latitude < south) {
                south = latLng.latitude;
            }
            if (latLng.longitude > east) {
                east = latLng.longitude;
            }
        }

        LatLng topRight = new LatLng(north, east);
        LatLng bottomLeft = new LatLng(south, west);
        return new LatLngBounds(bottomLeft, topRight);
    }

    private void showTrack(TrackData currentTrack) {
        final int COLOR_DARK_GREEN_ARGB = 0xff388E3C;
        final int COLOR_LIGHT_GREEN_ARGB = 0xff81C784;
        final int COLOR_DARK_ORANGE_ARGB = 0xffF57F17;
        final int COLOR_LIGHT_ORANGE_ARGB = 0xffF9A825;

        int strokeWidth = 5;
        int strokeColour = COLOR_DARK_GREEN_ARGB;
        ArrayList<LatLng> latLngs = currentTrack.getLatLngs();
        String style = currentTrack.getStyle();
        PolylineOptions polylineOptions = new PolylineOptions();
        // Create polyline options with existing LatLng ArrayList
        polylineOptions.addAll(latLngs);

        switch (style) {
            case "Undefined":
                strokeColour = COLOR_LIGHT_GREEN_ARGB;
                break;
            case "Track":
                strokeColour = COLOR_DARK_GREEN_ARGB;
                strokeWidth = 10;
                break;
            case "Road":
                strokeColour = COLOR_LIGHT_ORANGE_ARGB;
                break;
            case "Major road":
                strokeColour = COLOR_DARK_ORANGE_ARGB;
                strokeWidth = 10;
                break;
        }
        polylineOptions
                .color(strokeColour)
                .width(strokeWidth);

        map.addPolyline(polylineOptions);
    }

    // Inserts new track the makes it current
    private void cutAndNew() {
        Log.i(TAG, "cutAndNew: called");
        TrackDbHelper trackDbHelper = new TrackDbHelper(this);
        trackid = trackDbHelper.insertNewTrack();
    }

    private TrackData getCurrentTrackData() {
        return trackDbHelper.getCurrentTrackData();
    }

    private void displayAllVisibleTracks(GoogleMap map) {
        LatLngBounds latLngBounds;
        TrackData[] trackDataArray = trackDbHelper.getAllVisibleTrackData();

        for (int i = 0; i < trackDataArray.length; i++) {
            showTrack(trackDataArray[i]);
        }
        latLngBounds = calcBounds(trackDataArray);
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(latLngBounds, 30);
        map.animateCamera(cu);
    }


    // Utility
    protected synchronized void buildGoogleApiClient() {
        // mGoogleApiClient = new GoogleApiClient.Builder(this).build();
        // mGoogleApiClient.connect();
    }

    private void setupMapIfNeeded() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        if (map == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

    }

    private void getData() {
        // Data
        trackDbHelper = new TrackDbHelper(this);
        currentTrack = getCurrentTrackData();
        trackid = currentTrack.get_id();
    }

    private void setupUI() {
        // UI components
        statusTextView = findViewById(R.id.statusTextView);
    }


    private void populateTrackList() {
        theTrackList = trackDbHelper.getShortTrackList();
        trackListRecyclerView = findViewById(R.id.trackListRecyclerView);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        trackListRecyclerView.setLayoutManager(llm);
        trackListRecyclerView.setHasFixedSize(true);
        initializeAdapter();
    }

    private void initializeAdapter() {
        TrackListAdapter adapter = new TrackListAdapter(theTrackList);
        trackListRecyclerView.setAdapter(adapter);
    }


    public void onClickCalled(String trackid) {
        // Call another activity here and pass some arguments to it.
//        Intent intent = new Intent(this, TrackDialogActivity.class);
//        intent.putExtra("trackid", trackid);
//        intent.putExtra("task", "update");
//        startActivity(intent);
        Log.i(TAG, "onClickCalled: ");
    }
}