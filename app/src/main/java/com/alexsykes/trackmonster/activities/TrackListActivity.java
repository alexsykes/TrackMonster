package com.alexsykes.trackmonster.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.data.TrackDbHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class TrackListActivity extends AppCompatActivity {
    TrackDbHelper trackDbHelper;
    ArrayList<HashMap<String, String>> theTrackList;
    RecyclerView trackView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_list);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        trackDbHelper = new TrackDbHelper(this);
        populateTrackList();

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

    public static class TrackHolder extends RecyclerView.ViewHolder {
        TextView idTextView, nameTextView, countTextView, createdTextView;
        public TrackHolder(@NonNull View itemView) {
            super(itemView);
            idTextView = itemView.findViewById(R.id.id);
            nameTextView = itemView.findViewById(R.id.name);
            countTextView = itemView.findViewById(R.id.numWP);
            createdTextView = itemView.findViewById(R.id.date);
        }

        public void bind(final HashMap<String, String> theTrack, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String id = theTrack.get("id");
                    Log.i("Info", "onClick: itemClicked " + id);
                }
            });
        }
    }

    private class TrackListAdapter extends RecyclerView.Adapter<TrackHolder> {
        ArrayList<HashMap<String, String>> theTrackList;
        HashMap<String, String> theTrack;
        OnItemClickListener listener;

        public TrackListAdapter(ArrayList<HashMap<String, String>> theTrackList) {
            this.theTrackList = theTrackList;
        }

        public TrackListAdapter(ArrayList<HashMap<String, String>> theTrackList, OnItemClickListener listener) {
            this.theTrackList = theTrackList;
            this.listener = listener;
        }


        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }



        @NonNull
        @Override
        public TrackHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_row, parent, false);
            TrackHolder trackHolder = new TrackHolder(v);
            return trackHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull TrackHolder holder, int position) {
            theTrack = theTrackList.get(position);

            String theCreated = theTrack.get("created");
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z");
            // Date date = format.parse(theCreated);

            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm:ss");
            // String created = timeFormat.format(theCreated);
            holder.idTextView.setText(theTrack.get("id"));
            holder.nameTextView.setText(theTrack.get("name"));
            holder.countTextView.setText(theTrack.get("count"));
            holder.bind(theTrack, listener);
           // holder.createdTextView.setText(theCreated);
        }

        @Override
        public int getItemCount() {
            return theTrackList.size();
        }


    }

    public interface OnItemClickListener {
        void onItemClick(HashMap<String, String> theTrackList);
    }
}