package com.geotracer.geotracer.db.remote;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.data.ExtLocation;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;


public class FirestoreManagement extends Service {

    private FirebaseFirestore firestore;
    private CollectionReference collection;
    private static final LocationAggregator aggregator = new LocationAggregator();

    public class LocalBinder extends Binder {

        public FirestoreManagement getService() {
            return FirestoreManagement.this;
        }

    }

    private final IBinder classBinder = new LocalBinder();
    private final Logger logger = Logger.getGlobal();

    @Override
    public void onCreate(){
        FirebaseApp.initializeApp(getBaseContext());
        firestore = FirebaseFirestore.getInstance();
        collection = firestore.collection("geotraces");
    }

    @Override
    public IBinder onBind(Intent intent) {

        return classBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        super.onUnbind(intent);
        return true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        firestore.terminate();
    }

    public void insertLocation(String ID, BaseLocation location){

        RetStatus<ExtLocation> status = aggregator.insertValue(ID, location);
        switch(status.getStatus()){
            case OK:
                logger.info("Location ready, updating firebase datastore");
                collection
                        .add(status.getValue())
                        .addOnSuccessListener(documentReference -> logger.info("DocumentSnapshot added with ID: " + documentReference.getId()))
                        .addOnFailureListener(e -> logger.info( "Error adding document" + e));
                break;
            case COLLECTED:
                logger.info("New measure added to current location");
                break;
            case ERROR:
                logger.info("An error has occurred during the request management");
        }

    }

    public void insertInfectedLocations(List<BaseLocation> locationList){
        locationList.forEach( location -> collection
                    .add(new ExtLocation(location.getLocation()).setInfected())
                    .addOnSuccessListener(documentReference -> logger.info("DocumentSnapshot added with ID: " + documentReference.getId()))
                    .addOnFailureListener(e -> logger.info( "Error adding document" + e)));
    }

    public RetStatus<List<String>> getNearLocations(GeoPoint location, double radiusInM){
        final GeoLocation center = new GeoLocation(location.getLatitude(), location.getLongitude());

        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds b : bounds) {
            Query q = collection
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            tasks.add(q.get());
        }

        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Task<?>>> t) {
                        List<ExtLocation> locations = new ArrayList<>();

                        for (Task<QuerySnapshot> task : tasks) {
                            QuerySnapshot snap = task.getResult();
                            assert snap != null;
                            for (DocumentSnapshot doc : snap.getDocuments())
                                locations.add(new ExtLocation(
                                        doc.getGeoPoint("location"),
                                        doc.getDate("expire"),
                                        doc.getBoolean("infected"),
                                        doc.getLong("criticity"),
                                        doc.getString("geohash")));
                        }

                        logger.info("Near Location collected: " + locations.size() + " data points obtained");
                    }
                });
        return null;
    }

    public OpStatus dropExpiredLocations(){

        try {
            WriteBatch writeBatch = firestore.batch();

            collection.get().addOnSuccessListener((querySnapshot) -> {

                querySnapshot.forEach((locationDoc) -> {

                    if (Objects.requireNonNull(locationDoc.getDate("expire")).after(new Date()))
                        writeBatch.delete(Objects.requireNonNull(locationDoc.getReference()));

                });

            });

            writeBatch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    assert false;
                    logger.info("Firestore consolidation completed " + aVoid.toString());
                }
            });
            return OpStatus.OK;

        }catch(RuntimeException e){
            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }
}

