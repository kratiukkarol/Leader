package com.kratiukkarol.leader.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.kratiukkarol.leader.DAO.GeoPointDAO;
import com.kratiukkarol.leader.model.GeoPoint;

@Database(entities = {GeoPoint.class}, version = 1)
public abstract class GeoPointsDatabase extends RoomDatabase {

    private static volatile GeoPointsDatabase instance;

    public abstract GeoPointDAO geoPointDAO();

    public static synchronized GeoPointsDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(), GeoPointsDatabase.class, "geoPoints_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public static void destroyInstance(){
        instance = null;
    }
}
