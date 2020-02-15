package edu.aucegypt.AEdatacollector;

import android.provider.BaseColumns;

public final class SensorDataContract {

    private SensorDataContract() {

    }

    public static class SensorData implements BaseColumns {
        public static final String TABLE_NAME = "sensordata";
        public static final String COLUMN_NAME_SENSOR = "sensor";
        public static final String COLUMN_NAME_VALUE = "value";
        public static final String COLUMN_NAME_TIME = "time";
    }
}
