package com.geotracer.geotracer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;
import java.util.logging.Logger;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.geotracer.geotracer.notifications.NotificationSender;
import com.geotracer.geotracer.utils.data.ExtSignature;
import com.geotracer.geotracer.utils.data.Signature;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.google.firebase.FirebaseApp;

import org.json.JSONArray;

public class UsageTestActivity extends AppCompatActivity {

        private KeyValueManagement keyValueStore;
        private NotificationSender notificationSender;
        private final Logger logger = Logger.getGlobal();

        private final ServiceConnection keyValueService = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                KeyValueManagement.LocalBinder binder = (KeyValueManagement.LocalBinder) service;
                keyValueStore = binder.getService();

            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {

                keyValueStore = null;

            }
        };

        private final ServiceConnection notificationService = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {

                NotificationSender.LocalBinder binder = (NotificationSender.LocalBinder) service;
                notificationSender = binder.getService();

            }

          @Override
            public void onServiceDisconnected(ComponentName arg0) {

                notificationSender = null;

            }
        };

        @Override
        protected void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            FirebaseApp.initializeApp(getApplicationContext());

            setContentView(R.layout.activity_main);
            Intent intent = new Intent(this, KeyValueManagement.class);
            bindService(intent, keyValueService, Context.BIND_AUTO_CREATE);

            intent = new Intent(this, NotificationSender.class);
            bindService(intent, notificationService, Context.BIND_AUTO_CREATE);

            logger.info("[TEST] KeyValueDb Service started");

        }


        @Override
        protected void onDestroy(){
            super.onDestroy();
            unbindService(keyValueService);
        }

        public void insertBeacon(View view){

            String beacon = ((EditText)findViewById(R.id.beacon_add)).getText().toString();
            keyValueStore.insertBeacon(new ExtSignature(beacon,10));
        }

        public void insertBucket(View view){
            String bucket = ((EditText)findViewById(R.id.bucket_add)).getText().toString();
            keyValueStore.insertBucket(bucket);
        }

        public void insertSignature(View view){
            String signature = ((EditText)findViewById(R.id.signature_input)).getText().toString();
            keyValueStore.insertSignature(new Signature(signature));
        }

        public void getBeacon(View view){
            String beacon = ((EditText)findViewById(R.id.beacon_search)).getText().toString();
            ((TextView)findViewById(R.id.display)).setText(keyValueStore.beaconPresent(beacon).toString());
        }

        public void getBuckets(View view){
            JSONArray jsonArray = new JSONArray(keyValueStore.getBuckets().getValue());
            ((TextView)findViewById(R.id.display_buckets)).setText(jsonArray.toString());
        }

        public void getSignatures(View view){
            StringBuilder put = new StringBuilder();
            RetStatus<List<Signature>> ret = keyValueStore.getAllSignatures();
            if( ret.getStatus() != OpStatus.OK)
                return;
            List<Signature> signatures = ret.getValue();
            for( Signature signature: signatures)
                put.append(signature.getSignature()).append("\n");
            ((TextView)findViewById(R.id.display_signatures)).setText(put);

        }

        public void sendAlert(View view){
            notificationSender.infectionAlert();
        }

    }
