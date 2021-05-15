package com.geotracer.geotracer.db.local;

import com.geotracer.geotracer.utils.generics.RetStatus;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import static android.content.ContentValues.TAG;
import java.util.Calendar;
import android.util.Log;
import io.paperdb.Book;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


////// BUCKETS
//   A bucket represents a location from which the user listens updated. It is used to receive
//   updated from all the places where the user pass-through
//   Data Format:      BUCKET: expire

@SuppressWarnings("all")
public class BucketUtility {

    private final Book buckets;

    BucketUtility(Book buckets){
        this.buckets = buckets;
    }

    //  insert a new bucket for message notifications
    //  Returns:
    //      - OpStatus.OK: the bucket is added to the store
    //      - OpStatus.PRESENT: the bucket is already present inside the store
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus insertBucket( String bucket ){

        try {

            //  verify the presence of the bucket
            if (buckets.contains(bucket))
                return OpStatus.PRESENT;

            //  add remotely the bucket to the list of available
            addRemoteBucket(bucket);

            // we create an expire time of 14 days
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, 14);

            // bucket not present so we can insert it
            buckets.write(bucket, calendar.getTime());
            Log.d(TAG,"Bucket " + bucket +  " added locally");
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    //  removes a registered bucket
    //  Returns:
    //      - OpStatus.OK: the bucket is correctly removed from the store
    //      - OpStatus.NOT_PRESENT: the bucket is not present inside the store
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus removeBucket(String bucket){

        try {

            //  verifying the bucket is present
            if( buckets.contains(bucket)) {

                //  removing the bucket
                buckets.delete(bucket);
                Log.d(TAG,"Bucket " + bucket +  " removed");
                return OpStatus.OK;

            }else
                return OpStatus.NOT_PRESENT;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    //  returns all the saved buckets
    //  Returns:
    //      - OpStatus.OK: return all the saved buckets
    //      - OpStatus.ERROR: an error has occurred during the request

    public RetStatus<List<String>> getBuckets(){

        try {

            return new RetStatus<>(buckets.getAllKeys(),OpStatus.OK);

        }catch(RuntimeException e){

            e.printStackTrace();
            return new RetStatus<>(null, OpStatus.ERROR);

        }
    }

    //  drop the buckets from the local database
    //  Returns:
    //      - OpStatus.OK: buckets removed
    //      - OpStatus.ERROR: an error has occurred during the buckets removal
    public OpStatus dropAllBuckets(){
        try{

            buckets.destroy();
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    //  registry on the firestore database the bucket. Function required to supply the lack of the
    //  back-end. Helps the database to remove old data knowing the registered buckets without any
    //  remote code execution
    private void addRemoteBucket(String bucket ){

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference collection = firestore.collection("buckets");

        collection.whereEqualTo("bucket", bucket).get().addOnSuccessListener(queryDocumentSnapshots -> {
            //  if the bucket isn't present we insert it into the registry
            if (queryDocumentSnapshots.getDocuments().size() == 0){
                Map<String,String> bucketMap = new HashMap<>();
                bucketMap.put("bucket",bucket);
                collection.add(bucketMap).addOnSuccessListener(documentReference -> Log.d(TAG, "Bucket " + bucket + " added remotely"));
            }
        });
    }
}
