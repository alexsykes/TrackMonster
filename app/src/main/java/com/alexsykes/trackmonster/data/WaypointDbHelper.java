package com.alexsykes.trackmonster.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

public class WaypointDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "monster.db";
    private static final int DATABASE_VERSION = 1;
    private int trackid;

    public WaypointDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        trackid = prefs.getInt("trackid", 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void generateRandomData(int itemCount){
        SQLiteDatabase db = this.getWritableDatabase();
        String name;
        double lat = (0.5 - Math.random()) * 30;
        double lng = (0.5 - Math.random()) * 30;
        double alt = Math.random() * 1000;


        for(int i = 0; i < itemCount; i++) {
            name = "Waypoint " + i;
            lat += (0.5 - Math.random()) * 10;
            lng += (0.5 - Math.random()) * 10;
            alt = Math.random() * 1000;
            String data = name + "::Data - lng: "  + lng + " lat: " + lat;
            Log.i("Info", data);

            ContentValues values = new ContentValues();

            values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LAT, lat);
            values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LNG, lng);
            values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_NAME, name);
            values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_TRACKID, trackid);
            values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_ALT, alt);

            db.insertWithOnConflict("waypoints", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void addLocation(double lat, double lng, double speed, double bearing, double alt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LAT, lat);
        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LNG, lng);
        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_NAME, "Test");
        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_TRACKID, trackid);
        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_ALT, alt);

        db.insert("waypoints", null, values);
    }



    public ArrayList<LatLng> getTrackPoints(int trackID){
        ArrayList<LatLng> theWaypoints = new ArrayList<LatLng>();

        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT lat, lng FROM waypoints WHERE trackid = '" + trackID + "' ORDER BY _id ASC";


        Cursor result = db.rawQuery(query, null);
       // result.moveToFirst();
        while (result.moveToNext()) {
            theWaypoints.add(new LatLng(result.getDouble(0), result.getDouble(1)));
           // result.moveToNext();
        }
        return theWaypoints;
    }
}
