package com.alexsykes.trackmonster.handlers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

      //  holder.idTextView.setText(theTrack.get("id"));
        // holder.createdTextView.setText(theCreated);
        //  holder.isVisibleView.setImage("@drawable/ic_baseline_add_circle_24");
        holder.nameTextView.setText(theTrack.get("name"));
        holder.bind(theTrack, listener);
    }

    @Override
    public int getItemCount() { return theTrackList.size(); }

    public static class TrackHolder extends RecyclerView.ViewHolder {
        TextView idTextView, nameTextView;
        ImageView isVisibleView, isCurrentView;

        public TrackHolder(@NonNull View itemView) {
            super(itemView);
//            idTextView = itemView.findViewById(R.id.id);
//            isVisibleView = itemView.findViewById(R.id.isVisible);
//            isCurrentView = itemView.findViewById(R.id.isActive);

            nameTextView = itemView.findViewById(R.id.name);
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