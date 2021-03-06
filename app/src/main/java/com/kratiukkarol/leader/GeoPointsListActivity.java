package com.kratiukkarol.leader;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kratiukkarol.leader.viewModel.GeoPointViewModel;

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
