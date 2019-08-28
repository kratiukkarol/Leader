package com.kratiukkarol.leader.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Objects;

public class RunningService extends Service {

   private static final String TAG = "LocationService";

   private final static long UPDATE_INTERVAL = 6000;
   private final static long FASTEST_INTERVAL = 3000;
   //private final static float SMALLEST_DISPLACEMENT = 5f;

    private FusedLocationProviderClient mfusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location mLocation;

    public RunningService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mfusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG, "onLocationResult: got location result.");
                mLocation = locationResult.getLastLocation();
                Toast.makeText(RunningService.this, "Latitude: " + mLocation.getLatitude() + " Longitude: " + mLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            }
        };
        createLocationRequest();

        if (Build.VERSION.SDK_INT >= 26){
            String CHANNEL_ID = "location_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Location Channel", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) Objects.requireNonNull(getSystemService(Context.NOTIFICATION_SERVICE))).createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();
            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mfusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: called.");
        getWorkoutUpdates();
        return START_NOT_STICKY;
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        //locationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT);
    }

    private void getWorkoutUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "getLocationUpdates: stopping the location service.");
            stopSelf();
            return;
        }
        Log.d(TAG, "getLocationUpdates: getting location information.");
        mfusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                Log.d(TAG, "onLocationResult: got location result.");
//                Location location = locationResult.getLastLocation();
//                if (location != null){
                    // currentGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    // add to database
                    // add to list or replace with database only
                    // draw line
                    // count distance
                    // count currentTempo
                    // count averageTempo
                    // manage chronometer

//                    geoPointViewModel.insert(currentGeoPoint);
//                    points.add(new LatLng(currentGeoPoint.getLatitude(), currentGeoPoint.getLongitude()));
//                    calculateDistance();
//                    Log.d(TAG, "Currently on geoPoints list are: " + points.size() + " geoPoints");
//                    Toast.makeText(RunningService.this, "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude(), Toast.LENGTH_SHORT).show();
//                }

    }
}
