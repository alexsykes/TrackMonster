package com.alexsykes.trackmonster;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.alexsykes.trackmonster.data.TrackContract;
import com.alexsykes.trackmonster.data.TrackDbHelper;
import com.alexsykes.trackmonster.data.WaypointContract;
import com.alexsykes.trackmonster.data.WaypointDbHelper;

public class TrackMonster extends Application {

    // Databases
    private WaypointDbHelper waypointDbHelper;
    private TrackDbHelper trackDbHelper;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Info", "TrackMonster class: OnAppStart");

        waypointDbHelper = new WaypointDbHelper(this);
        trackDbHelper = new TrackDbHelper(this);
     //   waypointDbHelper.generateRandomData(30);

        // Create database connection
        dbInit();

    }

    private void dbInit() {
        // Database operations - https://www.tutorialspoint.com/android/android_sqlite_database.htm
        // First, get your database
        final String DATABASE_NAME = "monster.db";
        SQLiteDatabase db = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);

        // Create a String that contains the SQL statement to create the scores table
        String SQL_CREATE_WAYPOINT_TABLE = "CREATE TABLE IF NOT EXISTS " + WaypointContract.WaypointEntry.TABLE_NAME + " ("
                + WaypointContract.WaypointEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_TRACKID + " INTEGER , "
                + WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_ALT + " REAL, "
                + WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_NAME + " TEXT NOT NULL, "
                + WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_CREATED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_UPDATED + " TEXT , "
                + WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LNG + " REAL , "
                + WaypointContract.WaypointEntry.COLUMN_WAYPOINTS_LAT + " REAL );";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_WAYPOINT_TABLE);

        String SQL_CREATE_TRACK_TABLE = "CREATE TABLE IF NOT EXISTS " + TrackContract.TrackEntry.TABLE_NAME + " ("
                + TrackContract.TrackEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TrackContract.TrackEntry.COLUMN_TRACKS_NAME + " TEXT NOT NULL, "
                + TrackContract.TrackEntry.COLUMN_TRACKS_DESCRIPTION + " TEXT,  "
                + TrackContract.TrackEntry.COLUMN_TRACKS_ISVISIBLE + " INTEGER DEFAULT 1,  "
                + TrackContract.TrackEntry.COLUMN_TRACKS_CREATED + " TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + TrackContract.TrackEntry.COLUMN_TRACKS_UPDATED  + " TEXT );";

        db.execSQL(SQL_CREATE_TRACK_TABLE);

        trackDbHelper.insertFirstTrack("1", "Track 1");
    }
    protected boolean canConnect() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
