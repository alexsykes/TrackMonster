package com.alexsykes.trackmonster.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.handlers.TrackListAdapter;
import com.alexsykes.trackmonster.data.TrackDbHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;

public class TrackListActivity extends AppCompatActivity {
    TrackDbHelper trackDbHelper;
    ArrayList<HashMap<String, String>> theTrackList;
    RecyclerView trackView;
    private static final int TEXT_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_list);

        trackDbHelper = new TrackDbHelper(this);
        populateTrackList();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(getApplicationContext(), TrackDialogActivity.class);

                startActivity(intent);
            }
        });

    }private void showAddItemDialog(Context c) {
        final EditText taskEditText = new EditText(c);
        AlertDialog dialog = new AlertDialog.Builder(c)
                .setTitle("Add a new task")
                .setMessage("What do you want to do next?")
                .setView(taskEditText)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String task = String.valueOf(taskEditText.getText());
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    public void onClickCalled(String trialid) {
        // Call another activity here and pass some arguments to it.
        Intent intent = new Intent(this, TrackDialogActivity.class);
        // intent.putExtra("id", trialid);
        startActivity(intent);
      //  startActivityForResult(intent, TEXT_REQUEST);
    }

    private void populateTrackList() {
        theTrackList = trackDbHelper.getTrackList();
        trackView = findViewById(R.id.trackView);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        trackView.setLayoutManager(llm);
        trackView.setHasFixedSize(true);
        initializeAdapter();
    }
    private void initializeAdapter() {
        TrackListAdapter adapter = new TrackListAdapter(theTrackList);
        trackView.setAdapter(adapter);
    }
    public interface OnItemClickListener {
        void onItemClick(HashMap<String, String> theTrackList);
    }
}