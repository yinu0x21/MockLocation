package com.yinu.mocklocation;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import static android.location.LocationManager.GPS_PROVIDER;

public class LocationService extends Service {

    private static final String TAG = LocationService.class.getSimpleName();

    private static final String CHANNEL_NAME = "test_ch";
    private static final String CHANNEL_ID = "test_id";
    private static final String DESCRIPTION = "This is test";

    private static final String INTENT_ACTION = "inject_location";

    private LocationManager mLocationManager;

    @Override
    public void onCreate() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(GPS_PROVIDER, 0, 0f, mLocationListener);
//        mLocationManager.requestLocationUpdates(NETWORK_PROVIDER, 0, 0, this);

        enableProvider();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(DESCRIPTION);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Notification notification = builder.build();

        startForeground(1, notification);

        enableProvider();
        registerReceiver(mInjectionReceiver, new IntentFilter(INTENT_ACTION));

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        removeProvider();
    }

    /**
     * Location injection receiver
     *
     * adb shell am broadcast -a inject_location --esa latlon "48.873792, 2.295028"
     */
    private BroadcastReceiver mInjectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!INTENT_ACTION.equals(intent.getAction())) {
                return;
            }

            Bundle extras = intent.getExtras();
            String[] latlon = extras != null ? extras.getStringArray("latlon") : new String[0];
            if (latlon.length == 2) {
                updateLocation(Double.parseDouble(latlon[0]), Double.parseDouble(latlon[1]));
            }
        }
    };

    /**
     * Listener of LocationManagerService
     */
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged " + location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private void enableProvider() {
        LocationProvider provider = mLocationManager.getProvider(GPS_PROVIDER);
        Log.d(TAG, "LocationProvider " + provider);

        try {
            mLocationManager.addTestProvider(
                    GPS_PROVIDER,
                    true, false, false, false,
                    true, true, true,
                    0, 5
            );
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
        }
        try {
            mLocationManager.setTestProviderEnabled(GPS_PROVIDER, true);
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
        }

    }

    private void removeProvider() {
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager.setTestProviderEnabled(GPS_PROVIDER, false);
        mLocationManager.removeTestProvider(GPS_PROVIDER);
    }

    private void updateLocation(double latitude, double longitude) {
        Location location = makeLocation(latitude, longitude);
        Log.d(TAG, "updateLocation " + location);
        mLocationManager.setTestProviderLocation(GPS_PROVIDER, location);
    }

    private Location makeLocation(double latitude, double longitude) {
        Location location = new Location(GPS_PROVIDER);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        location.setAltitude(0.0);
        location.setAccuracy(5f);
        return location;
    }
}