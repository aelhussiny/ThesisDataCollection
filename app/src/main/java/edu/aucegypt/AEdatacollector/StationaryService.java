package edu.aucegypt.AEdatacollector;

import android.app.IntentService;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Date;

public class StationaryService extends IntentService implements SensorEventListener {
    private SQLiteDatabase db = null;
    private SensorManager sensorManager = null;
    private Sensor sensor = null;
    private boolean sensorRegistered = false;

    private static StationaryService instance = null;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    public StationaryService() {
        super("Stationary Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = (new DatabaseHelper(this)).getWritableDatabase();
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STATIONARY_DETECT);
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        instance = null;
        sensorManager.unregisterListener(this);
        db.close();
        Intent broadcastIntent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("sensorID", getSensorID());
        broadcastIntent.setAction("restartservice");
        broadcastIntent.putExtras(extras);
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!sensorRegistered) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
            sensorRegistered = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (sensorManager == null || sensor == null) {
            this.onCreate();
        }
        if (!sensorRegistered) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
            sensorRegistered = true;
        }
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    public String getSensorID() {
        return getString(R.string.stationarySensorID);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        ContentValues values = new ContentValues();
        values.put(SensorDataContract.SensorData.COLUMN_NAME_SENSOR, getSensorID());
        values.put(SensorDataContract.SensorData.COLUMN_NAME_VALUE, event.values[0]);
        values.put(SensorDataContract.SensorData.COLUMN_NAME_TIME, sensorTimeStampToTime(event.timestamp));
        try {
            db.insert(SensorDataContract.SensorData.TABLE_NAME, null, values);
        } catch (Exception e) {
            Log.e("AhmedTest", "Error inserting sensor " + getSensorID() + " data into database", e);
        }
    }

    private long sensorTimeStampToTime(long sensorTimestamp) {
        return (new Date()).getTime() + (sensorTimestamp - System.nanoTime()) / 1000000L;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
