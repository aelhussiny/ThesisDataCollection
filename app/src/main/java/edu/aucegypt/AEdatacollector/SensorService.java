package edu.aucegypt.AEdatacollector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class SensorService extends Service {

    private ArrayList<String> monitoredSensors;
    private SensorManager sensorManager = null;
    private SQLiteDatabase db = null;
    private MySensorListener listener = null;
    private HandlerThread mSensorThread = null;
    private Handler mSensorHandler = null;
    private boolean started = false;
    NotificationManager notificationManager;

    private static SensorService inst;

    public SensorService() {
        inst = this;
        monitoredSensors = new ArrayList<String>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!started) {
            started = true;
            notificationManager = getSystemService(NotificationManager.class);
            db = (new DatabaseHelper(SensorService.this)).getWritableDatabase();
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            listener = new MySensorListener(db);
            startForeground(1, buildNotification(getString(R.string.startingMonitoring)));

            mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
            mSensorThread.start();
            mSensorHandler = new Handler(mSensorThread.getLooper());

            if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST, mSensorHandler);
                monitoredSensors.add(getString(R.string.accelerometerID));
            }

            if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
                sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST, mSensorHandler);
                monitoredSensors.add(getString(R.string.gyroscopeID));
            }

            if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
                sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_FASTEST, mSensorHandler);
                monitoredSensors.add(getString(R.string.lightSensorID));
            }

            notificationManager.notify(1, buildNotification(getString(R.string.monitoring)));
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public ArrayList<String> getMonitoredSensors() {
        return this.monitoredSensors;
    }

    public static SensorService getInstance() {
        return inst;
    }

    private Notification buildNotification(String message) {
        Notification notification;
        Intent notificationIntent = new Intent(SensorService.this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(SensorService.this, 0,
                notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("edu.aucegypt.AEdatacollector.notificationChannel1", "Monitoring Notification", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Receive notification for sensor monitoring");
            notificationManager.createNotificationChannel(notificationChannel);

            Notification.Builder builder = new Notification.Builder(this, notificationChannel.getId())
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setAutoCancel(true)
                    .setContentIntent(intent);

            notification = builder.build();
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(intent);

            notification = builder.build();
        }
        return notification;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        inst = null;
        mSensorThread.quitSafely();
        sensorManager.unregisterListener(listener);
        db.close();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }

    private class MySensorListener implements SensorEventListener {

        SQLiteDatabase db;
        HashMap<Integer, String> sensorIDs;
        SharedPreferences sharedPreferences;
        SharedPreferences.Editor sharedPrefEditor;

        public MySensorListener (SQLiteDatabase db) {
            this.db = db;
            sensorIDs = new HashMap<Integer, String>();
            sensorIDs.put(Sensor.TYPE_ACCELEROMETER, getString(R.string.accelerometerID));
            sensorIDs.put(Sensor.TYPE_GYROSCOPE, getString(R.string.gyroscopeID));
            sensorIDs.put(Sensor.TYPE_LIGHT, getString(R.string.lightSensorID));
            sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferencesFile), Context.MODE_PRIVATE);
            sharedPrefEditor = sharedPreferences.edit();
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            String lastSensorData = sharedPreferences.getString("lastReadingFor" + sensorIDs.get(event.sensor.getType()), "");
            lastSensorData = lastSensorData.substring(lastSensorData.indexOf("_") + 1);
            String sensorValues = join(event.values);
            if (!sensorValues.equals(lastSensorData)) {
                ContentValues values = new ContentValues();
                values.put(SensorDataContract.SensorData.COLUMN_NAME_SENSOR, sensorIDs.get(event.sensor.getType()));
                values.put(SensorDataContract.SensorData.COLUMN_NAME_VALUE, sensorValues);
                long time = (new Date()).getTime();
                values.put(SensorDataContract.SensorData.COLUMN_NAME_TIME, time);
                try {
                    db.insert(SensorDataContract.SensorData.TABLE_NAME, null, values);
                    sharedPrefEditor.putString("lastReadingFor" + sensorIDs.get(event.sensor.getType()), time + "_" + sensorValues).commit();
                } catch (Exception e) {
                    Log.e("AhmedTest", "Error inserting sensor " + sensorIDs.get(event.sensor.getType()) + " data into database", e);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        private String join(float[] values) {
            String joinedValues = "";
            for(float value: values) {
                joinedValues += value + ",";
            }

            return joinedValues.substring(0, joinedValues.length()-2);
        }
    }
}
