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
import com.kratiukkarol.leader.model.GeoPoint;
import com.kratiukkarol.leader.services.LocationService;
import com.kratiukkarol.leader.viewModel.GeoPointViewModel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final String TAG = RunningActivity.class.getSimpleName();
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    private boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private GeoPointViewModel geoPointViewModel;
    private static List<LatLng> pointsList = new ArrayList<>();
    private static double totalDistance;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
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
        chronometer = findViewById(R.id.chronometer);

        getLocationPermission();
        addButtons();
        addCounters();
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
        if (isRunningServiceStarted()){
            startDrawingLines();
        } else if (mLocationPermissionsGranted) {
            getInitialLocation();
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
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
        Log.i(TAG, "addButtons: buttons added.");
    }

    @Override
    public void onClick(View view) {
        Intent locationIntent = new Intent(getApplicationContext(), LocationService.class);
        switch (view.getId()) {
            case R.id.start_button:
                if (!isRunningServiceStarted()) {
                    startService(locationIntent);
                    startDrawingLines();
                    startChronometer();
                    Toast.makeText(this, "Workout started.", Toast.LENGTH_SHORT).show();
                    // add to list or replace with database only
                    // draw line
                    // count distance
                    // count currentTempo
                    // count averageTempo
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
                if (isRunningServiceStarted()){
                    stopService(locationIntent);
                    pauseChronometer();
                    Toast.makeText(this, "Workout paused", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Workout is already paused.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.stop_button:
                if (isRunningServiceStarted()){
                    stopService(locationIntent);
                    stopChronometer();
                    mMap.clear();
                    pointsList.removeAll(pointsList);
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

    private void startDrawingLines() {
        geoPointViewModel.getAllGeoPoints().observe(this, (List<GeoPoint> pointList) -> drawLine());
    }

    private boolean isRunningServiceStarted(){
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)){
            if ("com.kratiukkarol.leader.services.LocationService".equals(serviceInfo.service.getClassName())){
                Log.d(TAG, "isRunningServiceStarted: running service is already started.");
                return  true;
            }
        }
        Log.d(TAG, "isRunningServiceStarted: running service is not running.");
        return false;
    }

    public void addCounters(){
        TextView distanceTextView = findViewById(R.id.distanceCounter);
        String distanceCounter = Double.toString(totalDistance);
        distanceTextView.setText(distanceCounter);
        //String distanceToDisplay = Double.toString(totalDistance);
//        NumberFormat numberFormat = NumberFormat.getNumberInstance();
//        numberFormat.setMinimumFractionDigits(1);
//        numberFormat.setMaximumFractionDigits(3);
        //distanceTextView.setText(distanceToDisplay);

        TextView tempoTextView = findViewById(R.id.tempoCounter);
        tempoTextView.setText("0 km/h");
        Log.i(TAG, "addCounters: counters added.");
    }

    public void startChronometer(){
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            isChronometerRunning = true;
    }

    public void pauseChronometer(){
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            isChronometerRunning = false;
    }

    public void stopChronometer(){
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
        super.onStart();
        isChronometerRunning = preferences.getBoolean("isChronometerRunning", false);
        if (isChronometerRunning){
            pauseOffset = preferences.getLong("chronometerPauseOffset", 0);
            chronometer.setBase(preferences.getLong("chronometerBaseTime", SystemClock.elapsedRealtime()));
            chronometer.start();
        } else{
            pauseOffset = preferences.getLong("chronometerPauseOffset", 0);
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
        }
    }

    @Override
    protected void onStop() {
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
        GeoPointsDatabase.destroyInstance();
    }

    public void drawLine() {
        List<GeoPoint> geoPointList = geoPointViewModel.getAllGeoPoints().getValue();
        //LatLng newPoint = null;
        Log.d(TAG, "drawLine: geoPointList size: " + geoPointList.size());
        if (geoPointList != null) {
            totalDistance = 0;
            for (GeoPoint geoPoint : geoPointList){
                pointsList.add(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
                calculateDistance();
                Log.d(TAG, "drawLine: calculated distance: " + totalDistance);
            }
        }

        PolylineOptions lineOptions = new PolylineOptions()
                .color(Color.GREEN)
                .width(5f)
                .addAll(pointsList);

        Polyline line = mMap.addPolyline(lineOptions);
        line.setClickable(true);
    }

    public double getDistance(LatLng startPoint, LatLng endPoint) {
        final int radius = 6371; //radius of earth in Km
        double lat1 = startPoint.latitude;
        double lat2 = endPoint.latitude;
        double lon1 = startPoint.longitude;
        double lon2 = endPoint.longitude;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = radius * c;
        double km=valueResult/1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec =  Integer.valueOf(newFormat.format(km));
        double meter=valueResult%1000;
        int  meterInDec= Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value",""+valueResult+"   KM  "+kmInDec+" Meter   "+meterInDec);

        return radius*c;
    }

    private void calculateDistance() {
        double distance;
        if (pointsList.size()<=1){
            return;
        }  else {
            distance = getDistance(pointsList.get(pointsList.size() - 2), pointsList.get(pointsList.size() - 1));
            totalDistance += distance;
        }
    }
}
