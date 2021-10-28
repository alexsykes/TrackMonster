package com.alexsykes.trackmonster.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;

import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.data.TrackDbHelper;
import com.google.android.material.textfield.TextInputLayout;

public class TrackDialogActivity extends AppCompatActivity {
    private static final String TAG = "Info";
    TextInputLayout nameTextInputLayout;
    TextInputLayout detailTextInputLayout;
    CheckBox isVisibleCheckBox;
    String name;
    String task;
    String trackDescription;
    boolean isVisible;
    String trackID;
    TrackDbHelper trackDbHelper;
    Intent intent;
    Bundle extras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_dialog);
        nameTextInputLayout = findViewById(R.id.track_name_text_input);
        detailTextInputLayout = findViewById(R.id.track_detail_text_input);
        isVisibleCheckBox = findViewById(R.id.track_visibility_checkBox);
        trackDbHelper = new TrackDbHelper(this);
        intent = getIntent();
        trackID = intent.getExtras().getString("trackid", "-1");
        task = intent.getExtras().getString("task");
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
        Log.i(TAG, "saveTrackDetails: in progress");
        name = nameTextInputLayout.getEditText().getText().toString();
        trackDescription = detailTextInputLayout.getEditText().getText().toString();
        isVisible = isVisibleCheckBox.isChecked();
        extras = intent.getExtras();
        trackID = extras.getString("trackid", "-1");

        trackDbHelper.insertNewTrack(trackID, name, trackDescription, isVisible);
        finish();
    }
}