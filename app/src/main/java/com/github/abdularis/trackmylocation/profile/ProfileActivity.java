package com.github.abdularis.trackmylocation.profile;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.abdularis.trackmylocation.R;

import com.github.abdularis.trackmylocation.dashboard.MainActivity;
import com.github.abdularis.trackmylocation.model.userbio;
import com.github.abdularis.trackmylocation.model.users;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProfileActivity extends AppCompatActivity {

    @BindView(R.id.user_profile_name)
    TextView txt_profileName;
    @BindView(R.id.user_profile_email)
    TextView txt_profileEmail;
    @BindView(R.id.profile_userType)
    TextView txt_profileType;
    @BindView(R.id.profile_user_info)
    TextView txt_userInfo;
    @BindView(R.id.btn_update)
    Button btn_update;
    List<userbio> bio_list;
    List<users> list;
    FirebaseAuth firebaseAuth;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);
        list = new ArrayList<users>();
        bio_list = new ArrayList<userbio>();
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = ProgressDialog.show(ProfileActivity.this, "",
                "Please wait...", true);
        fetchData();
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show_update_dialog();
            }
        });

    }

    private void show_update_dialog() {
        final Dialog dialog = new Dialog(ProfileActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.profile_edit_alert_dialog);
        dialog.setCancelable(true);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button dialogButton = (Button) dialog.findViewById(R.id.btn_update);
        EditText edt_name = dialog.findViewById(R.id.edt_name);
        EditText edt_bio = dialog.findViewById(R.id.edt_bio);

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = edt_name.getText().toString();
                String bio = edt_bio.getText().toString();
                if (isValid(userName, bio)) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseAuth.getUid()).child("username");
                    databaseReference.setValue(userName);
                    DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference("UsersBio").child(firebaseAuth.getUid()).child("bio");
                    databaseReference1.setValue(bio);
                    Toast.makeText(ProfileActivity.this, "Successfully updated", Toast.LENGTH_SHORT).show();
                    recreate();
                }
                dialog.dismiss();
            }
        });


        edt_name.setText(list.get(0).getUsername());
        SharedPreferences sharedPreferences = getSharedPreferences("info", MODE_PRIVATE);
        String bio = sharedPreferences.getString("bio", "");
        if (sharedPreferences.contains("bio")) {
            edt_bio.setText(bio);
        }
        edt_name.setSelection(edt_name.getText().length());


        dialog.show();
    }


    public boolean isValid(String name, String bio) {
        if (name.isEmpty() || bio.isEmpty()) {
            Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            return false;
        } else if (name.isEmpty()) {
            Toast.makeText(this, "Please enter userName", Toast.LENGTH_SHORT).show();
            return false;
        } else if (bio.isEmpty()) {
            Toast.makeText(this, "Please enter your bio", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void fetchData() {
        dialog.show();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        Query query = rootRef.child("Users").orderByChild("useruid").equalTo(firebaseAuth.getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    users u = ds.getValue(users.class);
                    list.add(u);
                }
                txt_profileName.setText("Name: " + list.get(0).getUsername());
                txt_profileEmail.setText("Email: " + list.get(0).getUseremail());
                txt_profileType.setText("Type: " + list.get(0).getUsertype());
                dialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        DatabaseReference rootRef1 = FirebaseDatabase.getInstance().getReference().child("UsersBio").child(firebaseAuth.getUid()).child("bio");
        rootRef1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bio_list.clear();
                String bio = dataSnapshot.getValue(String.class);
                if (bio != null) {
                    SharedPreferences.Editor shared = getSharedPreferences("info", MODE_PRIVATE).edit();
                    shared.putString("bio", bio);
                    shared.commit();
                    txt_userInfo.setText("Bio: " + bio);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
