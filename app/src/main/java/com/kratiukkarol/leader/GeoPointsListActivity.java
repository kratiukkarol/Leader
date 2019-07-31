package com.kratiukkarol.leader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.kratiukkarol.leader.model.GeoPoint;
import com.kratiukkarol.leader.viewModel.GeoPointViewModel;

import java.util.List;

public class GeoPointsListActivity extends AppCompatActivity {

    private GeoPointViewModel geoPointViewModel;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;
    private GeoPointAdapter geoPointAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_points_list);

        linearLayoutManager = new LinearLayoutManager(this);
        geoPointAdapter = new GeoPointAdapter();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(geoPointAdapter);

        geoPointViewModel = ViewModelProviders.of(this).get(GeoPointViewModel.class);
        geoPointViewModel.getAllGeoPoints().observe(this, geoPoints -> geoPointAdapter.setGeoPoints(geoPoints));
    }
}
