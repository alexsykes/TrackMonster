package com.alexsykes.trackmonster.data;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;

public class TrackData {
    public TrackData(int _id, int count, ArrayList<LatLng> latLngs, String name, String description, double north, double south, double east, double west, LatLngBounds latLngBounds) {
        this._id = _id;
        this.count = count;
        this.latLngs = latLngs;
        this.name = name;
        this.description = description;
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
        this.latLngBounds = latLngBounds;
    }

    private int _id;
    private int count;
    private ArrayList<LatLng> latLngs;
    private String name;
    private String description;
    private double north, south, east, west;
    private LatLngBounds latLngBounds;

    public TrackData() {
    }

    public int getCount() {
        return count;
    }

    public ArrayList<LatLng> getLatLngs() {
        return latLngs;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getNorth() {
        return north;
    }

    public double getSouth() {
        return south;
    }

    public double getEast() {
        return east;
    }

    public double getWest() {
        return west;
    }

    public LatLngBounds getLatLngBounds() {
        return latLngBounds;
    }
}
