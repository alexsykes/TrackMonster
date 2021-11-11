package com.alexsykes.trackmonster.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alexsykes.trackmonster.R;
import com.alexsykes.trackmonster.activities.TrackListActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.TrackHolder> {
    ArrayList<HashMap<String, String>> theTrackList;
    HashMap<String, String> theTrack;
    static SharedPreferences preferences;

    public TrackListAdapter(ArrayList<HashMap<String, String>> theTrackList) {
        this.theTrackList = theTrackList;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public TrackHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        preferences  = PreferenceManager.getDefaultSharedPreferences(viewGroup.getContext());
//        trackid = preferences.getInt("trackid", 1);
//        Log.i("Info", "trackid: " + trackid);
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.track_row, viewGroup, false);
        TrackHolder trackHolder = new TrackHolder(v);
        return trackHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TrackHolder holder, int position) {
        theTrack = theTrackList.get(position);
        // holder.itemView.setBackgroundResource(R.color.list_highlist);
        holder.isCurrentRadioButton.setChecked(1 == Integer.valueOf(theTrack.get("isCurrent")));
        // holder.itemView.setBackgroundResource(R.color.list_highlist);
        holder.isVisCheckbox.setChecked(1 == Integer.valueOf(theTrack.get("isVisible")));
        holder.nameTextView.setText(theTrack.get("name"));
        // holder.bind(theTrack, listener);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Info", "onClick: " + theTrackList.get(position).get("id"));
                String id = theTrackList.get(position).get("id");
                Context context = view.getContext();
                ((TrackListActivity) context).onClickCalled(id);
            }
        });

        holder.isCurrentRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int trackid = Integer.valueOf(theTrackList.get(position).get("id"));
                Log.i("Info", "isCurrentRadioButton.onCheckedChanged: " + trackid);

                Context context = buttonView.getContext();
                ((TrackListActivity) context).updateCurrent(trackid);
            }
        });

        holder.isVisCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int trackid = Integer.valueOf(theTrackList.get(position).get("id"));
                Log.i("Info", "isVisCheckbox.onCheckedChanged: " + trackid);
                boolean checked;
                checked = holder.isVisCheckbox.isChecked();
                Context context = buttonView.getContext();
                ((TrackListActivity) context).updateVisible(trackid, checked);
            }
        });

    }

    @Override
    public int getItemCount() { return theTrackList.size(); }

    public static class TrackHolder extends RecyclerView.ViewHolder {
        TextView idTextView, nameTextView;
        CheckBox isVisCheckbox;
        RadioButton isCurrentRadioButton;

        public TrackHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name);
            isCurrentRadioButton = itemView.findViewById(R.id.isCurrentRadioButton);
            isVisCheckbox = itemView.findViewById(R.id.isVisCheckbox);
        }

//        public void bind(final HashMap<String, String> theTrial, final OnItemClickListener listener) {
//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    String id = theTrial.get("id");
//                    Context context = v.getContext();
//                    ((TrackListActivity) context).onClickCalled(id);
//                }
//            });
//            itemView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    String id = theTrial.get("id");
//                    setActive(id);
//                    Log.i("Info", "onLongClick: " + id);
//                    itemView.setBackgroundColor(Color.GREEN);
//                    return true; // Halt execution of normal click
//                }
//            });
//        }

        private void setActive(String id) {
            int trackID = Integer.valueOf(id);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("trackid", trackID);
            editor.apply();
        }
    }
}