package com.alexsykes.trackmonster.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.data.TrackDbHelper;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;

public class TrackDialogActivity extends AppCompatActivity {
    private static final String TAG = "Info";
    TextInputLayout nameTextInputLayout;
    TextInputLayout descriptionTextInputLayout;
    CheckBox isVisibleCheckBox;
    CheckBox isCurrentCheckBox;
    String name;
    String task;
    String trackDescription;
    boolean isVisible, isCurrent;
    String trackID;
    String trackIdPrefs;
    TrackDbHelper trackDbHelper;
    Intent intent;
    Bundle extras;
    HashMap<String, String> theTrack;
    SharedPreferences prefs ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_dialog);

        // Setup UI
        nameTextInputLayout = findViewById(R.id.track_name_text_input);
        descriptionTextInputLayout = findViewById(R.id.track_detail_text_input);
        isVisibleCheckBox = findViewById(R.id.track_visibility_checkBox);
        isCurrentCheckBox = findViewById(R.id.track_active_checkBox);

        // Get data
        trackDbHelper = new TrackDbHelper(this);
        intent = getIntent();
        trackID = intent.getExtras().getString("trackid","1");
        task = intent.getExtras().getString("task");

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        trackIdPrefs = String.valueOf(prefs.getInt("trackid", 1));

        // Get existing track details and show values
        if (task.equals("update")) {
            theTrack = trackDbHelper.getTrackData(trackID);

            nameTextInputLayout.getEditText().setText(theTrack.get("name"));
            descriptionTextInputLayout.getEditText().setText(theTrack.get("description"));

            isCurrent = theTrack.get("id").equals(trackIdPrefs);

            isVisible = (theTrack.get("isVisible")).equals("1");
            isVisibleCheckBox.setChecked(isVisible);
            isCurrentCheckBox.setChecked(isCurrent);
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

        if (isCurrent) {
            editor.putInt("trackid", Integer.valueOf(trackID));
            editor.apply();
        }

        if (task.equals("new")) {
            int lastInsert = trackDbHelper.insertNewTrack(isCurrent, name, trackDescription, isVisible);
            trackID = String.valueOf(lastInsert);
        } else if (task.equals("update")) {
            trackDbHelper.updateTrack(trackID, name, trackDescription, isVisible);
        }


        if (isCurrent) {
            editor.putInt("trackid", Integer.valueOf(trackID));
            editor.apply();
        }
        finish();
    }
}