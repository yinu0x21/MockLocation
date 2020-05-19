package com.yinu.mocklocation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import androidx.annotation.WorkerThread;

import io.ticofab.androidgpxparser.parser.GPXParser;
import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;

public class GpsChoreographer extends HandlerThread {

    private static final String TAG = "GpsChoreographer";

    private static final String INTENT_ACTION = "choreograph";

    private final Context mContext;
    private final OnLocationUpdateRequestListener mListener;
    private GPXParser mParser = new GPXParser();
    private Handler mHandler;

    public interface OnLocationUpdateRequestListener {
        void onRequest(Location location);
    }

    public GpsChoreographer(Context context, OnLocationUpdateRequestListener listener) {
        super(TAG);
        mContext = context;
        mListener = listener;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();

        mHandler = new Handler(getLooper());
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!INTENT_ACTION.equals(intent.getAction())) return;
                if (!intent.hasExtra("uri")) return;

                mHandler.post(() -> parse(intent.getStringExtra("uri")));
            }
        }, new IntentFilter(INTENT_ACTION));
    }

    @Override
    public boolean quitSafely() {
        mParser = null;
        return super.quitSafely();
    }

    @WorkerThread
    private void parse(String uriString) {
        mParser.parse(uriString, gpx -> {
            choreograph(gpx);
        });
    }

    @WorkerThread
    private void choreograph(Gpx gpx) {
        if (gpx == null) return;

        mHandler.post(() -> {
            doChoreograph(gpx);
        });
    }

    private void doChoreograph(Gpx gpx) {
        if (gpx.getTracks() == null || gpx.getTracks().isEmpty()) {
            return;
        }
        // For now we use only first track segment
        TrackSegment segment = gpx.getTracks().get(0).getTrackSegments().get(0);
        for (TrackPoint point : segment.getTrackPoints()) {
            updateLocation(point);
        }
    }

    private synchronized void updateLocation(TrackPoint p) {
        mListener.onRequest(makeLocation(p));
        try {
            wait(1000);
        } catch (InterruptedException e) {
            // nop
        }
    }

    private Location makeLocation(TrackPoint p) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(p.getLatitude());
        location.setLongitude(p.getLongitude());
        location.setTime(p.getTime() == null ?
                System.currentTimeMillis() :
                p.getTime().getMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        location.setAltitude(p.getElevation());
        location.setAccuracy(5.0f);
        return location;
    }
}
