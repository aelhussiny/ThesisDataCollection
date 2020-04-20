package edu.aucegypt.AEdatacollector;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ProgressDialog pd;
    SQLiteDatabase db;
    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("AhmedTest", "OnCreate");
        super.onCreate(null);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = (new DatabaseHelper(MainActivity.this)).getReadableDatabase();
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
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please Wait");
            pd.setCancelable(false);
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    switch (message.what) {
                        case 1:
                            pd.setIndeterminate(true);
                            pd.show();
                            break;
                        case 2:
                            pd.setIndeterminate(false);
                            pd.dismiss();
                            break;
                        case 3:
                            pd.setMax(message.arg1);
                            break;
                        case 4:
                            pd.setProgress(message.arg1);
                    }
                }
            };
            if(checkPermissions()) {
                executeCreationTask();
            } else {
                requestMainPermissions();
            }
            findViewById(R.id.share_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        verifyStoragePermissions(MainActivity.this);
                        if(PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            createAndShareDBCopy();
                        }
                    } catch (Exception e) {
                        Log.e("AhmedTest", "Error:", e);
                        Toast.makeText(getApplicationContext(), "Error encountered", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

    }

    private void requestMainPermissions() {
        ArrayList<String> neededPermissions = new ArrayList<String>();
        neededPermissions.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            neededPermissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[neededPermissions.size()]), 1);
    }

    private boolean checkPermissions() {
        boolean granted = true;
        granted = granted && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_BOOT_COMPLETED) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            granted = granted && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED;
        }
        return granted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Arrays.binarySearch(grantResults, -1) >= 0 || grantResults.length == 0) {
            if (requestCode == 1) {
                Toast.makeText(MainActivity.this, "Application cannot proceed without correct permissions", Toast.LENGTH_LONG).show();
                requestMainPermissions();
            } else if (requestCode == 2) {
                Toast.makeText(MainActivity.this, "Cannot share database without files permission", Toast.LENGTH_LONG).show();
            }
        } else {
            if (requestCode == 1) {
                executeCreationTask();
            } else if (requestCode == 2) {
                try {
                    createAndShareDBCopy();
                } catch (Exception e) {
                    Log.e("AhmedTest", "Error:", e);
                    Toast.makeText(getApplicationContext(), "Error encountered", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void createAndShareDBCopy() throws Exception {
        startDBCopy();
    }

    private void executeCreationTask() {
        pd.show();
        (new CreationAsyncTask()).execute("");
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
                    2
            );
        }
    }

    private void startDBCopy() throws Exception {
        File file = getApplicationContext().getDatabasePath(DatabaseHelper.DATABASE_NAME);

        if(file.exists() && file.canRead())
        {
            File destination = new File(this.getExternalFilesDir(null), "database_copy_" + (new Date()).getTime() + ".db");
            if (!destination.exists()) {
                boolean result = destination.getParentFile().mkdirs();
                destination.createNewFile();
            }
            CopyAsyncTask copyTask = new CopyAsyncTask();
            copyTask.execute(file, destination);
        } else {
            throw new Exception("File inaccessible");
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            executeCreationTask();
        } catch (Exception e) {
            Log.e("AhmedTest", "Error executing creation task", e);
        }
    }

    protected void fileCopyFinished(File destination) {
        Uri destinationUri = FileProvider.getUriForFile(
                MainActivity.this, getApplicationContext().getPackageName() + ".provider", destination);
        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setStream(destinationUri)
                .setType("application/x-sqlite3")
                .setChooserTitle("Share Sensor Data")
                .getIntent();
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Sensor Data"));
    }

    private class CreationAsyncTask extends AsyncTask {


        HashMap<String, Long> lastAt = new HashMap<>();
        ArrayList<View> views = new ArrayList<View>();
        boolean showShare = false;

        public CreationAsyncTask() {

        }

        @Override
        protected Object doInBackground(Object[] objects) {
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferencesFile), Context.MODE_PRIVATE);
            Log.d("AhmedTest", "Starting doInBackground");

            String[] sensors = {getString(R.string.accelerometerID), getString(R.string.gyroscopeID), getString(R.string.lightSensorID)};
            for (String sensor: sensors) {
                String lastReading = sharedPreferences.getString("lastReadingFor" + sensor, "");
                if (lastReading.length() > 0) {
                    long time = Long.parseLong(lastReading.substring(0, lastReading.indexOf(("_"))));
                    lastAt.put(sensor, time);
                }
            }

            Log.d("AhmedTest", "Done with latestQuery");

            PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent i = new Intent();
                if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                    i.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            }


            if(!serviceIsRunning(SensorService.class)) {
                Intent sensorServiceIntent = new Intent(MainActivity.this, SensorService.class);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(sensorServiceIntent);
                } else {
                    startService(sensorServiceIntent);
                }
            }

            Log.d("AhmedTest", "Done with Service Starting");

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.e("AhmedTest", "Thread Sleep Error", e);
            }

            Log.d("AhmedTest", "Done with waiting for service starting");

            SensorService service = SensorService.getInstance();
            if (service != null) {
                Iterator<String> monitoredSensorsIterator = service.getMonitoredSensors().iterator();
                while (monitoredSensorsIterator.hasNext()) {
                    String sensor = monitoredSensorsIterator.next();
                    View statusView = null;
                    if (sensor.equals(getString(R.string.accelerometerID))) {
                        statusView = findViewById(R.id.accelerometerStatusView);
                    } else if (sensor.equals(getString(R.string.gyroscopeID))) {
                        statusView = findViewById(R.id.gyroscopeStatusView);
                    } else if (sensor.equals(getString(R.string.lightSensorID))) {
                        statusView = findViewById(R.id.lightStatusView);
                    }

                    if (statusView != null) {
                        views.add(statusView);
                    }
                }
            }

            Log.d("AhmedTest", "Done with checking monitored sensors");

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            Log.d("AhmedTest", "Starting PostExecute");
            updateTimeForSensor(lastAt);
            updateSensorStatus(views);
            pd.dismiss();
            Log.d("AhmedTest", "Finishing PostExecute");
        }

        private boolean serviceIsRunning(Class<?> serviceClass) {
            Log.d("AhmedTest", "Starting serviceIsRunning");
            try {
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (serviceClass.getName().equals(service.service.getClassName())) {
                        Log.d("AhmedTest", "Ending serviceIsRunning");
                        return true;
                    }
                }
                Log.d("AhmedTest", "Ending serviceIsRunning");
                return false;
            } catch (NullPointerException npe) {
                Log.d("AhmedTest", "Ending serviceIsRunning");
                Log.e("AhmedTest", "Couldn't get running services", npe);
                return false;
            }
        }
    }

    private class CopyAsyncTask extends AsyncTask {

        File destination;

        @Override
        protected Object doInBackground(Object[] files) {
            Message message1 = mHandler.obtainMessage(1);
            message1.sendToTarget();
            int totalSize = (int)Math.ceil(((File)files[0]).length() / 1024);
            Message message3 = mHandler.obtainMessage(3, totalSize);
            message3.sendToTarget();
            try {
                destination = (File)files[1];
                copy((File) files[0], (File) files[1]);
            } catch (Exception e) {
                Message message2 = mHandler.obtainMessage(2);
                message2.sendToTarget();
                Toast.makeText(getApplicationContext(), "Problem with copying file", Toast.LENGTH_LONG).show();
            }
            return null;
        }

        private void copy(File src, File dst) throws IOException {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst, false);

            byte[] buf = new byte[1024];
            int len;
            int soFar = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                Message message = mHandler.obtainMessage(4, soFar++);
                message.sendToTarget();
            }
            in.close();
            out.close();
        }

        @Override
        protected void onPostExecute(Object o) {
            Message message = mHandler.obtainMessage(2);
            message.sendToTarget();
            fileCopyFinished(destination);
        }
    }
}
