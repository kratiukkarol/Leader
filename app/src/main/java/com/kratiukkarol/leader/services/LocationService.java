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
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.kratiukkarol.leader.RunningActivity;
import com.kratiukkarol.leader.model.GeoPoint;
import com.kratiukkarol.leader.repository.GeoPointRepository;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LocationService extends Service {

   private static final String TAG = "LocationService";

   private final static long UPDATE_INTERVAL = 6000;
   private final static long FASTEST_INTERVAL = 3000;
   private final static float SMALLEST_DISPLACEMENT = 5f;

    private FusedLocationProviderClient mfusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location mLocation;
    private GeoPoint currentGeoPoint;
    private GeoPointRepository geoPointRepository;
    private static List<LatLng> pointsList;
    private static double totalDistance;
    private static LatLng latestGeoPoint;
    private static boolean isInitialized;

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    public LocationService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: called. ");
        geoPointRepository = new GeoPointRepository(getApplication());
        pointsList = new ArrayList<>();
        //geoPointServiceViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()).create(GeoPointViewModel.class);
        mfusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG, "onLocationResult: got location result.");
                mLocation = locationResult.getLastLocation();
                if (mLocation != null){
                    currentGeoPoint = new GeoPoint(mLocation.getLatitude(), mLocation.getLongitude());
                    geoPointRepository.insert(currentGeoPoint);
                    Log.d(TAG, "onLocationResult: GeoPoint inserted to the database.");
                    latestGeoPoint = new LatLng(currentGeoPoint.getLatitude(), currentGeoPoint.getLongitude());
                    pointsList.add(latestGeoPoint);
                    Log.d(TAG, "onLocationResult: Latest LatLng geoPoint added to the list.");
                    RunningActivity.drawCurrentLine();
                    calculateDistance();
                    Log.d(TAG, "onLocationResult: distance calculated...");
                }
            }
        };
        setInitialized(true);
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
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        mfusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        pointsList.removeAll(pointsList);
        geoPointRepository.deleteAllGeoPoints();
        totalDistance = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: called.");
        totalDistance = 0;
        //collectLastGeoPoints();
        getLocationUpdates();
        return START_NOT_STICKY;
    }

    private void createLocationRequest(){
        Log.d(TAG, "createLocationRequest: creating request.");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT);
    }

    private void getLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "getLocationUpdates: stopping the location service.");
            stopSelf();
            return;
        }
        Log.d(TAG, "getLocationUpdates: getting location information.");
        mfusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        //collectLatestGeoPoint();
        //calculateDistance();
    }

//    public void collectLastGeoPoints() {
//        List<GeoPoint> geoPointList = geoPointRepository.getAllGeoPoints().getValue();
//        Log.d(TAG, "drawLastLines: geoPointList size: " + (geoPointList != null ? geoPointList.size() : 0));
//        if (geoPointList != null) {
//            for (GeoPoint geoPoint : geoPointList){
//                pointsList.add(new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()));
//                calculateDistance();
//            }
//        }
//    }
//
//    public void collectLatestGeoPoint(){
//        currentGeoPoint = geoPointRepository.getLatestGeoPoint().getValue();
//        if (currentGeoPoint != null) {
//            Log.d(TAG, "collectLatestGeoPoint: collected latest GeoPoint: " + currentGeoPoint.getLatitude() +", " + currentGeoPoint.getLongitude());
//            latestGeoPoint = new LatLng(currentGeoPoint.getLatitude(), currentGeoPoint.getLongitude());
//            pointsList.add(latestGeoPoint);
//        }
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
        double km = valueResult/1;
        DecimalFormat newFormat = new DecimalFormat("##0.000");
        String kmInDec = newFormat.format(km);
        double meter = valueResult%1000;
        String meterInDec = newFormat.format(meter);
        Log.i("Radius Value",""+valueResult+"   KM  "+kmInDec+" Meter   "+meterInDec);

        return radius*c;
    }

    private void calculateDistance() {
        double distance;
        if (pointsList.size() >= 2) {
            distance = getDistance(pointsList.get(pointsList.size() - 2), pointsList.get(pointsList.size() - 1));
            totalDistance += distance;
        }
    }

//    public static LatLng[] getLatestTwoPoints(){
//        LatLng[] pointsToDrawLine = new LatLng[2];
//        if (pointsList.size() >= 2) {
//            pointsToDrawLine[0] = pointsList.get(pointsList.size() - 2);
//            pointsToDrawLine[1] = pointsList.get(pointsList.size() - 1);
//        }
//        return pointsToDrawLine;
//    }

    public static List<LatLng> getPointsList() {
        return pointsList;
    }

    public static double getTotalDistance() {
        return totalDistance;
    }

//    public static LatLng getLatestGeoPoint() {
//        return latestGeoPoint;
//    }
}
