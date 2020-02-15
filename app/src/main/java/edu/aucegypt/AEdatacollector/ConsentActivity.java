package edu.aucegypt.AEdatacollector;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Layout;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.Date;

public class ConsentActivity extends AppCompatActivity {

    AlertDialog signingDialog = null;
    boolean signed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferencesFile), Context.MODE_PRIVATE);

        setContentView(R.layout.activity_consent);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.agreeBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (signingDialog == null) {
                    createAcceptanceDialog(sharedPreferences);
                }
                signingDialog.show();
            }
        });
    }

    private void userAccepted(SharedPreferences sharedPreferences) {
        startActivity(new Intent(ConsentActivity.this, RegisterActivity.class));
        finish();

        /*
        String toastMessage = "Accepted ";
        toastMessage += sharedPreferences.getString(getString(R.string.consentName),"") + " ";
        toastMessage += sharedPreferences.getLong(getString(R.string.consentDate).toString(), 0) + " ";
        Toast.makeText(getApplication(), toastMessage, Toast.LENGTH_LONG).show();
        String encodedImage = sharedPreferences.getString(getString(R.string.consentSignature),null);

        byte[] b = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap bitmapImage = BitmapFactory.decodeByteArray(b, 0, b.length);
        ((ImageView)findViewById(R.id.signature)).setImageBitmap(bitmapImage);
        */

    }

    private void createAcceptanceDialog(final SharedPreferences sharedPreferences) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ConsentActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_sign, null))
                .setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        EditText consentNameEditText = (EditText)((AlertDialog)dialog).findViewById(R.id.consentNameEditText);
                        SignaturePad signaturePad = (SignaturePad) ((AlertDialog)dialog).findViewById(R.id.signature_pad);
                        String name = consentNameEditText.getText().toString();
                        String errorMessage = "";

                        if (name.length() < 5 || name.indexOf(' ') == -1) {
                            errorMessage += getString(R.string.enterFullName);
                        }

                        if (!signed) {
                            errorMessage += (errorMessage.length() > 0 ? " and " : "") + getString(R.string.enterSignature);
                        }

                        if (errorMessage.length() != 0) {
                            errorMessage = getString(R.string.please) + " " + errorMessage;
                            Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                            signingDialog.cancel();
                        } else {
                            Bitmap signatureBitmap = signaturePad.getSignatureBitmap();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            byte[] b = baos.toByteArray();
                            String encoded = Base64.encodeToString(b, Base64.DEFAULT);
                            editor.putBoolean(getString(R.string.consentAcceptanceKey), true);
                            editor.putString(getString(R.string.consentName), name);
                            editor.putLong(getString(R.string.consentDate), (new Date()).getTime());
                            editor.putString(getString(R.string.consentSignature), encoded);
                            editor.apply();
                            userAccepted(sharedPreferences);
                        }
                    }
                })
                .setCancelable(true);
        signingDialog = builder.create();
        signingDialog.setCanceledOnTouchOutside(true);
        signingDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                EditText consentNameEditText = (EditText)signingDialog.findViewById(R.id.consentNameEditText);
                consentNameEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            View view = ConsentActivity.this.getCurrentFocus();
                            if (view != null) {
                                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }

                        }
                        return  false;
                    }
                });

                SignaturePad signaturePad = (SignaturePad)signingDialog.findViewById(R.id.signature_pad);
                signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {
                    @Override
                    public void onStartSigning() {

                    }

                    @Override
                    public void onSigned() {
                        signed = true;
                    }

                    @Override
                    public void onClear() {

                    }
                });
            }
        });
        signingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                signed = false;
                signingDialog = null;
            }
        });
    }

}
