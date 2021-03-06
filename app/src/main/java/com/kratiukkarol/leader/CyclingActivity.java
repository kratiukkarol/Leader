package com.kratiukkarol.leader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kratiukkarol.leader.model.GeoPoint;
import com.kratiukkarol.leader.viewModel.GeoPointViewModel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CyclingActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final String TAG = CyclingActivity.class.getSimpleName();
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final float DEFAULT_ZOOM = 15f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 32167;

    private boolean mLocationPermissionsGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private GoogleMap mGoogleMap;
    private LocationRequest locationRequest;
    private static GeoPoint currentGeoPoint;
    private List<LatLng> points;
    private GeoPointViewModel geoPointViewModel;
    private Chronometer chronometer;
    private long pauseOffset;
    private boolean isChronometerRunning;
    private static double totalDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycling);
        points = new ArrayList<>();
        geoPointViewModel = ViewModelProviders.of(this).get(GeoPointViewModel.class);

        TextView distanceTextView = findViewById(R.id.distanceCounter);
        String distanceToDisplay = Double.toString(totalDistance);
//        NumberFormat numberFormat = NumberFormat.getNumberInstance();
//        numberFormat.setMinimumFractionDigits(1);
//        numberFormat.setMaximumFractionDigits(3);
        distanceTextView.setText(distanceToDisplay);

        TextView tempoTextView = findViewById(R.id.tempoCounter);
        tempoTextView.setText("0 km/h");

        chronometer = findViewById(R.id.chronometer);

        getLocationPermission();
        if (mLocationPermissionsGranted) {
            initMap();
            addButtons();
            getInitialLocation();
            createLocationRequest();
        }
    }

    @Override
    protected void onResume() { super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    public void initMap() {
        Log.d(TAG, "initMap: initializing cyclingMap");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.cyclingMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
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
                startChronometer();
                startLocationUpdates();
                drawLine();
                Toast.makeText(this, "Started location tracking", Toast.LENGTH_SHORT).show();
                moveCamera(currentGeoPoint);
                break;
            case R.id.pause_button:
                pauseChronometer();
                Toast.makeText(this, "Tracking paused", Toast.LENGTH_SHORT).show();
                break;
            case R.id.stop_button:
                stopChronometer();
                Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show();
                mGoogleMap.clear();
                points.clear();
                geoPointViewModel.deleteAllGeoPoints();
                totalDistance = 0;
                break;
            case R.id.list_button:
                Intent geoPointsListIntent = new Intent(this, GeoPointsListActivity.class);
                startActivity(geoPointsListIntent);
                break;
        }
    }

    public void startChronometer(){
        if (!isChronometerRunning){
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            isChronometerRunning = true;
        }
    }

    public void pauseChronometer(){
        if (isChronometerRunning){
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            isChronometerRunning = false;
        }
    }

    public void stopChronometer(){
        if (isChronometerRunning){
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            pauseOffset = 0;
        }
    }

    public void getInitialLocation() {
        Log.d(TAG, "getDeviceLocation: getting the device current location");
        if (checkSelfPermission(FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Log.d(TAG, "onSuccess: current location");
                currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                moveCamera(currentGeoPoint);
                geoPointViewModel.insert(currentGeoPoint);
            } else {
                Log.d(TAG, "onSuccess: current location is null");
                Toast.makeText(CyclingActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveCamera(GeoPoint geoPoint) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + geoPoint.getLatitude() + ", lon: " + geoPoint.getLongitude());
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()), CyclingActivity.DEFAULT_ZOOM));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (checkSelfPermission(FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready");
    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionsGranted = true;
            Log.d(TAG, "getLocationPermission: permission granted");
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    protected void createLocationRequest() {
        Log.d(TAG, "createLocationRequest: initializing location request");

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(12000);
        locationRequest.setFastestInterval(6000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(5f);
    }

    public void startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d(TAG, "startLocationUpdates: start location updates");
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                Log.d(TAG, "onLocationResult: got location result");
                Location location = locationResult.getLastLocation();
                if (location != null){
                    currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    geoPointViewModel.insert(currentGeoPoint);
                    points.add(new LatLng(currentGeoPoint.getLatitude(), currentGeoPoint.getLongitude()));
                    calculateDistance();
                    Log.d(TAG, "Currently on geoPoints list are: " + points.size() + " geoPoints");
                    Toast.makeText(CyclingActivity.this, "Latitude: " + currentGeoPoint.getLatitude() + " Longitude: " + currentGeoPoint.getLongitude(), Toast.LENGTH_SHORT).show();
                }
            }
        }, Looper.myLooper());
    }

    private void calculateDistance() {
        double distance;
        if (points.size()<=1){
            return;
        }  else
            distance = getDistance(points.get(points.size()-2), points.get(points.size()-1));
            totalDistance += distance;
    }

//    public void getDistance(){
//        float[] results = new float[1];
//        Location.distanceBetween(points.get(points.size()-2).latitude, points.get(points.size()-2).longitude, points.get(points.size()-1).latitude, points.get(points.size()-1).longitude, results);
//    }

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

    public void drawLine() {

        PolylineOptions lineOptions = new PolylineOptions()
                .color(Color.GREEN)
                .width(5f)
                .addAll(points);

//        LiveData<List<GeoPoint>> pointsList =  geoPointViewModel.getAllGeoPoints();
//
//        for (GeoPoint geoPoint : points){
//            lineOptions.add(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
//        }

        Polyline line = mGoogleMap.addPolyline(lineOptions);
        line.setClickable(true);
    }
}
