package com.kratiukkarol.leader.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.kratiukkarol.leader.DAO.GeoPointDAO;
import com.kratiukkarol.leader.database.GeoPointsDatabase;
import com.kratiukkarol.leader.model.GeoPoint;

import java.util.List;

public class GeoPointRepository {

    private GeoPointDAO geoPointDAO;
    private LiveData<List<GeoPoint>> allGeoPoints;
    private LiveData<GeoPoint> latestGeoPoint;

    public GeoPointRepository(Application application){
        GeoPointsDatabase database = GeoPointsDatabase.getInstance(application);
        geoPointDAO = database.geoPointDAO();
        allGeoPoints = geoPointDAO.getAllGeoPoints();
        latestGeoPoint = geoPointDAO.getLatestGeoPoint();
    }

    public void insert(GeoPoint geoPoint){
        new InsertGeoPointAsyncTask(geoPointDAO).execute(geoPoint);
    }

    public void update(GeoPoint geoPoint){
        new UpdateGeoPointAsyncTask(geoPointDAO).execute(geoPoint);
    }

    public void delete(GeoPoint geoPoint){
        new DeleteGeoPointAsyncTask(geoPointDAO).execute(geoPoint);
    }

    public void deleteAllGeoPoints(){
        new DeleteAllGeoPointsAsyncTask(geoPointDAO).execute();
    }

    public LiveData<List<GeoPoint>> getAllGeoPoints(){
        return allGeoPoints;
    }

    public LiveData<GeoPoint> getLatestGeoPoint(){
        return latestGeoPoint;
    }

    private static class InsertGeoPointAsyncTask extends AsyncTask<GeoPoint, Void, Void>{
        private GeoPointDAO geoPointDAO;

        private InsertGeoPointAsyncTask(GeoPointDAO geoPointDAO){
            this.geoPointDAO = geoPointDAO;
        }

        @Override
        protected Void doInBackground(GeoPoint... geoPoints) {
            geoPointDAO.insert(geoPoints[0]);
            return null;
        }
    }

    private static class UpdateGeoPointAsyncTask extends AsyncTask<GeoPoint, Void, Void>{
        private GeoPointDAO geoPointDAO;

        private UpdateGeoPointAsyncTask(GeoPointDAO geoPointDAO){
            this.geoPointDAO = geoPointDAO;
        }

        @Override
        protected Void doInBackground(GeoPoint... geoPoints) {
            geoPointDAO.update(geoPoints[0]);
            return null;
        }
    }

    private static class DeleteGeoPointAsyncTask extends AsyncTask<GeoPoint, Void, Void>{
        private GeoPointDAO geoPointDAO;

        private DeleteGeoPointAsyncTask(GeoPointDAO geoPointDAO){
            this.geoPointDAO = geoPointDAO;
        }

        @Override
        protected Void doInBackground(GeoPoint... geoPoints) {
            geoPointDAO.delete(geoPoints[0]);
            return null;
        }
    }

    private static class DeleteAllGeoPointsAsyncTask extends AsyncTask<Void, Void, Void>{
        private GeoPointDAO geoPointDAO;

        private DeleteAllGeoPointsAsyncTask(GeoPointDAO geoPointDAO){
            this.geoPointDAO = geoPointDAO;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            geoPointDAO.deleteAllPoints();
            return null;
        }
    }
}
