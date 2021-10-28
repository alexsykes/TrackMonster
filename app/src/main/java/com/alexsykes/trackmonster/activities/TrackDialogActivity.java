package com.alexsykes.trackmonster.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;

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
    TrackDbHelper trackDbHelper;
    Intent intent;
    Bundle extras;
    HashMap<String, String> theTrack;

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

        // Get existing track details and show values
        if (task.equals("update")) {
            theTrack = trackDbHelper.getTrackData(trackID);

            nameTextInputLayout.getEditText().setText(theTrack.get("name"));
            descriptionTextInputLayout.getEditText().setText(theTrack.get("description"));

//            if ((theTrack.get("isCurrent")).equals("1")) {
//                isCurrent = true;
//            } else {
//                isCurrent = false;
//            }

            if ((theTrack.get("isVisible")).equals("1")) {
                isVisible = true;
            } else {
                isVisible = false;
            }

         //   isCurrentCheckBox.setChecked(isCurrent);
            isVisibleCheckBox.setChecked(isVisible);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.track_dialog_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done_menu_item:
                saveTrackDetails();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveTrackDetails() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();


        name = nameTextInputLayout.getEditText().getText().toString();
        trackDescription = descriptionTextInputLayout.getEditText().getText().toString();
        isVisible = isVisibleCheckBox.isChecked();
        isCurrent = isCurrentCheckBox.isChecked();

        if (isCurrent) {
            editor.putInt("trackid", Integer.valueOf(trackID));
            editor.apply();
        }

        if(task.equals("new")) {
            trackDbHelper.insertNewTrack(name, trackDescription, isVisible);
        } else if (task.equals("update")) {
            trackDbHelper.updateTrack(trackID, name, trackDescription, isVisible);
        }
        finish();
    }
}