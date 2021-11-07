package com.alexsykes.trackmonster.data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.HashMap;

public class TrackDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "monster.db";
    private static final int DATABASE_VERSION = 1;

    public TrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
        String style = "Road";
        boolean isVisible = true;
        if(count>0) {
            cursor.moveToFirst();
            name = cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_NAME));
            description = cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION));
            isVisible = cursor.getInt(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE)) > 0;
            style = cursor.getString(cursor.getColumnIndex(TrackContract.TrackEntry.COLUMN_TRACKS_STYLE));
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
                latLngs.add(new LatLng(lat, lng));

                if (lat > northmost) {
                    northmost = lat;
                }
                if (lat < southmost) {
                    southmost = lat;
                }
                if (lng > eastmost) {
                    eastmost = lng;
                }
                if (lng < westmost) {
                    westmost = lng;
                }
            }
            cursor.close();
        }

        LatLng northeast = new LatLng(northmost, eastmost);
        LatLng southwest = new LatLng(southmost, westmost);
        latLngBounds = new LatLngBounds(southwest, northeast);

        int _id = trackid;
        TrackData trackData = new TrackData(_id, count, latLngs, name, description, northmost, southmost, eastmost, westmost, latLngBounds, isVisible, style);
        return trackData;
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

    public int insertNewTrack(boolean isCurrent, String name, String trackDescription, boolean isVisible, String style) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        int last = 0;

        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, name);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION, trackDescription);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE, isVisible);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_STYLE, style);

        db.insert("tracks", null, values);

        if (isCurrent) {
            String sql = "SELECT last_insert_rowid()";
            Cursor result = db.rawQuery(sql, null);
            result.moveToFirst();
            last = result.getInt(0);

        }
        db.close();
        return last;
    }

    public void insertFirstTrack(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "INSERT  OR IGNORE INTO tracks  (_id, name, isvisible, style ) VALUES ('1','" + name + "', true, 'Track')";
        db.execSQL(query);
        db.close();
    }

    public void updateTrack(int trackID, String name, String trackDescription, boolean isVisible, String style) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_NAME, name);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION, trackDescription);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE, isVisible);
        values.put(TrackContract.TrackEntry.COLUMN_TRACKS_STYLE, style);
        // values.put(TrackContract.TrackEntry.COLUMN_TRACKS_UPDATED, isVisible);
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
