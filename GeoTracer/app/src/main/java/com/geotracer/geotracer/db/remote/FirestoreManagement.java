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

@SuppressWarnings("all")
public class FirestoreManagement extends Service {

    private FirebaseFirestore firestore;
    private CollectionReference collection;
    private static final LocationAggregator aggregator = new LocationAggregator();

    FirestoreCallback callback;

    //  callback function to obtain asynchronously the data from firestore
    public interface FirestoreCallback {
        void onSuccess();
        void onFailure();
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
    public OpStatus insertLocation(String ID, BaseLocation location){

        if( ID == null || ID.length() == 0 || location == null)
            return OpStatus.ILLEGAL_ARGUMENT;

        //  we aggregate the value provided
        RetStatus<ExtLocation> status = aggregator.insertValue(ID, location);


        //  basing on the result of the aggregating operation we choose a reaction
        switch(status.getStatus()){
            case OK:  // aggregation of a value completed, is needed to push it into the database
                try {
                    if( status.getValue().getCriticity() != 0)
                        collection
                            .add(status.getValue())
                            .addOnSuccessListener(documentReference -> Log.d(TAG, "New document inserted into Firestore: " + documentReference.getId()))
                            .addOnFailureListener(e -> Log.d(TAG, "Error adding a document" + e));
                }catch( RuntimeException e){
                    e.printStackTrace();
                    return OpStatus.ERROR;
                }
                break;

            case COLLECTED:  //  given value aggregated
                Log.d(TAG,"New measure aggregated");
                break;

            case PRESENT:    //  given value already aggregated
                Log.d(TAG,"Data already aggregated. Operation rejected");
                break;

            default:         //  errors
                Log.d(TAG,"An error has occurred during the request management");
                return status.getStatus();
        }

        return OpStatus.OK;
    }

    //  function used when a user is infected, it floods all the user location of the last 2 weeks into
    //  the database in order to update the heatmap
    //      - OpStatus.OK: infected location correctly inserted
    //      - OpStatus.ERROR: some error happened during function execution
    public OpStatus insertInfectedLocations(List<BaseLocation> locationList){

        Log.d(TAG, "User infected. Starting flooding of infected locations");
        try {
            locationList.forEach(location -> collection
                    .add(new ExtLocation(location.getLocation()).setInfected())
                    .addOnSuccessListener(documentReference -> Log.d(TAG, "Infected location stored inside firestore: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.d(TAG, "Error adding a document" + e)));
            return OpStatus.OK;
        }catch(RuntimeException e){
            e.printStackTrace();
            return OpStatus.ERROR;
        }

    }

    //  TODO To be removed
    public OpStatus testInsertLocation(ExtLocation location){
        try {
            collection
                    .add(location)
                    .addOnSuccessListener(documentReference -> Log.d(TAG, "New document inserted into Firestore: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.d(TAG, "Error adding a document" + e));
            return OpStatus.OK;
        }catch(RuntimeException e){
            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }

    //  function used to collect data to generate a heatmap. It requires a location which will be the center
    //  of a circle with a certain radious. The function will return all the points inside the circle
    // Returns:
    //      - OpStatus.ILLEGAL_ARGUMENT: invalid arguments provided to the class
    //      - OpStatus.OK: near locations correctly collected
    //      - OpStatus.ERROR: some error happened during function execution
    public OpStatus getNearLocations(GeoPoint location, double radiusInM){

        if( location == null || radiusInM <= 0 )
            return OpStatus.ILLEGAL_ARGUMENT;

        final GeoLocation center = new GeoLocation(location.getLatitude(), location.getLongitude());
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();   // lists for all the queries

        try {
            //  generating the bounds used for the geo-query and for every bound we create a query
            //  in order to collect all the data placed between the bounds
            GeoFireUtils.getGeoHashQueryBounds(center, radiusInM).forEach(
                    bound -> tasks.add(collection
                            .orderBy("geoHash")
                            .startAt(bound.startHash)
                            .endAt(bound.endHash).get()));

            //  when all the data are ready
            Tasks.whenAllComplete(tasks)
                    .addOnCompleteListener(t -> {
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
                        Log.d(TAG, "Near Location collected: " + locations.size() + " data points obtained");
                        // callback.onDataCollected(locations);
                        Log.d(TAG, locations.toString());

                    });
            return OpStatus.OK;

        }catch( RuntimeException e ){
            e.printStackTrace();
            return OpStatus.ERROR;
        }

    }

    // [testing function] removes all the expired heatmap data from the database. This is very inefficient
    // but it's just a testing method to overcome the paywall of implementing functions directly
    // inside the Firebase Firestore cloud service
    // Returns:
    //      - OpStatus.OK: firestore database correctly consolidated
    //      - OpStatus.ERROR: some error happened during function execution
    public OpStatus dropExpiredLocations(){

        try {

            collection.whereLessThan("expire", new Date()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    WriteBatch batch = firestore.batch();
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult()))
                        batch.delete(document.getReference());
                    batch.commit().addOnSuccessListener(s -> Log.d(TAG, "Geotraces correctly consolidated"));
                }
            });

            Log.d(TAG, "Starting consolidation of stored notifications");
            dropExpiredNotifications();
            return OpStatus.OK;

        }catch(RuntimeException e){
            e.printStackTrace();
            return OpStatus.ERROR;
        }

    }

    // [testing function] removes all the expired notification data from the database. This is very inefficient
    // but it's just a testing method to overcome the paywall of implementing functions directly
    // inside the Firebase Firestore cloud service
    // Returns:
    //      - OpStatus.OK: all the notification buckets correctly consolidated
    //      - OpStatus.ERROR: some error happened during function execution
    public OpStatus dropExpiredNotifications(){

        try {

            //  from a dedicated collection we identify all the stored buckets
            firestore.collection("buckets").get().addOnCompleteListener(task -> {
                if( task.getResult() != null)
                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult())
                        //  to every bucket we initialize the consolidation
                        dropExpiredNotificationBucket( queryDocumentSnapshot.getString("bucket"));
            });
            return OpStatus.OK;

        }catch(RuntimeException e){
            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }

    // [testing function] support function to dropExpiredNotification. Removes all the expired notification
    // data from a single given bucket. This is very inefficient but it's just a testing method to overcome
    // the paywall of implementing functions directly inside the Firebase Firestore cloud service
    // Returns:
    //      - OpStatus.ILLEGAL_ARGUMENT: invalid arguments provided to the class
    //      - OpStatus.OK: single notification bucket correctly consolidated
    //      - OpStatus.ERROR: some error happened during function execution
    private OpStatus dropExpiredNotificationBucket(String bucket){

        if( bucket == null || bucket.length() == 0 )
            return OpStatus.ILLEGAL_ARGUMENT;

        try{

            //  we collect all the data that have an expired timestamp
            firestore.collection(bucket).whereLessThan("expire", new Date()).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // we create a batch to remove in background all the collected data
                    WriteBatch writeBatch = firestore.batch();
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult()))
                        writeBatch.delete(document.getReference());

                    writeBatch.commit().addOnSuccessListener(s -> Log.d(TAG, "Bucket " + bucket + " correclty consolidated"));
                }

            });
            return OpStatus.OK;

        }catch( RuntimeException e ){
            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }
}

