package edu.aucegypt.AEdatacollector;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

public class Restarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Received Restart Signal", Toast.LENGTH_LONG).show();
        if(!serviceIsRunning(context, SensorService.class)) {
            Toast.makeText(context, "Service isn't running", Toast.LENGTH_LONG).show();
            Intent sensorServiceIntent = new Intent(context, SensorService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(sensorServiceIntent);
            } else {
                context.startService(sensorServiceIntent);
            }
            Toast.makeText(context, "Service Started", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Service Started", Toast.LENGTH_LONG).show();
        }
    }

    private boolean serviceIsRunning(Context context, Class<?> serviceClass) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException npe) {
            Toast.makeText(context, "Couldn't get running services", Toast.LENGTH_LONG).show();
            return false;
        }
    }
}

