package com.kratiukkarol.leader;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.kratiukkarol.leader.services.RunningService;

public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final String TAG = RunningActivity.class.getSimpleName();
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    private boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);
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
        if (mLocationPermissionsGranted) {
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
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_button:
                if (!isRunningServiceStarted()) {
                    Intent startRunningIntent = new Intent(getApplicationContext(), RunningService.class);
                    startService(startRunningIntent);
                    Toast.makeText(this, "Workout started.", Toast.LENGTH_SHORT).show();
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
                    Intent pauseRunningIntent = new Intent(getApplicationContext(), RunningService.class);
                    stopService(pauseRunningIntent);
                    Toast.makeText(this, "Workout paused", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Workout is already paused.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.stop_button:
                if (isRunningServiceStarted()){
                    Intent stopRunningIntent = new Intent(getApplicationContext(), RunningService.class);
                    stopService(stopRunningIntent);
                    Toast.makeText(this, "Workout stopped", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Workout is already stopped.", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.list_button:
                //Intent geoPointsListIntent = new Intent(this, GeoPointsListActivity.class);
                //startActivity(geoPointsListIntent);
                break;
        }
    }

    private boolean isRunningServiceStarted(){
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)){
            if ("com.kratiukkarol.leader.services.RunningService".equals(serviceInfo.service.getClassName())){
                Log.d(TAG, "isRunningServiceStarted: running service is already started.");
                return  true;
            }
        }
        Log.d(TAG, "isRunningServiceStarted: running service is not running.");
        return false;
    }

    public void addCounters(){
        TextView distanceTextView = findViewById(R.id.distanceCounter);
        distanceTextView.setText("0.00");
        //String distanceToDisplay = Double.toString(totalDistance);
//        NumberFormat numberFormat = NumberFormat.getNumberInstance();
//        numberFormat.setMinimumFractionDigits(1);
//        numberFormat.setMaximumFractionDigits(3);
        //distanceTextView.setText(distanceToDisplay);

        TextView tempoTextView = findViewById(R.id.tempoCounter);
        tempoTextView.setText("0 km/h");

        //chronometer = findViewById(R.id.chronometer);
    }
}
