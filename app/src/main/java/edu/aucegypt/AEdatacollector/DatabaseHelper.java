package edu.aucegypt.AEdatacollector;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    Context context = null;
    private static DatabaseHelper sInstance;

    public static final int DATABASE_VERSION = 6;
    public static final String DATABASE_NAME = "AEDataCollectorDB.db";

    private static final String createUserDataTableSQL =
            "CREATE TABLE IF NOT EXISTS " + UserDataContract.UserData.TABLE_NAME + " (" +
                    UserDataContract.UserData.COLUMN_NAME_KEY + " TEXT PRIMARY KEY, " +
                    UserDataContract.UserData.COLUMN_NAME_VALUE + " TEXT)";

    private static final String createSensorDataTableSQL =
            "CREATE TABLE IF NOT EXISTS " + SensorDataContract.SensorData.TABLE_NAME + " (" +
                    SensorDataContract.SensorData._ID + " INTEGER PRIMARY KEY, " +
                    SensorDataContract.SensorData.COLUMN_NAME_SENSOR + " TEXT, " +
                    SensorDataContract.SensorData.COLUMN_NAME_VALUE + " TEXT, " +
                    SensorDataContract.SensorData.COLUMN_NAME_TIME + " INTEGER)";

    private static final String dropSensorDataTableSQL =
            "DROP TABLE IF EXISTS " + SensorDataContract.SensorData.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createUserDataTableSQL);
        db.execSQL(createSensorDataTableSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(dropSensorDataTableSQL);
        db.execSQL(createSensorDataTableSQL);
    }
}