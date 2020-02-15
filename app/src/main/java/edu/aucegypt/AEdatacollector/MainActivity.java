package edu.aucegypt.AEdatacollector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("AhmedTest", "OnCreate");
        super.onCreate(null);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CreationAsyncTask creationTask = new CreationAsyncTask();

        SQLiteDatabase db = (new DatabaseHelper(getApplicationContext())).getReadableDatabase();
        String[] columns = {UserDataContract.UserData.COLUMN_NAME_KEY};
        Cursor cursor = db.query(UserDataContract.UserData.TABLE_NAME, columns, null, null, null, null, null);
        HashSet<String> requiredKeys = new HashSet<String>(Arrays.asList("name", "consentDate", "signature", "gender", "ageGroup"));
        HashSet<String> foundKeys = new HashSet<String>();
        while(cursor.moveToNext()) {
            foundKeys.add(cursor.getString(cursor.getColumnIndex(UserDataContract.UserData.COLUMN_NAME_KEY)));
        }
        cursor.close();
        db.close();

        if(!requiredKeys.equals(foundKeys)) {
            backToRegistration();
        } else {
            creationTask.execute("");
            findViewById(R.id.share_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        verifyStoragePermissions(MainActivity.this);
                        if(PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_STREAM, getDBCopyUri());
                            shareIntent.setType("application/x-sqlite3");
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(shareIntent, "Share Sensor Data"));
                        }
                    } catch (Exception e) {
                        Log.e("AhmedTest", "Error:", e);
                        Toast.makeText(getApplicationContext(), "Error encountered", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

    }

    public static void verifyStoragePermissions(Activity activity) {
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    1
            );
        }
    }

    private Uri getDBCopyUri() throws Exception {
        File file = getApplicationContext().getDatabasePath(DatabaseHelper.DATABASE_NAME);

        if(file.exists() && file.canRead())
        {
            File destination = new File(Environment.getExternalStorageDirectory() + "/" + getPackageName(), "database_copy.db");
            if (!destination.exists()) {
                destination.getParentFile().mkdirs();
                destination.createNewFile();
            }
            copy(file, destination);
            return Uri.fromFile(destination);
        } else {
            throw new Exception("File inaccessible");
        }
    }

    private void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst, false);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void backToRegistration() {
        startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        finish();
    }

    private void updateTimeForSensor(HashMap<String, Long> lastAt) {
        TextView latestCollectionTextView = null;

        Iterator it = lastAt.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> pair = (Map.Entry<String, Long>)it.next();
            String sensorID = pair.getKey();
            long latestTime = pair.getValue();
            if (sensorID.equals(getString(R.string.accelerometerID))) {
                latestCollectionTextView = (TextView) findViewById(R.id.accelerometerLastCollection);
            } else if (sensorID.equals(getString(R.string.gyroscopeID))) {
                latestCollectionTextView = (TextView) findViewById(R.id.gyroscopeLastCollection);
            } else if (sensorID.equals(getString(R.string.lightSensorID))) {
                latestCollectionTextView = (TextView) findViewById(R.id.lightLastCollection);
            } else if (sensorID.equals(getString(R.string.stationarySensorID))) {
                latestCollectionTextView = (TextView) findViewById(R.id.stationaryLastCollection);
            } else if (sensorID.equals(getString(R.string.significantMotionSensorID))) {
                latestCollectionTextView = (TextView) findViewById(R.id.sigMotionLastCollection);
            }

            if (latestCollectionTextView != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss.SSSS Z");
                latestCollectionTextView.setText(sdf.format(new Date(latestTime)));
            }
        }
    }

    private void updateSensorStatus(ArrayList<View> views) {
        for(View view: views) {
            view.setBackgroundColor(getColor(android.R.color.holo_green_light));
        }
    }

    private void allowSharing() {
        findViewById(R.id.share_btn).setVisibility(View.VISIBLE);
    }

    private class CreationAsyncTask extends AsyncTask {

        HashMap<String, Long> lastAt = new HashMap<>();
        ArrayList<View> views = new ArrayList<View>();
        boolean showShare = true;

        @Override
        protected Object doInBackground(Object[] objects) {
            SQLiteDatabase db = (new DatabaseHelper(getApplicationContext())).getReadableDatabase();
            String latestQuery = "SELECT " + SensorDataContract.SensorData.COLUMN_NAME_SENSOR + ", MAX(" + SensorDataContract.SensorData.COLUMN_NAME_TIME + ") AS " + SensorDataContract.SensorData.COLUMN_NAME_TIME + " FROM " +
                    SensorDataContract.SensorData.TABLE_NAME + " GROUP BY " + SensorDataContract.SensorData.COLUMN_NAME_SENSOR;
            final Cursor latestCollectionCursor = db.rawQuery(latestQuery, null);

            while (latestCollectionCursor.moveToNext()) {
                String sensor = latestCollectionCursor.getString(latestCollectionCursor.getColumnIndex(SensorDataContract.SensorData.COLUMN_NAME_SENSOR));
                long time = latestCollectionCursor.getLong(latestCollectionCursor.getColumnIndex(SensorDataContract.SensorData.COLUMN_NAME_TIME));
                lastAt.put(sensor, time);
            }
            latestCollectionCursor.close();

            String twoWeeksQuery = "SELECT MIN(" + SensorDataContract.SensorData.COLUMN_NAME_TIME + ") AS minimum, MAX(" + SensorDataContract.SensorData.COLUMN_NAME_TIME + ") AS maximum FROM " + SensorDataContract.SensorData.TABLE_NAME;
            final Cursor twoWeeksCursor = db.rawQuery(twoWeeksQuery, null);

            if(twoWeeksCursor.moveToFirst()) {
                long minimum = twoWeeksCursor.getLong(0);
                long maximum = twoWeeksCursor.getLong(1);
                if (maximum - minimum >= (2 * 7 * 24 * 60 * 60 * 1000)) {
                    showShare = true;
                }
            }


            db.close();



            SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
            if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                startService(new Intent(MainActivity.this, AccelerometerService.class));
            } else {
                Log.d("AhmedTest", "No Accelerometer Found");
            }

            if(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
                startService(new Intent(MainActivity.this, GyroscopeService.class));
            } else {
                Log.d("AhmedTest", "No Gyroscope Found");
            }

            if(sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
                startService(new Intent(MainActivity.this, LightService.class));
            } else {
                Log.d("AhmedTest", "No Light Sensor Found");
            }

            if(sensorManager.getDefaultSensor(Sensor.TYPE_STATIONARY_DETECT) != null) {
                startService(new Intent(MainActivity.this, StationaryService.class));
            } else {
                Log.d("AhmedTest", "No Stationary Sensor Found");
            }

            if(sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null) {
                startService(new Intent(MainActivity.this, SigMotionService.class));
            } else {
                Log.d("AhmedTest", "No SigMotion Sensor Found");
            }

            Class[] serviceClasses = {AccelerometerService.class, GyroscopeService.class, LightService.class, StationaryService.class, SigMotionService.class};
            for (Class service: serviceClasses) {
                if (serviceIsRunning(service)) {
                    View statusView = null;
                    if (service.getName().equals("edu.aucegypt.AEdatacollector.AccelerometerService")) {
                        statusView = findViewById(R.id.accelerometerStatusView);
                    } else if (service.getName().equals("edu.aucegypt.AEdatacollector.GyroscopeService")) {
                        statusView = findViewById(R.id.gyroscopeStatusView);
                    } else if (service.getName().equals("edu.aucegypt.AEdatacollector.LightService")) {
                        statusView = findViewById(R.id.lightStatusView);
                    } else if (service.getName().equals("edu.aucegypt.AEdatacollector.StationaryService")) {
                        statusView = findViewById(R.id.stationaryStatusView);
                    } else if (service.getName().equals("edu.aucegypt.AEdatacollector.SigMotionService")) {
                        statusView = findViewById(R.id.sigMotionStatusView);
                    }

                    if (statusView != null) {
                        views.add(statusView);
                    }
                }
            }


            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            updateTimeForSensor(lastAt);
            updateSensorStatus(views);
            if (showShare) {
                allowSharing();
            }
        }

        private boolean serviceIsRunning(Class<?> serviceClass) {
            try {
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (serviceClass.getName().equals(service.service.getClassName())) {
                        return true;
                    }
                }
                return false;
            } catch (NullPointerException npe) {
                Log.e("AhmedTest", "Couldn't get running services", npe);
                return false;
            }
        }
    }
}
