package com.geotracer.geotracer.testingapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class LogService extends Service {

    public static final String ACTION_BROADCAST = LogService.class.getName();
    // Binder given to clients
    private final IBinder binder = new LocalBinder();


    public class LocalBinder extends Binder {
        LogService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LogService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(this.getClass().getName(), "Service Created");
    }



    @Override
    public IBinder onBind(Intent intent) {

        Log.d(this.getClass().getName(), "Service Bounded");
        return binder;
    }

    //METHOD USED BY CLIENT TO PRINT LOG MESSAGES
    public void printLog(String class_name, String message){

        Log.d(this.getClass().getName(), "printLog function");
        //SHOW ONLY THE TIME
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        String log = "> [" + class_name + "]" +" [" + sdf.format(ts) + "]: " + message;
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra("LogMessage", log);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}