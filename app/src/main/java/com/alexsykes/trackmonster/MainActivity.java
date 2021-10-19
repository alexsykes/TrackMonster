package com.alexsykes.trackmonster;

import androidx.appcompat.app.AppCompatActivity;

import android.app.UiAutomation;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// See https://developers.google.com/maps/documentation/android-sdk/map-with-marker
// https://developers.google.com/maps/documentation/android-sdk/map

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    @Override
    public void onMapReady(GoogleMap map) {
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.

        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);

        LatLng home = new LatLng(53.594700, -2.560996);
        LatLng madrakah = new LatLng(22.184852, 40.726148);
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.addMarker(new MarkerOptions()
                .position(madrakah)
                .title("Madrakah"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(home, 10));
        map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

        CameraPosition cameraPosition = new CameraPosition.Builder()
        .target(home)
                .zoom(15)
        .bearing(90)
        .build();

         map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
}