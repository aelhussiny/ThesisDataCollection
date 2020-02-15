package edu.aucegypt.AEdatacollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

public class Restarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        String sensorID = extras.getString("sensorID", "");

        Class service = null;
        if (sensorID.equals(context.getString(R.string.accelerometerID))) {
            service = AccelerometerService.class;
        } else if (sensorID.equals(context.getString(R.string.gyroscopeID))) {
            service = GyroscopeService.class;
        } else if (sensorID.equals(context.getString(R.string.lightSensorID))) {
            service = LightService.class;
        } else if (sensorID.equals(context.getString(R.string.stationarySensorID))) {
            service = StationaryService.class;
        } else if (sensorID.equals(context.getString(R.string.significantMotionSensorID))) {
            service = SigMotionService.class;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, service));
        } else {
            context.startService(new Intent(context, service));
        }
    }
}

