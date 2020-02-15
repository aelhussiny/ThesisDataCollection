package edu.aucegypt.AEdatacollector;

import android.provider.BaseColumns;

public final class UserDataContract {

    private UserDataContract() {

    }

    public static class UserData implements BaseColumns {
        public static final String TABLE_NAME = "userdata";
        public static final String COLUMN_NAME_KEY = "key";
        public static final String COLUMN_NAME_VALUE = "value";
    }
}
