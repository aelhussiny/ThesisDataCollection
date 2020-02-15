package edu.aucegypt.AEdatacollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Ahmed ElHussiny Data Collection Started", Toast.LENGTH_LONG).show();

        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            startSensor(context, context.getString(R.string.accelerometerID));
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            startSensor(context, context.getString(R.string.gyroscopeID));
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            startSensor(context, context.getString(R.string.lightSensorID));
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_STATIONARY_DETECT) != null) {
            startSensor(context, context.getString(R.string.stationarySensorID));
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null) {
            startSensor(context, context.getString(R.string.significantMotionSensorID));
        }
    }

    private void startSensor(Context context, String sensorID) {
        Intent broadcastIntent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("sensorID", sensorID);
        broadcastIntent.setAction("restartservice");
        broadcastIntent.putExtras(extras);
        broadcastIntent.setClass(context, Restarter.class);
        context.sendBroadcast(broadcastIntent);
    }
}

