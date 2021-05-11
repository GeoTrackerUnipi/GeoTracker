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
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class KeyValueManagement extends Service {

    //  class used to pass the key-value service by a Binder
    public class LocalBinder extends Binder {
        public KeyValueManagement getService() {
            return KeyValueManagement.this;
        }
    }

    private final IBinder classBinder = new LocalBinder();  // maintains a reference to a global service class
    public final SignatureUtility signatures = new SignatureUtility(Paper.book("signatures"));
    public final PositionUtility positions = new PositionUtility(Paper.book("positions"));
    public final BeaconUtility beacons = new BeaconUtility(Paper.book("beacons"));
    public final BucketUtility buckets = new BucketUtility(Paper.book("buckets"));

    @Override
    public void onCreate(){
        super.onCreate();

        //  initialization of Paper key-value database
        Paper.init(getBaseContext());

        //  launch the DatabaseConsolidator worker
        WorkManager
                .getInstance(this.getBaseContext())
                .enqueueUniquePeriodicWork(                 //  only one worker which will be called periodically
                        "consolidator",       //  name assigned to worker
                          ExistingPeriodicWorkPolicy.KEEP,  //  if a worker is already present discharge the new worker
                          new PeriodicWorkRequest.Builder(     //  worker will be called 1 time per hour
                                  DatabaseConsolidator.class,
                                  1, TimeUnit.HOURS
                          )
                .setConstraints(         //  worker will be called only when the device is idle
                        new Constraints
                                .Builder()
                                .setRequiresDeviceIdle(true)
                                .build()
                ).build());

    }

    @Override
    public IBinder onBind(Intent intent) {

        return classBinder;

    }

}

