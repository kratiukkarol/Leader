package com.kratiukkarol.leader.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "geo_points")
public class GeoPoint {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private double latitude;

    private double longitude;

    public GeoPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
