package com.alexsykes.trackmonster;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.alexsykes.trackmonster.data.TrackContract;
import com.alexsykes.trackmonster.data.WaypointContract;

import java.util.ArrayList;
import java.util.HashMap;

public class TrackDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "monster.db";
    private static final int DATABASE_VERSION = 1;

    public TrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void addRandomTrack() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, "Another data track");
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION, "A description of a different track");

        db.insert("tracks", null, values);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<HashMap<String, String>> getTrackList() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> theTrackList = new ArrayList<>();

        String query = "SELECT t._id AS id, t.name AS name, t.created AS created, COUNT(w._id) AS count FROM tracks t LEFT JOIN waypoints w ON id = w.trackid " +
                "GROUP BY w.trackid ORDER BY t._id ASC";

        // query = "SELECT * FROM tracks";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> tracks = new HashMap<>();
            tracks.put("id", cursor.getString(0));
            tracks.put("name", cursor.getString(1));
            tracks.put("created", cursor.getString(2));
            tracks.put("count", cursor.getString(3));
            theTrackList.add(tracks);
        }
        cursor.close();
        return theTrackList;

    }
}
