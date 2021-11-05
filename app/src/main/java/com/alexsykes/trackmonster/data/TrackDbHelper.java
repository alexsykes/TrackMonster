package com.alexsykes.trackmonster.data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.alexsykes.trackmonster.data.TrackContract;
import com.alexsykes.trackmonster.data.WaypointContract;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.HashMap;

public class TrackDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "monster.db";
    private static final int DATABASE_VERSION = 1;
    private int trackid;

    public TrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        trackid = prefs.getInt("trackid", 1);
    }

/*    public void addRandomTrack() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, "Third data track");
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION, "A description of yet another track");
        db.insert("tracks", null, values);
    }*/

    @SuppressLint("Range")
    public HashMap<String, String> getTrackData(String trackid) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT * FROM tracks WHERE _id = " + trackid;
        Log.i("Info", query);
        Cursor cursor = db.rawQuery(query, null);

        cursor.moveToFirst();
        HashMap<String, String> theTrack = new HashMap<String, String>();
        theTrack.put("id", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry._ID)));
        theTrack.put("description", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION)));
        theTrack.put("isVisible", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE)));
        theTrack.put("name", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_NAME)));
        cursor.close();
        return theTrack;
    }
    public ArrayList<HashMap<String, String>> getTrackList() {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT t._id AS id, t.name AS name, t.created AS created, COUNT(w._id) AS count, t.isVisible AS isVisible FROM tracks t LEFT JOIN waypoints w ON id = w.trackid " +
                "GROUP BY w.trackid ORDER BY t._id ASC";

        // query = "SELECT * FROM tracks";
        Cursor cursor = db.rawQuery(query, null);
        ArrayList<HashMap<String, String>> theTrackList = new ArrayList<>();
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
    public TrackData getTrackData(int trackid){
        // Declare fields

        // Get database and return data
        SQLiteDatabase db = this.getWritableDatabase();

        // Get track data
        String trackQuery = "SELECT * FROM tracks WHERE _id = " + trackid + " ORDER BY _id ASC";
        Cursor cursor = db.rawQuery(trackQuery, null);
        int count = cursor.getCount();

        String description = "";
        String name = "";
        if(count>0) {
            cursor.moveToFirst();
            name = cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_NAME));
            description = cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION));
        }

        // Get waypoint data
        String waypointQuery = "SELECT * FROM waypoints WHERE trackid = " + trackid + " ORDER BY _id ASC";
        cursor = db.rawQuery(waypointQuery, null);

        // Check for valid data
        count = cursor.getCount();
        double westmost = 0;
        double eastmost = 0;
        double southmost = 0;
        double northmost = 0;
        LatLngBounds latLngBounds;
        ArrayList<LatLng> latLngs = new ArrayList<LatLng>();
        if(count > 0){
            cursor.moveToFirst();
            // Get initial values
            westmost = cursor.getDouble(cursor.getColumnIndex(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LNG));
            eastmost = cursor.getDouble(cursor.getColumnIndex(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LNG));
            northmost = cursor.getDouble(cursor.getColumnIndex(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LAT));
            southmost = cursor.getDouble(cursor.getColumnIndex(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LAT));

            latLngs.add((new LatLng(northmost,westmost)));

            while (cursor.moveToNext()) {
                // Compare map extremes
                double lng = cursor.getDouble(cursor.getColumnIndex(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LNG));
                double lat = cursor.getDouble(cursor.getColumnIndex(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LAT));

                // Add LatLng to Arraylist
                latLngs.add(new LatLng(lat,lng));

                if(lat > northmost) { northmost = lat; };
                if(lat < southmost) { southmost = lat; };
                if(lng > eastmost) { eastmost = lng; };
                if(lng < westmost) { westmost = lng; };
            }
            cursor.close();
        }

        LatLng northeast = new LatLng(northmost, eastmost);
        LatLng southwest = new LatLng(southmost, westmost);
        latLngBounds = new LatLngBounds(southwest, northeast);

        int _id = trackid;
        TrackData trackData = new TrackData(_id, count, latLngs, name, description, northmost, southmost, eastmost, westmost, latLngBounds);
        return new TrackData();
    }

    @SuppressLint("Range")
    public ArrayList<HashMap<String, String>> getShortTrackList() {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT _id , name, isVisible FROM tracks  ORDER BY _id ASC";

        // query = "SELECT * FROM tracks";
        Cursor cursor = db.rawQuery(query, null);
        ArrayList<HashMap<String, String>> theTrackList = new ArrayList<>();
        while (cursor.moveToNext()) {
            HashMap<String, String> tracks = new HashMap<>();
            tracks.put("id",cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry._ID)));
            tracks.put("name", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_NAME)));
            tracks.put("isVisible", cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE)));
            theTrackList.add(tracks);
        }
        cursor.close();
        return theTrackList;
    }

    public void addEntry(String name, String description) {
        SQLiteDatabase db = this.getWritableDatabase();

        //  String query  "INSERT INTO " + DATABASE_NAME
    }

    public void insertNewTrack(String name, String trackDescription, boolean isVisible ) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, name);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION, trackDescription);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE, isVisible);

        db.insert("tracks", null, values);
    }

    public void insertFirstTrack(String trackID, String name ) {
        SQLiteDatabase db = this.getWritableDatabase();
/*        ContentValues values = new ContentValues();
        trackid = Integer.valueOf(trackID);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, name);
        values.put(TrackContract.TrackEntry._ID, trackid);
        db.insert("tracks", null, values);*/

        String query = "INSERT  OR IGNORE INTO tracks  (_id, name) VALUES ('1','"+name+"')";
        db.execSQL(query);
        db.close();
    }

    public void updateTrack(String trackID, String name, String trackDescription, boolean isVisible  ) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, name);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION, trackDescription);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE, isVisible);
        values.put(TrackContract.TrackEntry._ID, trackID);

        String[] whereArgs = new String[]{String.valueOf(trackID)};
        String where = "_id=?";
        db.update("tracks", values, where, whereArgs);
        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<ArrayList<LatLng>> getAllTrackPoints() {

        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT _id  FROM tracks WHERE isVisible = true ORDER BY _id ASC";
        Cursor cursor = db.rawQuery(query, null);
        ArrayList<Integer> theTrackIndices = new ArrayList<Integer>();
        while (cursor.moveToNext()) {
            theTrackIndices.add(cursor.getInt(0));
        }
        cursor.close();

        int count = theTrackIndices.size();

        ArrayList<ArrayList<LatLng>> allTrackData = new ArrayList<ArrayList<LatLng>>();
        for(int i = 0; i<count; i++){
            int index = theTrackIndices.get(i);
            query = "SELECT lat, lng  FROM waypoints WHERE trackid = " + index + " ORDER BY _id ASC";

            Cursor theWaypoints = db.rawQuery(query,null);
            ArrayList<LatLng> theTrackData = new ArrayList<LatLng>();
            while(theWaypoints.moveToNext()) {
                theTrackData.add(new LatLng(theWaypoints.getDouble(0), theWaypoints.getDouble(1)));
            }
            allTrackData.add(theTrackData);
        }
        return allTrackData;
    }
}
