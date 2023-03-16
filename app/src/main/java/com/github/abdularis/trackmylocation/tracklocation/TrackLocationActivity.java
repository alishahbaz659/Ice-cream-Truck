package com.github.abdularis.trackmylocation.tracklocation;

import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.abdularis.trackmylocation.App;
import com.github.abdularis.trackmylocation.R;
import com.github.abdularis.trackmylocation.ViewModelFactory;
import com.github.abdularis.trackmylocation.data.rxfirestore.errors.DocumentNotExistsException;
import com.github.abdularis.trackmylocation.model.SharedLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TrackLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Marker mMyLocMarker;
    private GoogleMap mGoogleMap;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.text_dev_id)
    EditText mTextDevId;
    @BindView(R.id.btn_track)
    Button mBtnTrack;
    @BindView(R.id.text_loc_stat)
    TextView mTextLocStat;
    public List<SharedLocation> location_list;

    @Inject
    ViewModelFactory mViewModelFactory;
    TrackLocationViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_location);
        location_list = new ArrayList<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.track_location);
        }

        ((App) getApplication()).getAppComponent().inject(this);
        initViewModel();
        SharedPreferences sharedPreferences = getSharedPreferences("user_type", MODE_PRIVATE);
        String usertype = sharedPreferences.getString("usertype", "");
        if (usertype.equals("Driver")) {
            usertype = "Customer";
        } else if(usertype.equals("Customer")){
            usertype = "Driver";
        }
        String finalUsertype = usertype;
        mBtnTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mViewModel.isTracking()) {
                    mViewModel.stopTracking();
                    return;
                }
                Toast.makeText(TrackLocationActivity.this, "Start tracking...", Toast.LENGTH_SHORT).show();
                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                Log.d("TAG",finalUsertype);
                Query query = rootRef.child("Userlocations").orderByChild("usertype").equalTo(finalUsertype);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        location_list.clear();
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            SharedLocation u = ds.getValue(SharedLocation.class);
                            location_list.add(u);
                        }
                        //getting all the location based on their types from database
                        for (int i = 0; i <= location_list.size() - 1; i++) {
                            locationUpdated(location_list.get(i));
                        }
                        //    Toast.makeText(TrackLocationActivity.this, "" + location_list.size(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewModel.stopTracking();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
    }


    private void locationUpdated(SharedLocation sharedLocation) {
        LatLng pos = new LatLng(sharedLocation.getLocation().latitude, sharedLocation.getLocation().longitude);
        //&& mMyLocMarker == null
        if (mGoogleMap != null) {

            if (sharedLocation.getUsertype().equals("Driver")) {
                MarkerOptions options = new MarkerOptions()
                        .title(sharedLocation.getUsertype())
                        .position(pos).icon(BitmapDescriptorFactory.fromResource(R.drawable.truck));
                mMyLocMarker = mGoogleMap.addMarker(options);
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 2));
            } else if (sharedLocation.getUsertype().equals("Customer")) {
                MarkerOptions options = new MarkerOptions()
                        .title(sharedLocation.getUsertype())
                        .position(pos).icon(BitmapDescriptorFactory.fromResource(R.drawable.customer));
                mMyLocMarker = mGoogleMap.addMarker(options);
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 2));
            }
        } else if (mMyLocMarker != null) {
            mMyLocMarker.setPosition(pos);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(pos));
        }

        mTextLocStat.setText(pos.toString());
    }

    private void initViewModel() {
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(TrackLocationViewModel.class);
        mViewModel.getTrackingState().observe(this, tracking -> {
            if (tracking != null && tracking) {
                mTextDevId.setEnabled(false);
                mBtnTrack.setBackground(getResources().getDrawable(R.drawable.bg_btn_stop));
                mBtnTrack.setText(R.string.stop);
                mTextLocStat.setText(R.string.fetching);
            } else {
                mTextDevId.setEnabled(true);
                mBtnTrack.setBackground(getResources().getDrawable(R.drawable.bg_btn_start));
                mBtnTrack.setText(R.string.start);
                mTextLocStat.setText(R.string.disconnected);
            }
        });
    }


}
