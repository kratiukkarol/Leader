package com.kratiukkarol.leader;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kratiukkarol.leader.database.GeoPointsDatabase;
import com.kratiukkarol.leader.services.LocationService;
import com.kratiukkarol.leader.viewModel.GeoPointViewModel;

import java.text.DecimalFormat;

public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final String TAG = RunningActivity.class.getSimpleName();
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final DecimalFormat distanceFormat = new DecimalFormat("##0.000");

    private boolean mLocationPermissionsGranted = false;
    private static GoogleMap mMap;
    private static Polyline line;
    private GeoPointViewModel geoPointViewModel;
    private Intent locationIntent;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    TextView distanceTextView;
    TextView tempoTextView;
    private Chronometer chronometer;
    private long pauseOffset;
    private boolean isChronometerRunning;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: called.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);
        geoPointViewModel = ViewModelProviders.of(this).get(GeoPointViewModel.class);
        preferences = getSharedPreferences("preferences", MODE_PRIVATE);
        distanceTextView = findViewById(R.id.distanceCounter);
        chronometer = findViewById(R.id.chronometer);
        tempoTextView = findViewById(R.id.tempoCounter);

        addButtons();
        getLocationPermission();
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mLocationPermissionsGranted = true;
            initMap();
        } else {
            ActivityCompat.requestPermissions(this,permissions,LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called");
        mLocationPermissionsGranted = false;

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "onRequestPermissionsResult: permission failed");
                        return;
                    }
                }
                mLocationPermissionsGranted = true;
                Log.d(TAG, "onRequestPermissionsResult: permission granted");
                initMap();
            }
        }
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.runningMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(RunningActivity.this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;
        if (mLocationPermissionsGranted && !isLocationServiceStarted()) {
            getInitialLocation();
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (isLocationServiceStarted()){
            Log.d(TAG, "onStart: drawing last lines.");
            drawLastLines();
        }
        mMap.setMyLocationEnabled(true);
    }

    private void getInitialLocation() {
        Log.d(TAG, "getInitialLocation: getting the device initial location");
        try {
            if (mLocationPermissionsGranted) {
                FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if(location!=null){
                        Log.d(TAG,"onSuccess: current location");
                        moveCamera(new LatLng(location.getLatitude(), location.getLongitude()));
                    } else {
                        Log.d(TAG, "onSuccess: current location is null");
                        Toast.makeText(RunningActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (SecurityException exc) {
            Log.d(TAG, "getDeviceLocation: SecurityException: " + exc.getMessage());
        }
    }

    private void moveCamera(LatLng latLng) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lon: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, RunningActivity.DEFAULT_ZOOM));
    }

    public void addButtons() {
        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(this);
        Button pauseButton = findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(this);
        Button stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(this);
        Button listButton = findViewById(R.id.list_button);
        listButton.setOnClickListener(this);
        Log.d(TAG, "addButtons: buttons added.");
    }

    public void initializeLocationService(){
        startService(locationIntent);
    }

    public void initializeMapWithService(){
        actualizeMap();
        startChronometer();
        Toast.makeText(this, "Workout started.", Toast.LENGTH_SHORT).show();
        // count currentTempo
        // count averageTempo
    }

    @Override
    public void onClick(View view) {
        locationIntent = new Intent(getApplicationContext(), LocationService.class);
        switch (view.getId()) {
            case R.id.start_button:
                Log.d(TAG, "onClick: start_button clicked.");
                if (!isLocationServiceStarted()) {
                    initializeLocationService();
                    if (LocationService.isInitialized()){
                        initializeMapWithService();
                    } else {
                        startChronometer();
                    }
                } else {
                    Toast.makeText(this, "Workout is already started.", Toast.LENGTH_SHORT).show();
                }
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                        RunningActivity.this.startForegroundService(startRunningIntent);
//                        Toast.makeText(this, "Workout started.", Toast.LENGTH_SHORT).show();
//                    } else {
//                        startService(startRunningIntent);
//                        Toast.makeText(this, "Workout started.", Toast.LENGTH_SHORT).show();
//                    }
//                }
                break;
            case R.id.pause_button:
                Log.d(TAG, "onClick: pause_button clicked.");
                if (isLocationServiceStarted()){
                    stopService(locationIntent);
                    pauseChronometer();
                    Toast.makeText(this, "Workout paused", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Workout is already paused.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.stop_button:
                Log.d(TAG, "onClick: stop_button clicked.");
                if (isLocationServiceStarted()){
                    stopService(locationIntent);
                    stopChronometer();
                    actualizeDistanceCounter();
                    actualizeTempoCounter();
                    mMap.clear();
                    Toast.makeText(this, "Workout stopped", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Workout is not running.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.list_button:
                Intent geoPointsListIntent = new Intent(this, GeoPointsListActivity.class);
                startActivity(geoPointsListIntent);
                break;
        }
    }

    private void actualizeMap() {
        geoPointViewModel.getAllGeoPoints().observe(this, pointList -> {
            Log.d(TAG, "actualizeMap: actualizing map...");
            if (isLocationServiceStarted()){
                //drawCurrentLine();
                actualizeDistanceCounter();
                actualizeTempoCounter();
            } else {
                Log.d(TAG, "actualizeMap: Nothing to actualize...");
            }
        });
    }

    private void actualizeDistanceCounter(){
        distanceTextView = findViewById(R.id.distanceCounter);
        double totalDistance = LocationService.getTotalDistance();
        String distanceToDisplay = distanceFormat.format(totalDistance);
        distanceTextView.setText(distanceToDisplay + " km");
        Log.d(TAG, "actualizeDistanceCounter: DistanceCounter actualized: " + distanceToDisplay);
    }

    public void actualizeTempoCounter(){
        tempoTextView = findViewById(R.id.tempoCounter);
        tempoTextView.setText("0 km/h");
        Log.d(TAG, "actualizeTempoCounter: TempoCounter actualized.");
    }

    public void actualizeChronometer(){
        isChronometerRunning = preferences.getBoolean("isChronometerRunning", false);
        if (isChronometerRunning){
            pauseOffset = preferences.getLong("chronometerPauseOffset", 0);
            chronometer.setBase(preferences.getLong("chronometerBaseTime", SystemClock.elapsedRealtime()));
            chronometer.start();
        } else {
            pauseOffset = preferences.getLong("chronometerPauseOffset", 0);
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
        }
    }

    private boolean isLocationServiceStarted(){
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)){
            if ("com.kratiukkarol.leader.services.LocationService".equals(serviceInfo.service.getClassName())){
                Log.d(TAG, "isLocationServiceStarted: location service is already started.");
                return  true;
            }
        }
        Log.d(TAG, "isLocationServiceStarted: location service is not started.");
        return false;
    }

    public void startChronometer(){
        Log.d(TAG, "startChronometer: chronometer started.");
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            isChronometerRunning = true;
    }

    public void pauseChronometer(){
        Log.d(TAG, "pauseChronometer: chronometer paused.");
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            isChronometerRunning = false;
    }

    public void stopChronometer(){
        Log.d(TAG, "stopChronometer: chronometer stopped.");
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            pauseOffset = 0;
            isChronometerRunning = false;
            editor = preferences.edit();
            editor.putLong("chronometerPauseOffset", pauseOffset);
            editor.putLong("chronometerBaseTime", chronometer.getBase());
            editor.apply();
    }

    @Override
    protected void onStart() {

        Log.d(TAG, "onStart: called.");
        super.onStart();
        actualizeDistanceCounter();
        actualizeChronometer();
        actualizeTempoCounter();
        actualizeMap();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called.");
        super.onStop();
        editor = preferences.edit();
        editor.putBoolean("isChronometerRunning", isChronometerRunning);
        editor.putLong("chronometerPauseOffset", pauseOffset);
        editor.putLong("chronometerBaseTime", chronometer.getBase());
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: called.");
        geoPointViewModel.getAllGeoPoints().removeObservers(this);
        GeoPointsDatabase.destroyInstance();
    }

    public void drawLastLines() {
        if (LocationService.getPointsList().size() >= 2){
            Log.d(TAG, "drawLastLines: drawing last lines...");
            PolylineOptions lastLines = new PolylineOptions()
                    .color(Color.GREEN)
                    .width(5f)
                    .addAll(LocationService.getPointsList());

            line = mMap.addPolyline(lastLines);
            line.setClickable(true);
        } else {
            Log.d(TAG, "drawLastLines: The points list does not contain two elements.");
        }
    }

    public static void drawCurrentLine(){
        if (LocationService.getPointsList().size() >= 2){
            Log.d(TAG, "drawCurrentLine: drawing current line...");
            Log.d(TAG, "drawCurrentLine: Points list size: " + LocationService.getPointsList().size());
            PolylineOptions latestLine = new PolylineOptions()
                    .color(Color.BLUE)
                    .width(5f)
                    .add(LocationService.getPointsList().get(LocationService.getPointsList().size() - 2),
                            LocationService.getPointsList().get(LocationService.getPointsList().size() - 1));

            line = mMap.addPolyline(latestLine);
            Log.d(TAG, "drawCurrentLine: The line has been drawn on the map.");
            line.setClickable(true);
        } else {
            Log.d(TAG, "drawCurrentLine: The points list does not contain two elements.");
        }
    }
}
