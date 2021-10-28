package com.alexsykes.trackmonster.handlers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.activities.TrackListActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.TrackHolder> {
    ArrayList<HashMap<String, String>> theTrackList;
    HashMap<String, String> theTrack;
    OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HashMap<String, String> theTrack);
    }

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
    public TrackHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.track_row, viewGroup, false);
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
    public int getItemCount() { return theTrackList.size(); }

    public static class TrackHolder extends RecyclerView.ViewHolder {
        TextView idTextView, nameTextView, countTextView, createdTextView;

        public TrackHolder(@NonNull View itemView) {
            super(itemView);
            idTextView = itemView.findViewById(R.id.id);
            nameTextView = itemView.findViewById(R.id.name);
            countTextView = itemView.findViewById(R.id.numWP);
            createdTextView = itemView.findViewById(R.id.date);
        }

        public void bind(final HashMap<String, String> theTrial, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String id = theTrial.get("id").toString();
                    Context context = v.getContext();
                    ((TrackListActivity) context).onClickCalled(id);

                }
            });
        }
    }
}