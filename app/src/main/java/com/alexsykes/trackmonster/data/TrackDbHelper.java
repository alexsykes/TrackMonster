package com.alexsykes.trackmonster.data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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

        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, "Third data track");
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION, "A description of yet another track");
        db.insert("tracks", null, values);
    }

    @SuppressLint("Range")
    public HashMap<String, String> getTrackData(String trackid) {
        SQLiteDatabase db = this.getWritableDatabase();
        HashMap<String, String> theTrack = new HashMap<String, String>();

        String query = "SELECT * FROM tracks WHERE _id = " + trackid;
        Log.i("Info", query);
        Cursor cursor = db.rawQuery(query, null);

        cursor.moveToFirst();
        theTrack.put("id", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry._ID)));
        theTrack.put("description", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION)));
        theTrack.put("isCurrent", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_ISCURRENT)));
        theTrack.put("isVisible", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE)));
        theTrack.put("name", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_NAME)));
        cursor.close();
        return theTrack;
    }
    public ArrayList<HashMap<String, String>> getTrackList() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> theTrackList = new ArrayList<>();

        String query = "SELECT t._id AS id, t.name AS name, t.created AS created, COUNT(w._id) AS count, t.isVisible AS isVisible FROM tracks t LEFT JOIN waypoints w ON id = w.trackid " +
                "GROUP BY w.trackid ORDER BY t._id ASC";

        // query = "SELECT * FROM tracks";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> tracks = new HashMap<>();
            tracks.put("id", cursor.getString(0));
            tracks.put("name", cursor.getString(1));
            tracks.put("created", cursor.getString(2));
            tracks.put("count", cursor.getString(3));
            tracks.put("isVisible", cursor.getString(4));
            theTrackList.add(tracks);
        }
        cursor.close();
        return theTrackList;
    }



    @SuppressLint("Range")
    public ArrayList<HashMap<String, String>> getShortTrackList() {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<HashMap<String, String>> theTrackList = new ArrayList<>();

        String query = "SELECT _id , name ,  isVisible, isCurrent FROM tracks  ORDER BY _id ASC";

        // query = "SELECT * FROM tracks";
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            HashMap<String, String> tracks = new HashMap<>();
            tracks.put("id",cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry._ID)));
            tracks.put("name", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_NAME)));
            tracks.put("isVisible", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE)));
            tracks.put("isCurrent", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_ISCURRENT)));
            theTrackList.add(tracks);
        }
        cursor.close();
        return theTrackList;
    }


    public void addEntry(String name, String description) {
        SQLiteDatabase db = this.getWritableDatabase();

      //  String query  "INSERT INTO " + DATABASE_NAME
    }

    public void insertNewTrack(String trackID, String name, String trackDescription, boolean isVisible) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, name);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION, trackDescription);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE, isVisible);

        db.insert("tracks", null, values);
    }

    public void updateTrack(String trackID, String name, String trackDescription, boolean isVisible) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, name);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION, trackDescription);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE, isVisible);
        values.put(TrackContract.TrackEntry._ID, trackID);

       // db.updateWithOnConflict("tracks", null, values, );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
