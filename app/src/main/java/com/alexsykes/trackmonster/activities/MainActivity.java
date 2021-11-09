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
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.google.android.gms.tasks.OnCompleteListener;
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

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleApiClient.ConnectionCallbacks {
    int updateInterval;
    int trackid;

    // Flags
    private boolean isRecording;
    private boolean locationPermissionGranted;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "requestingLocationUpdates";

    private GoogleMap map;

    // Google API
    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleApiClient mGoogleApiClient;

    // CONSTANTS
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String KEY_IS_RECORDING = "isRecording";
    private static final String KEY_TRACKID = "trackid";
    private boolean requestingLocationUpdates;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private FloatingActionButton fabRecord, fabCutAndNew;
    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FASTEST_UPDATE_INTERVAL = 30;
    private static final int PERMISSION_FINE_LOCATION = 99;
    private static final String TAG = "Info";

    private Location lastKnownLocation;
    private CameraPosition cameraPosition;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private TrackDbHelper trackDbHelper;
    private WaypointDbHelper waypointDbHelper;
    private ArrayList<LatLng> currentTrack;

    // Lifecycle starts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateValuesFromBundle(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPrefs();
        trackDbHelper = new TrackDbHelper(this);
        // UI components
        View mLayout = findViewById(R.id.map);
        fabRecord = findViewById(R.id.fabRecord);
        fabCutAndNew = findViewById(R.id.fabCutAndNew);

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * updateInterval);
        locationRequest.setFastestInterval(1000 * FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                if (isRecording) {
                    Log.i(TAG, "Recording new location");
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        processNewLocation(location);
                    }
                }
            }
        };

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set up FAB menu
        fabSetup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "MainActivity: onResume: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "MainActivity: onStart: ");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isRecording = prefs.getBoolean("isRecording", false);
        if (isRecording == true) {
        } else {
            isRecording = false;
        }
        displayAllVisibleTracks();
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        Log.i(TAG, "MainActivity: onRestoreInstanceState: ");
        lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        isRecording = (boolean) savedInstanceState.getSerializable(KEY_IS_RECORDING);
        trackid = savedInstanceState.getInt("trackid");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "MainActivity: onSaveInstanceState: ");
        outState.putSerializable(KEY_IS_RECORDING, isRecording);
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                requestingLocationUpdates);
        outState.putInt(KEY_TRACKID, trackid);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isRecording", isRecording);
        editor.apply();

        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
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
        // Reference version has super call missing with locationPermissionGranted mutations as below
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSION_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //  locationPermissionGranted = true;
                } else {
                    Toast.makeText(this, "Permissions needed", Toast.LENGTH_SHORT).show();
                    finish();
                }
            default:
        }
        updateLocationUI();
    }

    // Setup
    private void getPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("canConnect", canConnect());
        editor.apply();

        isRecording = prefs.getBoolean("isRecording", false);
        trackid = prefs.getInt("trackid", 1);
        updateInterval = prefs.getInt("interval", DEFAULT_UPDATE_INTERVAL);
    }

    private void fabSetup() {
        fabRecord.setBackgroundTintList(AppCompatResources.getColorStateList(this, fab_colour_state_list));
        fabCutAndNew.setBackgroundTintList(AppCompatResources.getColorStateList(this, small_fab_colour_state_list));
        fabRecord.setColorFilter(Color.WHITE);
        fabCutAndNew.setColorFilter(Color.WHITE);
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


        // Update prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isRecording", isRecording);
        editor.apply();

        // OnClickListener - toggle recording mode
        fabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Flip to flag, hide the buttons
                isRecording = !isRecording;
                fabCutAndNew.hide();
                Log.i(TAG, "fabRecord: onClick: isRecording " + isRecording);

                if (isRecording) {
                    fabRecord.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.ic_baseline_stop_24));
                } else {
                    fabRecord.setImageDrawable(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.ic_play_arrow_48));
                }
            }
        });

        // OnLongClickListener - show option to cut and create new track
        fabRecord.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isRecording) {
                    fabRecord.show();
                    fabCutAndNew.show();
                    fabCutAndNew.animate().translationX(-(fabRecord.getCustomSize()));
                }
                return false;
            }
        });
        fabCutAndNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fabCutAndNew.animate().translationX(0);
                fabCutAndNew.hide();
                if (isRecording) {
                    cutAndNew();
                }
            }
        });
    }
    protected boolean canConnect() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    // Activity Navigation starts
    private void goTrackList() {
        Intent intent = new Intent(this, TrackListActivity.class);
        // intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
    private void goSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        // intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
    // Activity Navigation ends

    // Events starts
    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        UiSettings uiSettings = map.getUiSettings();
        // uiSettings.setZoomControlsEnabled(true);
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
        currentTrack = waypointDbHelper.getTrackPoints(trackid);
        if (currentTrack.size() > 0) {
            LatLngBounds latLngBounds = showCurrentTrack(currentTrack);
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 1000, 1000, 3));
        }
        // trackDbHelper = new TrackDbHelper(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected: restored");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnected: suspended" + i);
    }

    // Overloaded methods returning LatLngBounds for TrackData
    private LatLngBounds calcBounds(TrackData[] trackDataArray) {
        int numTracks;
        LatLng northeast, southwest;

        numTracks = trackDataArray.length;
        LatLngBounds latLngBounds;
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

        LatLngBounds latLngBounds = calcBounds(currentTrack);
        return latLngBounds;
    }

    private void processNewLocation(Location location) {
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

    private void cutAndNew() {
        Log.i(TAG, "cutAndNew: called");
        TrackDbHelper trackDbHelper = new TrackDbHelper(this);
        trackid = trackDbHelper.insertNewTrack();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("trackid", trackid);
        editor.apply();
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

    private void displayActiveTrack() {
        TrackData trackData = trackDbHelper.getTrackData(trackid);
    }

    private void displayAllVisibleTracks() {
        int numTracks;
        LatLngBounds latLngBounds;
        TrackData[] trackDataArray = trackDbHelper.getAllTrackData();
        numTracks = trackDataArray.length;

        latLngBounds = calcBounds(trackDataArray);
    }

    private void getDeviceLastLocation() {
        /*      Can this be replaced by map showing all visible tracks?
         * *     Get the best and most recent location of the device, which may be null in rare
         *      cases when a location is not available.*/

        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
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
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void startLocationUpdates() {
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
        Log.i(TAG, "startLocationUpdates: ");
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
                //.addOnConnectionFailedListener(this)
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