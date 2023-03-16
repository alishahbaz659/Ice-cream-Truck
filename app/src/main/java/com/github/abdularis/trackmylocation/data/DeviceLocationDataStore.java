package com.github.abdularis.trackmylocation.data;

import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.github.abdularis.trackmylocation.data.rxfirestore.RxFirestore;
import com.github.abdularis.trackmylocation.model.SharedLocation;
import com.github.abdularis.trackmylocation.model.users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class DeviceLocationDataStore {

    private static final String TAG = "DeviceLocationDataStore";

    private FirebaseUser mUser;
    private DocumentReference mUserDocRef;
    private DocumentReference mShareLocDocRef;
    private Disposable mShareLocDisposable;
    List<users> list;
    String usertype;

    public DeviceLocationDataStore() {
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        list = new ArrayList<>();
        if (mUser == null) return;

        mUserDocRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(mUser.getUid());


    }

    public Observable<String> getDeviceId() {
        return RxFirestore.getDocument(mUserDocRef)
                .map(documentSnapshot -> documentSnapshot.getString("devId"));
    }

    public Observable<SharedLocation> getSharedLocationUpdate(String devId) {
        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection("shared_locations")
                .document(devId);
        return RxFirestore.getDocument(docRef)
                .map(documentSnapshot -> documentSnapshot.toObject(SharedLocation.class));
    }

    public void shareMyLocation(Flowable<Location> locationFlowable) {

        mShareLocDisposable = Flowable
                .combineLatest(locationFlowable, getDeviceId().toFlowable(BackpressureStrategy.MISSING), (location, devId) -> {
                    SharedLocation sl = new SharedLocation();
                    sl.setLocation(new SharedLocation.LatLong(location.getLatitude(), location.getLongitude()));
                    saveToFirebase(sl);
                    return sl;
                })
                .subscribe(this::saveToFirebase,
                        throwable -> Log.v(TAG, "shareMyLocation:onError: " + throwable.toString()));

    }

    public Completable stopShareMyLocation() {
        if (mShareLocDisposable != null && !mShareLocDisposable.isDisposed()) {
            mShareLocDisposable.dispose();
            mShareLocDisposable = null;

            if (mShareLocDocRef != null) {
                return Completable.create(emitter -> {
                    mShareLocDocRef.delete()
                            .addOnSuccessListener(aVoid -> emitter.onComplete())
                            .addOnFailureListener(emitter::onError);
                    mShareLocDocRef = null;
                });
            }
        }

        return Completable.complete();
    }

    private void saveToFirebase(SharedLocation sharedLocation) {


        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        Query query = rootRef.child("Users").orderByChild("useruid").equalTo(FirebaseAuth.getInstance().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    users u = ds.getValue(users.class);
                    list.add(u);
                }
                usertype=list.get(0).getUsertype();
                Log.d("TAG",usertype);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });


        DatabaseReference myref = FirebaseDatabase.getInstance().getReference("Userlocations").child(FirebaseAuth.getInstance().getUid());
        SharedLocation sharedLocation1 = new SharedLocation();
        sharedLocation1.setLocation(sharedLocation.getLocation());
        sharedLocation1.setUsertype(usertype);
        sharedLocation1.setUseruid(FirebaseAuth.getInstance().getUid());
        myref.setValue(sharedLocation1);

    }

}
