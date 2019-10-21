package com.kratiukkarol.leader.DAO;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.kratiukkarol.leader.model.GeoPoint;

import java.util.List;

@Dao
public interface GeoPointDAO {

    @Insert
    void insert(GeoPoint geoPoint);

    @Update
    void update(GeoPoint geoPoint);

    @Delete
    void delete(GeoPoint geoPoint);

    @Query("DELETE FROM geo_points")
    void deleteAllPoints();

    @Query("SELECT * FROM geo_points")
    LiveData<List<GeoPoint>> getAllGeoPoints();

    @Query("SELECT * FROM geo_points ORDER BY id DESC LIMIT 1")
    LiveData<GeoPoint> getLatestGeoPoint();
}
