package edu.aucegypt.AEdatacollector;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferencesFile), Context.MODE_PRIVATE);
        boolean accepted = sharedPreferences.getBoolean(getString(R.string.consentAcceptanceKey), false);
        if (!accepted || !(sharedPreferences.contains(getString(R.string.consentName)) && sharedPreferences.contains(getString(R.string.consentDate)) && sharedPreferences.contains(getString(R.string.consentSignature)))) {
            backToConsent();
        } else {
            setContentView(R.layout.activity_register);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ArrayList<String> validationErrors = validateInputs();
                    if (validationErrors.size() == 0) {
                        addToDatabase();
                        goToMain();
                    } else {
                        showErrors(validationErrors);
                    }
                }
            });
        }
    }

    private void goToMain() {
        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
        finish();
    }

    private void backToConsent() {
        startActivity(new Intent(RegisterActivity.this, ConsentActivity.class));
        finish();
    }

    private void showErrors(ArrayList<String> validationErrors) {
        Toast.makeText(RegisterActivity.this, getString(R.string.please) + " " + TextUtils.join(" and ", validationErrors), Toast.LENGTH_LONG).show();
    }

    private void addToDatabase() {
        String gender = ((RadioButton)findViewById(((RadioGroup)findViewById(R.id.genderRG)).getCheckedRadioButtonId())).getText().toString();
        String ageGroup = ((RadioButton)findViewById(((RadioGroup)findViewById(R.id.ageGroupRG)).getCheckedRadioButtonId())).getText().toString();

        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferencesFile), Context.MODE_PRIVATE);
        SQLiteDatabase db = (new DatabaseHelper(getApplicationContext())).getWritableDatabase();

        insertIntoUserData(db, "name", sharedPreferences.getString(getString(R.string.consentName),""));
        insertIntoUserData(db, "consentDate", sharedPreferences.getLong(getString(R.string.consentDate),-1)+"");
        insertIntoUserData(db, "signature", sharedPreferences.getString(getString(R.string.consentSignature),""));
        insertIntoUserData(db, "gender", gender);
        insertIntoUserData(db, "ageGroup", ageGroup);

        db.close();
    }

    private void insertIntoUserData(SQLiteDatabase db, String key, String value) {
        if (value.length() > 0 && !value.equals("-1")) {
            ContentValues values = new ContentValues();
            values.put(UserDataContract.UserData.COLUMN_NAME_KEY, key);
            values.put(UserDataContract.UserData.COLUMN_NAME_VALUE, value);
            db.insertWithOnConflict(UserDataContract.UserData.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private ArrayList<String> validateInputs() {
        ArrayList<String> errors = new ArrayList<String>();

        RadioGroup genderRadioGroup = (RadioGroup)findViewById(R.id.genderRG);
        RadioGroup ageGroupRadioGroup = (RadioGroup)findViewById(R.id.ageGroupRG);

        if(genderRadioGroup.getCheckedRadioButtonId() == -1) {
            errors.add(getString(R.string.selectGender));
        }

        if(ageGroupRadioGroup.getCheckedRadioButtonId() == -1) {
            errors.add(getString(R.string.selectAgeGroup));
        }

        return errors;
    }

}
