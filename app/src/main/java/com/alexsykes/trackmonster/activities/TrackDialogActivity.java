package com.alexsykes.trackmonster.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class TrackDialogActivity extends AppCompatActivity implements OnMapReadyCallback {
    // Request code for creating a PDF document.
    private static final int CREATE_FILE = 1;
    private static final int PICK_PDF_FILE = 2;

    private static final String TAG = "Info";
    TextInputLayout nameTextInputLayout;
    TextInputLayout descriptionTextInputLayout;
    CheckBox isVisibleCheckBox;
    CheckBox isCurrentCheckBox;
    RadioGroup trackStyleGroup;
    RadioButton undefinedButton, trackButton, roadButton, majorRoadButton;
    String name;
    String task;
    String style;
    String trackDescription;
    boolean isVisible, isCurrent;
    int trackID;
    TrackDbHelper trackDbHelper;
    Intent intent;
    SharedPreferences prefs;
    TrackData trackData;

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
        // Map components
        View mLayout = findViewById(R.id.trackMap);
        trackStyleGroup = findViewById(R.id.trackStyleGroup);
        undefinedButton = findViewById(R.id.undefinedButton);
        trackButton = findViewById(R.id.trackButton);
        roadButton = findViewById(R.id.roadButton);
        majorRoadButton = findViewById(R.id.majorRoadButton);

        // Add listeners for changes
        nameTextInputLayout.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveTrackDetails();
            }
        });

        descriptionTextInputLayout.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveTrackDetails();
            }
        });

        trackStyleGroup.setOnCheckedChangeListener((group, checkedId) -> saveTrackDetails());

        isVisibleCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> saveTrackDetails());

        isCurrentCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> saveTrackDetails());

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.trackMap);
        mapFragment.getMapAsync(this);

        // Get data
        trackDbHelper = new TrackDbHelper(this);
        intent = getIntent();
        trackID = Integer.parseInt(intent.getExtras().getString("trackid", "1"));
        task = intent.getExtras().getString("task");

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // trackIdFromPrefs = prefs.getInt("trackid", 1);

        // Get existing track details and show values
        if (task.equals("update")) {
            trackData = trackDbHelper.getTrackData(trackID);

            nameTextInputLayout.getEditText().setText(trackData.getName());
            descriptionTextInputLayout.getEditText().setText(trackData.getDescription());
            isVisible = trackData.isVisible();
            isCurrent = trackData.isCurrent();
            isVisibleCheckBox.setChecked(isVisible);
            isCurrentCheckBox.setChecked(isCurrent);
            style = trackData.getStyle();

            switch (style) {
                case "Track":
                    trackButton.setChecked(true);
                    break;
                case "Road":
                    roadButton.setChecked(true);
                    break;
                case "Major road":
                    majorRoadButton.setChecked(true);
                    break;
                case "Undefined":
                    undefinedButton.setChecked(true);
                    break;
                default:
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.track_dialog_bottom_menu, menu);
        return true;
    }

    private void saveTrackDetails() {
        name = nameTextInputLayout.getEditText().getText().toString();
        trackDescription = descriptionTextInputLayout.getEditText().getText().toString();
        isVisible = isVisibleCheckBox.isChecked();
        isCurrent = isCurrentCheckBox.isChecked();

        int radioButtonID = trackStyleGroup.getCheckedRadioButtonId();
        RadioButton selected = trackStyleGroup.findViewById(radioButtonID);
        String style = selected.getText().toString();

        if (task.equals("new")) {
            trackID = trackDbHelper.insertNewTrack(isCurrent, name, trackDescription, isVisible, style);
            task = "update";
        } else if (task.equals("update")) {
            trackDbHelper.updateTrack(trackID, name, trackDescription, isVisible, isCurrent, style);
        }
        trackDbHelper.close();
    }

    @Override
    public void onStop() {
        saveTrackDetails();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setCompassEnabled(false);
        uiSettings.setAllGesturesEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        if (task.equals("update")) {
            // Display track on map
            displayTrack();
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_menu_item:
                saveTrackData();
                return true;
            case R.id.email_menu_item:
                emailTrackData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void emailTrackData() {
        trackDataToKML(trackData);
        openFile();
    }

    private void saveTrackData() {
        String filename = "trackdata.kml";
        trackDataToKML(trackData);
        createFile(filename);
    }


    private void createFile(String filename) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.setType("Application/Vnd.google-earth.kml");
        intent.putExtra(Intent.EXTRA_TITLE, "invoice.pdf");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, "Documents");

        startActivityForResult(intent, CREATE_FILE);
    }

    // private void openFile(Uri pickerInitialUri) {
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // intent.setType("application/pdf");
        intent.setType("Application/Vnd.google-earth.kml");

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, PICK_PDF_FILE);
    }

    private void displayTrack() {
        final int COLOR_DARK_GREEN_ARGB = 0xff388E3C;
        final int COLOR_LIGHT_GREEN_ARGB = 0xff81C784;
        final int COLOR_DARK_ORANGE_ARGB = 0xffF57F17;
        final int COLOR_LIGHT_ORANGE_ARGB = 0xffF9A825;

        int strokeWidth = 5;
        int strokeColour = COLOR_DARK_GREEN_ARGB;

        LatLngBounds latLngBounds = trackData.getLatLngBounds();
        ArrayList<LatLng> latLngs = trackData.getLatLngs();
        PolylineOptions polylineOptions = new PolylineOptions();
        // Create polyline options with existing LatLng ArrayList
        polylineOptions.addAll(latLngs);


        Polyline polyline = map.addPolyline(polylineOptions);
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
        polyline.setColor(strokeColour);
        polyline.setWidth(strokeWidth);

        map.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 1000, 1000, 3));
    }


    private String trackDataToKML(TrackData trackData) {
        ArrayList<LatLng> latLngs = trackData.getLatLngs();
        int numPoints = latLngs.size();
        String name = trackData.getName();
        String description = trackData.getDescription();

        String pointsString = "";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "  <Document>");
        stringBuilder.append("\n" +
                "    <name>" + name + "</name>\n");
        stringBuilder.append("\n" +
                "    <description>" + description + "</description>\n");
        stringBuilder.append("    <Style id=\"yellowLineGreenPoly\">\n" +
                "      <LineStyle>\n" +
                "        <color>7f00ffff</color>\n" +
                "        <width>4</width>\n" +
                "      </LineStyle>\n" +
                "      <PolyStyle>\n" +
                "        <color>7f00ff00</color>\n" +
                "      </PolyStyle>\n" +
                "    </Style>\n<styleUrl>#yellowLineGreenPoly</styleUrl>\n");

        stringBuilder.append("      <LineString>\n" +
                "        <extrude>1</extrude>\n" +
                "        <tessellate>1</tessellate>\n" +
                "        <altitudeMode>absolute</altitudeMode>\n" +
                "        <coordinates>");

        for (int i = 0; i < numPoints; i++) {
            LatLng latLng = latLngs.get(i);
            String lat = String.valueOf(latLng.latitude);
            String lng = String.valueOf(latLng.longitude);
            stringBuilder.append(lng);
            stringBuilder.append(",");
            stringBuilder.append(lat);
            stringBuilder.append(",");
            stringBuilder.append("0");
            stringBuilder.append("\n");
        }
        stringBuilder.append("        </coordinates>\n" +
                "      </LineString>\n" +
                "    </Placemark>\n" +
                "  </Document>\n" +
                "</kml>");

        return stringBuilder.toString();
    }

    private String latLngToKMLString(LatLng latLng) {
        String latLngString = "";


        return latLngString;
    }
}