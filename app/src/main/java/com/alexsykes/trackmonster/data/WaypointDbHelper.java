package com.alexsykes.trackmonster.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.Marker;

public class WaypointDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "monster.db";
    private static final int DATABASE_VERSION = 1;

    public WaypointDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
            values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_TRACKID, 3);
            values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_ALT, alt);

            db.insertWithOnConflict("waypoints", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void addLocation(double lat, double lng, double speed, double bearing, double alt, int trackid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LAT, lat);
        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LNG, lng);
        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_NAME, "Test");
        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_TRACKID, trackid);
        values.put(WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_ALT, alt);

        db.insert("waypoints", null, values);
    }
}
