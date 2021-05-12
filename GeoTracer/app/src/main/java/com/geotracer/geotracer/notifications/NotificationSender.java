package com.geotracer.geotracer.notifications;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.google.firebase.firestore.ListenerRegistration;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentChange;
import android.content.ServiceConnection;
import androidx.work.OneTimeWorkRequest;
import com.esotericsoftware.minlog.Log;
import android.content.ComponentName;
import androidx.work.WorkManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import android.content.Context;
import android.content.Intent;
import android.app.Service;
import java.util.ArrayList;
import android.os.IBinder;
import java.util.HashMap;
import android.os.Binder;
import java.util.Objects;
import java.util.List;

import io.paperdb.Paper;


@SuppressWarnings("unused")
public class NotificationSender extends Service {
    public static final String ACTION_BROADCAST = NotificationSender.class.getName();
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

                observedLocations.forEach(bucket -> listeners
                        .put(bucket, db.collection(bucket)
                                .addSnapshotListener((value, error) -> {

                                    assert value != null;
                                    if(value.getMetadata().isFromCache()) return;

                                    for (DocumentChange dc : value.getDocumentChanges())
                                        if( dc.getType() == DocumentChange.Type.ADDED &&
                                                keyValueStore.beacons.beaconPresent(
                                                        (String)dc.getDocument().getData().get("signature")) == OpStatus.PRESENT){
                                            infectionReaction();
                                            break;
                                        }
                                })
                        ));
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

        Log.info(this.getClass().getName(), "Service Bounded");
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

        listeners
                .put(bucket, db.collection(bucket)
                        .addSnapshotListener((value, error) -> {

                            assert value != null;
                            if(value.getMetadata().isFromCache()) return;

                            for (DocumentChange dc : value.getDocumentChanges())
                                if( dc.getType() == DocumentChange.Type.ADDED &&
                                        keyValueStore.beacons.beaconPresent(
                                                (String)dc.getDocument().getData().get("signature")) == OpStatus.PRESENT){
                                    infectionReaction();
                                    break;
                                }
                        })
                );

        return OpStatus.OK;
    }

    public OpStatus removeBucket(String bucket){
        OpStatus result = keyValueStore.buckets.removeBucket(bucket);
        if( result == OpStatus.OK)
            Objects.requireNonNull(listeners.get(bucket)).remove();
        return result;
    }

    public boolean amiInfected(){
        if(Paper.book("notification_state").contains("application_state")){
            try {
                if(Objects.requireNonNull(DateFormat.getDateInstance().parse(Paper.book("notification_state").read("application_state"))).after(new Date()))
                    return true;
                else
                    Paper.book("notification_state").delete("application_state");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void infectionReaction(){

        //  notifications expire time 14 days
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, 14);

            //  if the user has stored information we update them in order to increase the expire
        if( Paper.book("notification_state").contains("application_state"))
            Paper.book("notification_state").delete("application_state");
        else{
            //  otherwise we advise the GUI to show a notification and store the expiring
            String info = "WARNING!!\n You could have been in contact with an infected person\n";
            Intent intent = new Intent(ACTION_BROADCAST);
            intent.putExtra("Contact", info);
            if(LocalBroadcastManager.getInstance(this).sendBroadcast(intent))
                Log.info(this.getClass().getName(), "Message sent");
        }

        Paper.book("notification_state").write("application_state", calendar.getTime().toString());

    }
}
