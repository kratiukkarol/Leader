package com.kratiukkarol.leader.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.kratiukkarol.leader.model.GeoPoint;
import com.kratiukkarol.leader.repository.GeoPointRepository;

import java.util.List;

public class GeoPointViewModel extends AndroidViewModel {
    private GeoPointRepository repository;
    private LiveData<List<GeoPoint>> allGeoPoints;

    public GeoPointViewModel(@NonNull Application application) {
        super(application);
        repository = new GeoPointRepository(application);
        allGeoPoints = repository.getAllGeoPoints();
    }

    public void insert(GeoPoint geoPoint){
        repository.insert(geoPoint);
    }

    public void update(GeoPoint geoPoint){
        repository.update(geoPoint);
    }

    public void delete(GeoPoint geoPoint){
        repository.delete(geoPoint);
    }

    public void deleteAllGeoPoints(GeoPoint geoPoint){
        repository.deleteAllGeoPoints();
    }

    public LiveData<List<GeoPoint>> getAllGeoPoints(){
        return allGeoPoints;
    }
}
