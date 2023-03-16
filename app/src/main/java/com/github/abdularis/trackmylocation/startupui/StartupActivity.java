package com.github.abdularis.trackmylocation.startupui;

import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;

import com.github.abdularis.trackmylocation.R;
import com.github.abdularis.trackmylocation.common.Util;
import com.github.abdularis.trackmylocation.dashboard.MainActivity;
import com.github.abdularis.trackmylocation.model.users;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StartupActivity extends AppCompatActivity {

    // request code untuk login
    private static final int RC_LOGIN = 123;
    Spinner login_spinner;
    FirebaseUser firebaseUser;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        login_spinner = findViewById(R.id.login_spinner);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Util.checkGooglePlayServicesAvailability(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_LOGIN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                //saving to realtime database
                firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                DatabaseReference myref = FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getUid());
                String username = firebaseUser.getDisplayName();
                if (firebaseUser.getDisplayName() == null) {
                    username = "NA";
                }
                users user = new users(firebaseUser.getEmail(), username, login_spinner.getSelectedItem().toString(), FirebaseAuth.getInstance().getUid());
                myref.setValue(user);
                goToMainActivity();
                SharedPreferences.Editor shared = getSharedPreferences("user_type", MODE_PRIVATE).edit();
                shared.putString("usertype", login_spinner.getSelectedItem().toString());
                shared.commit();


                //saving to fireastore datbase
                Map<String, Object> user1 = new HashMap<>();
                user1.put("name", firebaseUser.getDisplayName());
                user1.put("devId", FirebaseAuth.getInstance().getUid());
                user1.put("photoUrl", "null");

                //saving to collection
                db.collection("users").document(FirebaseAuth.getInstance().getUid())
                        .set(user1)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("TAG", "DocumentSnapshot successfully written!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("TAG", "Error writing document", e);
                            }
                        });

            } else {
                if (response == null) {
                    Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
                } else if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "No Network Connection", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void onSignInClicked(View view) {
      Intent i = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.GoogleBuilder().build()
                ))
                .setLogo(R.drawable.logo)
                .build();
        startActivityForResult(i, RC_LOGIN);

    }

    private void goToMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }
}