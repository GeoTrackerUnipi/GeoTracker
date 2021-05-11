package com.geotracer.geotracer.notifications;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class NotificationSender extends Service {

    public class LocalBinder extends Binder {

        public NotificationSender getService() {
            return NotificationSender.this;
        }

    }

    private final ServiceConnection keyValueService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            KeyValueManagement.LocalBinder binder = (KeyValueManagement.LocalBinder) service;
            keyValueStore = binder.getService();

            //  getting from the local database all the user's buckets
            RetStatus<List<String>> result = keyValueStore.buckets.getBuckets();

            try {
                if (result.getStatus() == OpStatus.OK)
                    observedLocations.addAll(result.getValue());

                observedLocations.forEach(bucket -> {
                    listeners
                            .put(bucket, db.collection(bucket)
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable @org.jetbrains.annotations.Nullable QuerySnapshot value,
                                                            @Nullable @org.jetbrains.annotations.Nullable FirebaseFirestoreException error) {

                                            assert value != null;
                                            if(value.getMetadata().isFromCache()) return;

                                            for (DocumentChange dc : value.getDocumentChanges())
                                                if( dc.getType() == DocumentChange.Type.ADDED &&
                                                        keyValueStore.beacons.beaconPresent(
                                                                (String)dc.getDocument().getData().get("signature")) == OpStatus.PRESENT){
                                                    infectionReaction();
                                                    break;
                                                }
                                        }
                                    })
                            );
                });
            }catch(RuntimeException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            keyValueStore = null;

        }
    };

    private final IBinder classBinder = new NotificationSender.LocalBinder();
    private final Logger logger = Logger.getGlobal();
    private final HashMap<String, ListenerRegistration> listeners = new HashMap<>();
    private final List<String> observedLocations = new ArrayList<>();
    private FirebaseFirestore db;
    private KeyValueManagement keyValueStore;

    @Override
    public void onCreate() {

        super.onCreate();
        try {
            //  creating connection to Firestore
            db = FirebaseFirestore.getInstance();

            //  creating connection to service KeyValueManagement
            logger.info("Context: " + getApplicationContext() + " local: " + getBaseContext());
            Intent service = new Intent(getBaseContext(), KeyValueManagement.class);
            bindService(service, keyValueService, Context.BIND_AUTO_CREATE);

        }catch(RuntimeException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy(){
        try {
            unbindService(keyValueService);
            db.terminate();
        }catch(RuntimeException e){
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return classBinder;

    }

    public void infectionAlert(){

        WorkManager
                .getInstance(this.getBaseContext())
                .enqueue(
                        new OneTimeWorkRequest.Builder(InfectionAlarm.class).build()
                );

    }

    public OpStatus addBucket(String bucket){

        OpStatus status = keyValueStore.buckets.insertBucket(bucket);
        if( status != OpStatus.OK)
            return status;

        listeners.put(bucket,db.collection(bucket).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable @org.jetbrains.annotations.Nullable QuerySnapshot value,
                                @Nullable @org.jetbrains.annotations.Nullable FirebaseFirestoreException error) {

                assert value != null;
                if(value.getMetadata().isFromCache()) return;
                for (DocumentChange dc : value.getDocumentChanges())
                    if( dc.getType() == DocumentChange.Type.ADDED && keyValueStore.beacons.beaconPresent( (String)dc.getDocument().getData().get("signature")) == OpStatus.PRESENT){
                        infectionReaction();
                        break;
                    }
            }
        }));
        return OpStatus.OK;
    }

    public OpStatus removeBucket(String bucket){
        OpStatus result = keyValueStore.buckets.removeBucket(bucket);
        if( result == OpStatus.OK)
            Objects.requireNonNull(listeners.get(bucket)).remove();
        return result;
    }

    public void infectionReaction(){
        logger.info("INFECTED!!!!!");
    }
}
