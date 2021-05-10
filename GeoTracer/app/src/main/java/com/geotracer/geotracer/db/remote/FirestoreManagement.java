package com.geotracer.geotracer.db.remote;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


public class FirestoreManagement extends Service {

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public class LocalBinder extends Binder {

        public FirestoreManagement getService() {
            return FirestoreManagement.this;
        }

    }

    private final IBinder classBinder = new LocalBinder();
    private final Logger logger = Logger.getGlobal();

    @Override
    public IBinder onBind(Intent intent) {
        return classBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return true;
    }

    public void insertLocation(String bucket){
        FirebaseDatabase rtd = FirebaseDatabase.getInstance();
        DatabaseReference myRef = rtd.getReference("notifications");
        Map<String, Object> user = new HashMap<>();
        user.put("first", "Ada");
        user.put("last", "Lovelace");
        user.put("born", 1815);

        db.collection("geotraces")
                .add(user)
                .addOnSuccessListener(documentReference -> logger.info("DocumentSnapshot added with ID: " + documentReference.getId()))
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        logger.info( "Error adding document" + e);
                    }
                });
    }

    public RetStatus<List<String>> getNearLocations(){
        return null;
    }

    public OpStatus dropExpiredLocations(){
        return null;
    }
}

