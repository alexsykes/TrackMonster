package com.alexsykes.trackmonster.activities;

import static com.alexsykes.trackmonster.R.color.fab_colour_state_list;
import static com.alexsykes.trackmonster.R.color.small_fab_colour_state_list;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.data.TrackData;
import com.alexsykes.trackmonster.data.TrackDbHelper;
import com.alexsykes.trackmonster.data.WaypointDbHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

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

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    int updateInterval;
    int trackid;
    public static final int DEFAULT_UPDATE_INTERVAL = 30;

    // Flags
    private boolean isRecording;
    private boolean locationPermissionGranted;
    String statusText;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requestingLocationUpdates";


    // Google API
    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleApiClient mGoogleApiClient;

    // CONSTANTS
    private static final int DEFAULT_ZOOM = 15;
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
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private TrackDbHelper trackDbHelper;
    private WaypointDbHelper waypointDbHelper;
    // UI
    private FloatingActionButton fabRecord, fabCutAndNew, fabAddWaypoint;
    private TextView statusTextView;
    private GoogleMap map;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    // Lifecycle starts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: ");
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "TrackMonster::MyWakelockTag");
        setContentView(R.layout.activity_main);
        updateValuesFromBundle(savedInstanceState);
        trackDbHelper = new TrackDbHelper(this);
        currentTrack = getCurrentTrackData();
        trackid = currentTrack.get_id();
        // UI components
        // View mLayout = findViewById(R.id.map);
        fabRecord = findViewById(R.id.fabRecord);
        statusTextView = findViewById(R.id.statusTextView);
        fabCutAndNew = findViewById(R.id.fabCutAndNew);
        fabAddWaypoint = findViewById(R.id.fabAddWaypoint);

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000L * updateInterval);
        locationRequest.setFastestInterval(1000L * FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (isRecording) {
                        processNewLocation(location);
                    }
                }
            }
        };

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        //if (requestingLocationUpdates) {
            startLocationUpdates();
        // }
        Log.i(TAG, "MainActivity: onResume: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "MainActivity: onStart: ");
        getPrefs();
        if (isRecording) {
            statusText = "Recording";
            wakeLock.acquire();
        } else {
            isRecording = false;
            statusText = "Paused";
        }
        // Check for zero = no current track
        trackid = trackDbHelper.getCurrentTrackID();
        if (trackid == 0) {
            statusText = statusText + "No track selected";
        }        // Set up FAB menu
        fabSetup();
        statusTextView.setText(statusText);
        // displayAllVisibleTracks();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: isRecording: " + isRecording);
        // Save current state 
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("isRecording", isRecording);
        editor.apply();
        
        stopLocationUpdates();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

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
        updateLocationUI();
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

    private void fabSetup() {
        fabRecord.setBackgroundTintList(AppCompatResources.getColorStateList(this, fab_colour_state_list));
        fabCutAndNew.setBackgroundTintList(AppCompatResources.getColorStateList(this, small_fab_colour_state_list));
        fabAddWaypoint.setBackgroundTintList(AppCompatResources.getColorStateList(this, small_fab_colour_state_list));
        fabRecord.setColorFilter(Color.WHITE);
        fabCutAndNew.setColorFilter(Color.WHITE);
        fabAddWaypoint.setColorFilter(Color.WHITE);
        // FAB actions
        if (isRecording) {
            fabRecord.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_pause_24));
            startLocationUpdates();
            isRecording = true;
        } else {
            fabRecord.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_play_arrow_48));
            stopLocationUpdates();
            isRecording = false;
        }


        // OnClickListener - toggle recording mode
        fabRecord.setOnClickListener(v -> {
            // Flip to flag, hide the buttons
            isRecording = !isRecording;
            fabCutAndNew.hide();
            fabAddWaypoint.hide();
            Log.i(TAG, "fabRecord: onClick: isRecording " + isRecording);

            if (isRecording) {
                wakeLock.acquire();

                fabRecord.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.ic_baseline_stop_24));
                startLocationUpdates();
            } else {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }

                fabRecord.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.ic_play_arrow_48));
                stopLocationUpdates();
            }
            // Update prefs
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isRecording", isRecording);
            editor.apply();
        });

        // OnLongClickListener - show option to cut and create new track
        fabRecord.setOnLongClickListener(v -> {
            if (isRecording) {
                fabRecord.show();
                fabCutAndNew.show();
                fabAddWaypoint.show();
                fabCutAndNew.animate().translationX(-(fabRecord.getCustomSize()));
                fabAddWaypoint.animate().translationX(-((fabRecord.getCustomSize() + fabRecord.getCustomSize())));
            }
            return false;
        });

        fabCutAndNew.setOnClickListener(v -> {
            fabCutAndNew.animate().translationX(0);
            fabCutAndNew.hide();
            fabAddWaypoint.animate().translationX(0);
            fabAddWaypoint.hide();
            if (isRecording) {
                cutAndNew();
            }
        });

        fabAddWaypoint.setOnClickListener(v -> {
            fabCutAndNew.animate().translationX(0);
            fabCutAndNew.hide();
            fabAddWaypoint.animate().translationX(0);
            fabAddWaypoint.hide();
            if (isRecording) {
                addWaypoint();
            }
        });
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
        UiSettings uiSettings = map.getUiSettings();
        // uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setAllGesturesEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        getLocationPermission();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                map.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            map.setMyLocationEnabled(true);
        }

        // map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
        if (cameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        getDeviceLastLocation();

        // Replace this
        waypointDbHelper = new WaypointDbHelper(this);
        ArrayList<LatLng> currentTrack = waypointDbHelper.getTrackPoints(trackid);
        if (currentTrack.size() > 0) {
            //   LatLngBounds latLngBounds = showCurrentTrack(currentTrack);
            //   map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 1000, 1000, 3));
        }
        // trackDbHelper = new TrackDbHelper(this);
        // displayAllVisibleTracks(map);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected: ");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnected: suspended" + i);
        Context context = getApplicationContext();
        CharSequence text = "Connection lost";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed: ");
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

    // Location section
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private LatLngBounds showCurrentTrack(ArrayList<LatLng> currentTrack) {
        PolylineOptions polylineOptions = new PolylineOptions();
        // Create polyline options with existing LatLng ArrayList
        polylineOptions.addAll(currentTrack);
        polylineOptions
                .width(5)
                .color(Color.BLUE);

        map.addPolyline(polylineOptions);

        return calcBounds(currentTrack);
    }

    private void showTrack(ArrayList<LatLng> currentTrack) {
        PolylineOptions polylineOptions = new PolylineOptions();
        // Create polyline options with existing LatLng ArrayList
        polylineOptions.addAll(currentTrack);
        polylineOptions
                .width(5)
                .color(Color.BLUE);

        map.addPolyline(polylineOptions);
    }

    private void processNewLocation(Location location) {
        trackid = trackDbHelper.getCurrentTrackID();
        waypointDbHelper = new WaypointDbHelper(this);
        String logentry;
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        double speed = location.getSpeed();
        double bearing = location.getBearing();
        double alt = location.getAltitude();

        waypointDbHelper.addLocation(trackid, lat, lng, speed, bearing, alt);

        LatLng latLng = new LatLng(lat, lng);
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        logentry = "Status:\nLat: " + lat + "\nLng: " + lng + "\nSpeed: " + speed + "\nBearing: " + bearing;
        Log.i("Status", logentry);
    }

    // Inserts new track the makes it current
    private void cutAndNew() {
        Log.i(TAG, "cutAndNew: called");
        TrackDbHelper trackDbHelper = new TrackDbHelper(this);
        trackid = trackDbHelper.insertNewTrack();
    }

    // Location UI interaction
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private TrackData getCurrentTrackData() {
        return trackDbHelper.getCurrentTrackData();
    }

    private void displayAllVisibleTracks(GoogleMap map) {
        LatLngBounds latLngBounds;
        TrackData[] trackDataArray = trackDbHelper.getAllVisibleTrackData();

        for (int i = 0; i < trackDataArray.length; i++) {
            showTrack(trackDataArray[i].getLatLngs());
        }
        latLngBounds = calcBounds(trackDataArray);
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 1000, 1000, 3));
    }

    private void getDeviceLastLocation() {
        /*      Can this be replaced by map showing all visible tracks?
         * *     Get the best and most recent location of the device, which may be null in rare
         *      cases when a location is not available.*/

        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.getResult();
                        Log.i(TAG, "getDeviceLastLocation: onComplete: lastKnownLocation");
                        if (lastKnownLocation != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.");
                        Log.e(TAG, "Exception: %s", task.getException());
                        // map.moveCamera(CameraUpdateFactory
                        //         .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                        map.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates: ");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        requestingLocationUpdates = true;
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.i(TAG, "stopLocationUpdates: ");
        requestingLocationUpdates = false;
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    // Utility
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        // Update the value of requestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }
    }
}