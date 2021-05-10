package com.geotracer.geotracer.db.local;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import io.paperdb.Paper;

import androidx.annotation.RequiresApi;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.geotracer.geotracer.db.DatabaseConsolidator;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.data.ExtSignature;
import com.geotracer.geotracer.utils.data.Signature;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class KeyValueManagement extends Service {

    public class LocalBinder extends Binder {

        public KeyValueManagement getService() {
            return KeyValueManagement.this;
        }

    }


    private final IBinder classBinder = new LocalBinder();
    private final Logger logger = Logger.getGlobal();

    @Override
    public void onCreate(){
        super.onCreate();
        Paper.init(this.getBaseContext());

        Constraints.Builder constraint = new Constraints.Builder();
        constraint.setRequiresDeviceIdle(true);

        WorkManager
                .getInstance(this.getBaseContext())
                .enqueueUniquePeriodicWork(
                        "consolidator",
                          ExistingPeriodicWorkPolicy.KEEP,
                          new PeriodicWorkRequest.Builder(DatabaseConsolidator.class, 1, TimeUnit.HOURS)
                .setConstraints(constraint.build())
                .build());

    }

    @Override
    public IBinder onBind(Intent intent) {

        return classBinder;

    }

    ////// SIGNATURES

    //  insert a new signature if it is not present
    //  Returns:
    //      - OpStatus.OK: signature correctly inserted
    //      - OpStatus.PRESENT: signature already present, not inserted
    //      - OpStatus.ERROR: an error has occurred during the request
    
    public OpStatus insertSignature(Signature signature){

        try {

            if (Paper.book("signatures" ).contains(signature.getSignature()))
                return OpStatus.PRESENT;

            logger.info( "Putting new signature: "+signature.getSignature());
            Paper.book("signatures").write(signature.getSignature(), signature.toString());
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    //  get all the valid registered signatures
    //  Returns:
    //      - OpStatus.OK: all the valid signatures are given with the object
    //      - OpStatus.EMPTY: the operation went well but no signature is present
    //      - OpStatus.ERROR: an error has occurred during the request

    public RetStatus<List<Signature>> getAllSignatures(){

        try{


            List<Signature> signatures = new ArrayList<>();
            Paper.book("signatures").getAllKeys().forEach(
                    s -> signatures.add(new Signature(Paper.book("signatures").read(s))));
            Collections.sort(signatures);
            logger.info( "Getting all the signatures. Number of signatures: "+signatures.size());
            for( int a = 0; a<signatures.size(); a++)
                if( !signatures.get(a).isExpired()){
                    logger.info( "Effective signatures: " + (signatures.size()-a));
                    if( a > 0)
                        return new RetStatus<>(signatures.subList(a, signatures.size()), OpStatus.OK);
                    else
                        return new RetStatus<>(signatures, OpStatus.OK);
                    }
            logger.info( "Effective signatures: 0");
            return new RetStatus<>(new ArrayList<>(),OpStatus.EMPTY);

        }catch(RuntimeException e){

            e.printStackTrace();
            return new RetStatus<>(null,OpStatus.ERROR);

        }
    }

    public OpStatus removeAllSignatures(){

        try{

            Paper.book("signatures").destroy();
            return OpStatus.OK;

        }catch(RuntimeException e){
            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }
    ////// USER POSITIONS

    //  insert a new user position
    //  Returns:
    //      - OpStatus.OK: all the valid signatures are given with the object
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus insertPosition( BaseLocation location ){

        String stringTag = location.getLocation().toString();
        try{

            if (Paper.book("positions" ).contains(stringTag)) {

                Paper.book("positions").delete(stringTag);
                logger.info("Updating position: " + stringTag );

            }else
                logger.info( "Putting new position " + stringTag );

            Paper.book("positions").write(stringTag, location.toString());
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }

    //  returns all the valid positions inserted by the user
    //  Returns:
    //      - OpStatus.OK: all the valid signatures are given with the object
    //      - OpStatus.EMPTY: the operation went well but no signature is present
    //      - OpStatus.ERROR: an error has occurred during the request

    public RetStatus<List<BaseLocation>> getAllPositions(){
        try{

            List<BaseLocation> positions = new ArrayList<>();
            Gson gson = new Gson();
            Paper.book("positions").getAllKeys().forEach(s->positions.add(gson.fromJson(s,BaseLocation.class)));
            if( positions.size() == 0 )
                return new RetStatus<>(new ArrayList<>(),OpStatus.EMPTY);
            logger.info( "Getting all the positions. Number of positions: "+positions.size());
            Collections.sort(positions);
            for( int a = 0; a<positions.size(); a++)
                if( !positions.get(a).isExpired()) {
                    logger.info( "Effective positions: " + (positions.size()-a));
                    if (a > 0)
                        return new RetStatus<>(positions.subList(a, positions.size()), OpStatus.OK);
                    else
                        return new RetStatus<>(positions, OpStatus.OK);
                }
            logger.info( "Effective positions: 0");
            return new RetStatus<>(new ArrayList<>(),OpStatus.EMPTY);

        }catch(RuntimeException e){
            e.printStackTrace();
            return new RetStatus<>(null, OpStatus.ERROR);
        }
    }

    ////// BEACONS

    //  insert a new captured position
    //  Returns:
    //      - OpStatus.OK: all the valid signatures are given with the object
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus insertBeacon(ExtSignature beacon){

        String beaconString = beacon.toString();
        String beaconTag = beacon.getSignature();

        try {

            if (Paper.book("beacons").contains(beaconTag)) {
                if(new ExtSignature(Paper.book("beacons").read(beaconTag)).toBeUpDated(beacon.getDistance())) {
                    Paper.book("beacons").delete(beaconTag);
                    logger.info("Updating beacon " + beaconTag);
                }else
                    return OpStatus.OK;
            }else
                logger.info( "Putting new beacon " + beaconTag );
            Paper.book("beacons").write(beaconTag, beaconString);

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }

        return OpStatus.OK;

    }

    //  verify if a beacon is present
    //  Returns:
    //      - OpStatus.PRESENT: the beacon is present inside the database
    //      - OpStatus.NOT_PRESENT: the beacon is not present inside the database
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus beaconPresent(String beacon){

        try {
            return Paper
                    .book("beacons")
                    .contains(beacon) && !new ExtSignature(Paper.book("beacons").read(beacon)).isExpired() ?
                    OpStatus.PRESENT : OpStatus.NOT_PRESENT;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    ////// BUCKETS

    //  insert a new bucket for message notifications
    //  Returns:
    //      - OpStatus.OK: the bucket is added to the store
    //      - OpStatus.PRESENT: the bucket is already present inside the store
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus insertBucket(String bucket){

        try {

            if (Paper.book("buckets").contains(bucket)) {

                return OpStatus.PRESENT;

            }
            logger.info( "Putting new bucket " + bucket );

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, 14);
            Paper.book("buckets").write(bucket, calendar.getTime());

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }

        return OpStatus.OK;
    }

    //  remove a registered bucket
    //  Returns:
    //      - OpStatus.OK: the bucket is removed from the store
    //      - OpStatus.NOT_PRESENT: the bucket is not present inside the store
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus removeBucket(String bucket){

        try {
            if( Paper.book("buckets").contains(bucket)) {
                logger.info("Removing bucket: " + bucket);
                Paper.book("buckets").delete(bucket);
            }else
                return OpStatus.NOT_PRESENT;
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    //  returns all the saved buckets
    //  Returns:
    //      - OpStatus.OK: return all the saved bucket
    //      - OpStatus.ERROR: an error has occurred during the request

    public RetStatus<List<String>> getBuckets(){

        try {

            return new RetStatus<>(Paper.book("buckets").getAllKeys(),OpStatus.OK);

        }catch(RuntimeException e){

            e.printStackTrace();
            return new RetStatus<>(null, OpStatus.ERROR);

        }
    }
}

