package com.alexsykes.trackmonster.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

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
import android.widget.TextView;
import android.widget.Toast;

import com.alexsykes.trackmonster.R;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

// See https://developers.google.com/maps/documentation/android-sdk/map-with-marker
// https://developers.google.com/maps/documentation/android-sdk/map
// https://developer.android.com/training/location/retrieve-current
// https://www.journaldev.com/13325/android-location-api-tracking-gps
// https://www.youtube.com/watch?v=2ibBng2eJJA
// https://www.youtube.com/watch?v=_xUcYfbtfsI

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleApiClient.ConnectionCallbacks {
    private final LatLng defaultLocation = new LatLng(52.023728, -1.147916);
    int updateInterval;
    int trackid;

    // Flags
    private boolean useGPSonly;
    private boolean trackingOn;
    private boolean requestingLocationUpdates;
    private boolean locationPermissionGranted;

    // UI components
    private View mLayout;
    private GoogleMap map;
    private TextView statusLine;

    // Google API
    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleApiClient mGoogleApiClient;

    // CONSTANTS
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    public static final String EXTRA_MESSAGE = "com.alexsykes.trackmonster.activities.MESSAGE";
    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FASTEST_UPDATE_INTERVAL = 30;
    private static final int PERMISSION_FINE_LOCATION = 99;
    private static final String TAG = "Info";

    private Location lastKnownLocation;
    private CameraPosition cameraPosition;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private LocationRequest locationRequest;
    private TrackDbHelper trackDbHelper;
    private WaypointDbHelper waypointDbHelper;

    // Lifecycle starts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPrefs();
        statusLine = findViewById(R.id.statusLine);
        mLayout = findViewById(R.id.map);
        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * updateInterval);
        locationRequest.setFastestInterval(1000 * FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(locationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.i(TAG, "onLocationResult: called");
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                     processNewLocation(location);
                }
            }
        };

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (!useGPSonly) {
            //    fusedLocationProviderClient = new FusedLocationProviderClient(this);
            //    setUpLocation();
        }
    }

    private void processNewLocation(Location location) {
        waypointDbHelper = new WaypointDbHelper(this);
        String logentry;
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        double speed = location.getSpeed();
        double bearing = location.getBearing();
        double alt = location.getAltitude();

        waypointDbHelper.addLocation(lat, lng, speed, bearing, alt);

        LatLng latLng = new LatLng(lat, lng);
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        logentry = "Status:\nLat: " + lat + "\nLng: " + lng + "\nSpeed: " + speed + "\nBearing: " + bearing;
        Log.i("Status", logentry);

        // TODO updateCamera
        // updateMap(location);
        statusLine.setText(logentry);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "MainActivity: onResume: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // startLocationUpdates();
        Log.i(TAG, "MainActivity: onStart: ");
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        Log.i(TAG, "MainActivity: onRestoreInstanceState: ");
        lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "MainActivity: onSaveInstanceState: ");
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

    // Setup
    private void getPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (canConnect() == false) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("useGPSonly", true);
            editor.apply();
        }

        useGPSonly = prefs.getBoolean("useGPSonly", false);
        trackingOn = prefs.getBoolean("trackingOn", false);
        trackid = prefs.getInt("trackid", 1);
        updateInterval = prefs.getInt("interval", DEFAULT_UPDATE_INTERVAL);
        requestingLocationUpdates = !useGPSonly;
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
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setAllGesturesEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        getLocationPermission();
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
                if(trackingOn) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }

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

        // LatLng home = new LatLng(53.594700, -2.560996);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
        if (cameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
       getDeviceLastLocation();
        showCurrentTrack();
       // showAllVisibleTracks();
    }

    private void showCurrentTrack() {
        waypointDbHelper = new WaypointDbHelper(this);
        ArrayList<LatLng> currentTrack = new ArrayList<LatLng>();
        currentTrack = waypointDbHelper.getTrackPoints(trackid);

        PolylineOptions polylineOptions = new PolylineOptions();

// Create polyline options with existing LatLng ArrayList
        polylineOptions.addAll(currentTrack);
        polylineOptions
                .width(5)
                .color(Color.RED);

        map.addPolyline(polylineOptions);

        LatLngBounds latLngBounds = calcBounds(currentTrack);
        Log.i(TAG, "moveCamera: ");
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(21.5838, 39.2177), 3));
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,1000, 1000, 3));
    }

    private LatLngBounds calcBounds(ArrayList<LatLng> track){
        double north = -88;
        double south = 88;
        double east = -180;
        double west = 180;

       // LatLng latLng = new LatLng(0,0);

        for (LatLng latLng : track){
            if(latLng.latitude > north) { north = latLng.latitude; }
            if(latLng.longitude < west) { west = latLng.longitude; }
            if(latLng.latitude < south) { south = latLng.latitude; }
            if(latLng.longitude > east) { east = latLng.longitude; }
        }

        LatLng topRight = new LatLng(north, east);
        LatLng bottomLeft = new LatLng(south, west);
        return new LatLngBounds(bottomLeft, topRight);
    }
    private void showAllVisibleTracks() {
        trackDbHelper = new TrackDbHelper(this);
        ArrayList<ArrayList<LatLng>> theTrackData = new ArrayList<ArrayList<LatLng>>();
        theTrackData = trackDbHelper.getAllTrackPoints();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //updateGPS();
                } else {
                    Toast.makeText(this, "Permissions needed", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected: restored");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnected: suspended" + i);

    }
    // Events ends
/*
    private void updateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // updateMap(location);
                }
            });
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
        }
    }*/
/*
    private void updateMap(Location location) {
        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
        }

        Log.i("Info", "updateMap: called");
    }*/

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
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
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
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
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                //.addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    // [START maps_current_place_location_permission]
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
    // [END maps_current_place_location_permission]

    // [START maps_current_place_get_device_location]
    private void getDeviceLastLocation() {

        /*         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.*/

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
                            //    map.moveCamera(CameraUpdateFactory.newLatLngZoom( new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }
}