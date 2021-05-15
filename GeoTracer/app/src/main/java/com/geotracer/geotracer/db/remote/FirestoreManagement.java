package com.geotracer.geotracer.db.remote;

import com.google.firebase.firestore.CollectionReference;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.geotracer.geotracer.utils.data.ExtLocation;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import static android.content.ContentValues.TAG;
import com.google.firebase.firestore.GeoPoint;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.Task;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.FirebaseApp;
import android.content.Intent;
import java.util.ArrayList;
import android.app.Service;
import android.os.IBinder;
import java.util.Objects;
import android.os.Binder;
import android.util.Log;
import java.util.Date;
import java.util.List;


//// FIRESTORE MANAGEMENT
//   The class is in charge of store Location values into the remote Firestore database and
//   retrieving it by geo-queries. It also gives a testing method to clean the expired values inside
//   the database, this is very inefficient but it's just a testing method to overcome the paywall of
//   implementing functions directly inside the Firebase Firestore cloud service

@SuppressWarnings("unused")
public class FirestoreManagement extends Service {

    private FirebaseFirestore firestore;
    private CollectionReference collection;
    private static final LocationAggregator aggregator = new LocationAggregator();

    FirestoreCallback callback;

    //  callback function to obtain asynchronously the data from firestore
    public interface FirestoreCallback {
        void onDataCollected(List<ExtLocation> location);
    }

    //  binder for giving the Service class
    public class LocalBinder extends Binder {

        public FirestoreManagement getService() {
            return FirestoreManagement.this;
        }

    }

    private final IBinder classBinder = new LocalBinder();

    @Override
    public void onCreate(){
        FirebaseApp.initializeApp(getBaseContext());
        firestore = FirebaseFirestore.getInstance();
        collection = firestore.collection("geotraces");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        firestore.terminate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return classBinder;
    }

    //  function for pushing location values inside the firestore
    public void insertLocation(String ID, BaseLocation location){

        //  we aggregate the value provided
        RetStatus<ExtLocation> status = aggregator.insertValue(ID, location);

        //  basing on the result of the aggregating operation we choose a reaction
        switch(status.getStatus()){
            case OK:  // aggregation of a value completed, is needed to push it into the database
                collection
                        .add(status.getValue())
                        .addOnSuccessListener(documentReference -> Log.d(TAG,"New document inserted into Firestore: " + documentReference.getId()))
                        .addOnFailureListener(e -> Log.d(TAG,"Error adding a document" + e));
                break;

            case COLLECTED:  //  given value aggregated
                Log.d(TAG,"New measure aggregated");
                break;

            case PRESENT:    //  given value already aggregated
                Log.d(TAG,"Data already aggregated. Operation rejected");
                break;

            default:         //  errors
                Log.d(TAG,"An error has occurred during the request management");
        }

    }

    //  function used when a user is infected, it floods all the user location of the last 2 weeks into
    //  the database in order to update the heatmap
    public void insertInfectedLocations(List<BaseLocation> locationList){

        Log.d(TAG, "User infected. Starting flooding of infected locations");
        locationList.forEach( location -> collection
                    .add(new ExtLocation(location.getLocation()).setInfected())
                    .addOnSuccessListener(documentReference -> Log.d(TAG,"Infected location stored inside firestore: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.d(TAG, "Error adding a document" + e)));

    }

    //  TODO To be removed
    public void testInsertLocation(ExtLocation location){
        collection
                .add(location)
                .addOnSuccessListener(documentReference -> Log.d(TAG,"New document inserted into Firestore: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.d(TAG,"Error adding a document" + e));
    }

    //  function used to collect data to generate a heatmap. It requires a location which will be the center
    //  of a circle with a certain radious. The function will return all the points inside the circle
    public FirestoreCallback getNearLocations(GeoPoint location, double radiusInM){

        final GeoLocation center = new GeoLocation(location.getLatitude(), location.getLongitude());
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();   // lists for all the queries

        //  generating the bounds used for the geo-query and for every bound we create a query
        //  in order to collect all the data placed between the bounds
        GeoFireUtils.getGeoHashQueryBounds(center, radiusInM).forEach(
                bound -> tasks.add( collection
                        .orderBy("geoHash")
                        .startAt(bound.startHash)
                        .endAt(bound.endHash).get()));

        //  when all the data are ready
        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener( t -> {
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
                    Log.d(TAG,"Near Location collected: " + locations.size() + " data points obtained");
                   // callback.onDataCollected(locations);
                    Log.d(TAG, locations.toString());

                });

        return this.callback;
    }

    // [testing function] removes all the expired data from the database. This is very inefficient
    // but it's just a testing method to overcome the paywall of implementing functions directly
    // inside the Firebase Firestore cloud service
    public OpStatus dropExpiredLocations(){

        try {

            //  we create a batch operation which will execute all the requested operation in a
            //  asynchronous way
            WriteBatch writeBatch = firestore.batch();

            firestore.collection("buckets").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                        collection.get().addOnSuccessListener((querySnapshot) -> querySnapshot.forEach((locationDoc) -> {

                            if (Objects.requireNonNull(locationDoc.getDate("expire")).after(new Date()))
                                writeBatch.delete(Objects.requireNonNull(locationDoc.getReference()));

                        }));
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            });

            //  if a data is expired then we set a delete operation on the batch
            collection.get().addOnSuccessListener((querySnapshot) -> querySnapshot.forEach((locationDoc) -> {

                if (Objects.requireNonNull(locationDoc.getDate("expire")).after(new Date()))
                    writeBatch.delete(Objects.requireNonNull(locationDoc.getReference()));

            }));

            //  we execute all the settled operation
            writeBatch.commit().addOnSuccessListener(aVoid -> {
                assert false;
                Log.d(TAG,"Firestore consolidation completed " + aVoid.toString());
            });
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }
}

