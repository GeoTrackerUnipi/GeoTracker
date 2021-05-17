package com.geotracer.geotracer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.List;
import java.util.logging.Logger;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.geotracer.geotracer.db.remote.FirestoreManagement;
import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.mainapp.MainActivity;
import com.geotracer.geotracer.notifications.NotificationSender;
import com.geotracer.geotracer.settingapp.SettingActivity;
import com.geotracer.geotracer.testingapp.LogService;
import com.geotracer.geotracer.testingapp.TestingActivity;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.data.ExtLocation;
import com.geotracer.geotracer.utils.data.ExtSignature;
import com.geotracer.geotracer.utils.data.Signature;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;

public class UsageTestActivity extends AppCompatActivity {

        boolean bound;
        BroadcastReceiver onNotice;
        private KeyValueManagement keyValueStore;
        private NotificationSender notificationSender;
        private FirestoreManagement firestore;

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
                bound = true;

            }

          @Override
            public void onServiceDisconnected(ComponentName arg0) {

                notificationSender = null;
                bound = false;

            }
        };


    private final ServiceConnection firestoreService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            FirestoreManagement.LocalBinder binder = (FirestoreManagement.LocalBinder) service;
            firestore = binder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            firestore = null;

        }
    };

        @Override
        protected void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);

            setContentView(R.layout.testing_main);
            Intent intent = new Intent(this, KeyValueManagement.class);
            bindService(intent, keyValueService, Context.BIND_AUTO_CREATE);

            intent = new Intent(this, NotificationSender.class);
            bindService(intent, notificationService, Context.BIND_AUTO_CREATE);

            intent = new Intent(this, FirestoreManagement.class);
            bindService(intent, firestoreService, Context.BIND_AUTO_CREATE);

            logger.info("[TEST] KeyValueDb Service started");

            LocalBroadcastManager.getInstance(UsageTestActivity.this).registerReceiver(
                    onNotice = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {

                            Log.i(this.getClass().getName(), "BROADCAST LISTENER FOR CONTACTS");
                            String toLog = intent.getStringExtra("Contact");

                            TextView tv = new TextView(UsageTestActivity.this);
                            if(tv == null)
                                Log.d(this.getClass().getName() + "BROADCAST LISTENER FOR CONTACTS", "Empty location");
                            else
                                showPopupWindow(tv, toLog);

                            FrameLayout frameLayout = findViewById(R.id.contact_frame);
                            frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
                            TextView contact_text = findViewById(R.id.contact_text);
                            contact_text.setText(getResources().getString(R.string.contacts));
                            ((UserStatus) UsageTestActivity.this.getApplication()).setContacts(true);
                        }
                    },new IntentFilter(LogService.ACTION_BROADCAST)

            );



        }


    @Override
    protected void onStop() {
        super.onStop();
        unbindService(notificationService);
        bound = false;
    }

    protected void onResume() {
        super.onResume();

        IntentFilter iff= new IntentFilter(NotificationSender.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, iff);

        if(((UserStatus) this.getApplication()).getContacts()) {
            FrameLayout frameLayout = findViewById(R.id.contact_frame);
            frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
            TextView tv = findViewById(R.id.contact_text);
            tv.setText(getResources().getString(R.string.contacts));
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onNotice);
    }


    @Override
        protected void onDestroy(){
            super.onDestroy();
            unbindService(keyValueService);
        }

        public void insertBeacon(View view){

            String beacon = ((EditText)findViewById(R.id.beacon_add)).getText().toString();
            OpStatus status = keyValueStore.beacons.insertBeacon(new ExtSignature(beacon,10));
            Log.println(Log.INFO,"TEST","Insert Beacon Status: " + status.toString());
        }

        public void insertBucket(View view){
            String bucket = ((EditText)findViewById(R.id.bucket_add)).getText().toString();
            keyValueStore.buckets.insertBucket(bucket);
        }

        public void insertSignature(View view){
            String signature = ((EditText)findViewById(R.id.signature_input)).getText().toString();
            OpStatus status = keyValueStore.signatures.insertSignature(new Signature(signature));
            Log.println(Log.INFO,"TEST","Insert Beacon Status: " + status.toString());
        }

        public void insertPosition(View view){
            String latitude = ((EditText)findViewById(R.id.latitude)).getText().toString();
            String longitude = ((EditText)findViewById(R.id.longitude)).getText().toString();
            OpStatus status = keyValueStore.positions.insertOrUpdatePosition(new BaseLocation(new GeoPoint(Float.parseFloat(latitude), Float.parseFloat(longitude))));
            Log.println(Log.INFO,"TEST","Insert Beacon Status: " + status.toString());
        }

        public void insertExtPosition(View view){
            double latitude = Double.parseDouble(((EditText)findViewById(R.id.ext_latitude)).getText().toString());
            double longitude = Double.parseDouble(((EditText)findViewById(R.id.ext_longitude)).getText().toString());
            ExtLocation location = new ExtLocation(new GeoPoint(latitude,longitude));
            if(((RadioButton)findViewById(R.id.insert_ext_loc)).isChecked()){
                location.setCriticity(1);
                location.setInfected();

            }else{
                Integer criticity = Integer.parseInt(((EditText)findViewById(R.id.criticity)).getText().toString());
                location.setCriticity(criticity);
            }

            firestore.testInsertLocation(location);
            Log.println(Log.INFO,"TEST","Insert Beacon Status: " + location.getGeoHash());

        }

        public void getBeacon(View view){
            String beacon = ((EditText)findViewById(R.id.beacon_search)).getText().toString();
            ((TextView)findViewById(R.id.display)).setText(keyValueStore.beacons.beaconPresent(beacon).toString());
        }

        public void getBuckets(View view){
            JSONArray jsonArray = new JSONArray(keyValueStore.buckets.getBuckets().getValue());
            ((TextView)findViewById(R.id.display_buckets)).setText(jsonArray.toString());
        }

        public void getSignatures(View view){
            StringBuilder put = new StringBuilder();
            RetStatus<List<Signature>> ret = keyValueStore.signatures.getAllSignatures();
            if( ret.getStatus() != OpStatus.OK)
                return;
            List<Signature> signatures = ret.getValue();
            for( Signature signature: signatures)
                put.append(signature.getSignature()).append("\n");
            ((TextView)findViewById(R.id.display_signatures)).setText(put);

        }

        public void getPositions(View view){
            StringBuilder put = new StringBuilder();
            RetStatus<List<BaseLocation>> ret = keyValueStore.positions.getAllPositions();
            if( ret.getStatus() != OpStatus.OK)
                return;
            List<BaseLocation> signatures = ret.getValue();
            for( BaseLocation signature: signatures)
                put.append(signature.getLocation()).append("\n");
            ((TextView)findViewById(R.id.display_positions)).setText(put);
        }

        public void getExtLocations(View view){
            double latitude = Double.parseDouble(((EditText)findViewById(R.id.getExtLatitude)).getText().toString());
            double longitude = Double.parseDouble(((EditText)findViewById(R.id.getExtLongitude)).getText().toString());
            ExtLocation location = new ExtLocation(new GeoPoint(latitude,longitude));
            firestore.getNearLocations(new GeoPoint(latitude,longitude),5000);

        }

        public void sendAlert(View view){
            RetStatus<List<BaseLocation>> positions = keyValueStore.positions.getAllPositions();
            notificationSender.infectionAlert();
            if( positions.getStatus() == OpStatus.OK)
                firestore.insertInfectedLocations(positions.getValue());
        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.db_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        TextView tv;
        Intent i;
        switch(item.getItemId()){
            case R.id.from_DB_to_main:
                i = new Intent(this, MainActivity.class);
                startActivity(i);
                return true;
            case R.id.from_DB_to_testing:
                i = new Intent(this, TestingActivity.class);
                startActivity(i);
                return true;
            case R.id.from_DB_to_settings:
                i = new Intent(this, SettingActivity.class);
                startActivity(i);
                return true;
            case R.id.from_DB_to_info:
                i = new Intent(this, InfoActivity.class);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    protected void showPopupWindow(TextView location, String message){

        //instantiate the popup.xml layout file
        LayoutInflater layoutInflater = (LayoutInflater) UsageTestActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //it is used to take the resources from the popup.xml file
        View customView = layoutInflater.inflate(R.layout.popup,null);

        Button closePopupBtn = (Button) customView.findViewById(R.id.closePopupBtn);

        //instantiate popup window
        PopupWindow popupWindow = new PopupWindow(customView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        //display the popup window
        popupWindow.showAtLocation(location, Gravity.CENTER, 0, 0);


        TextView popup_view = (TextView) customView.findViewById(R.id.popup_text);
        popup_view.setText(message);

        //close the popup window on button click
        closePopupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });



    }


}
