package com.geotracer.geotracer.db.local;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.annotation.RequiresApi;
import java.util.concurrent.TimeUnit;
import androidx.work.Constraints;
import androidx.work.WorkManager;
import android.content.Intent;
import android.app.Service;
import android.os.IBinder;
import android.os.Binder;
import android.os.Build;
import io.paperdb.Paper;

//   KEY-VALUE MANAGEMENT
//   Class to manage the access to a local key-value database. The service is composed by several
//   subclasses to better improve usability of the class giving a more logical separation between
//   the function provided by the module
//          - .signatures: functions to operate on user signatures
//          - .positions: functions to operate on user's positions
//          - .beacons: functions to operate on other application's signatures
//          - .buckets: functions to add/remove bucket from which receive notifications


@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class KeyValueManagement extends Service {

    //  class used to pass the key-value service by a Binder
    public class LocalBinder extends Binder {
        public KeyValueManagement getService() {
            return KeyValueManagement.this;
        }
    }

    private final IBinder classBinder = new LocalBinder();  // maintains a reference to a global service class
    public SignatureUtility signatures = null;   //  functions to operate on signatures
    public PositionUtility positions = null;       //
    public BeaconUtility beacons = null;
    public BucketUtility buckets = null;

    @Override
    public void onCreate(){
        super.onCreate();

        //  initialization of Paper key-value database
        Paper.init(getBaseContext());

        signatures = new SignatureUtility(Paper.book("signatures"));   //  functions to operate on signatures
        positions = new PositionUtility(Paper.book("positions"));       //
        beacons = new BeaconUtility(Paper.book("beacons"));
        buckets = new BucketUtility(Paper.book("buckets"));
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

