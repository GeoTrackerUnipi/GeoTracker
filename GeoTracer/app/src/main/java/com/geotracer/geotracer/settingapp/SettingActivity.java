package com.geotracer.geotracer.settingapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.UserStatus;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.geotracer.geotracer.db.remote.FirestoreManagement;
import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.mainapp.MainActivity;
import com.geotracer.geotracer.notifications.NotificationSender;
import com.geotracer.geotracer.service.GeotracerService;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;

import java.util.List;

public class SettingActivity extends AppCompatActivity {


    NotificationSender notificationSender;
    BroadcastReceiver notificationBroadcast;
    FirestoreManagement firestoreManagement;
    private KeyValueManagement keyValueStore;
    GeotracerService geotracerMainService;
    public static final String SETTING_ACTIVITY_LOG = "SettingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        /*

        BROADCAST LISTENER FOR CONTACTS
         */

        LocalBroadcastManager.getInstance(SettingActivity.this).registerReceiver(
                notificationBroadcast = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        Log.d(SETTING_ACTIVITY_LOG, "BROADCAST LISTENER FOR CONTACTS");
                        String toLog = intent.getStringExtra("Contact");

                        TextView tv = new TextView(SettingActivity.this);
                        if(tv == null)
                            Log.d(SETTING_ACTIVITY_LOG, "BROADCAST LISTENER FOR CONTACTS: Empty location");
                        else
                            showPopupWindow(tv, toLog);

                        FrameLayout frameLayout = findViewById(R.id.contact_frame);
                        frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
                        TextView contact_text = findViewById(R.id.contact_text);
                        contact_text.setText(getResources().getString(R.string.contacts));
                        ((UserStatus) SettingActivity.this.getApplication()).setContacts(true);
                    }
                },new IntentFilter(NotificationSender.ACTION_BROADCAST)

        );

        /*

        LISTENER FOR SWITCH BUTTONS
         */

        Switch proximity_notifications = (Switch) findViewById(R.id.notification_proximity);
        proximity_notifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //ENABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE

                    /* FIXME */
                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.enableProximityWarnings();
                        if(result)
                            Log.d(SETTING_ACTIVITY_LOG, "Proximity Warning Notifications Enabled");
                        else
                            Log.d(SETTING_ACTIVITY_LOG, "Proximity Warning Notifications Enabled Already Enabled");
                    }
                    else
                        Log.w(SETTING_ACTIVITY_LOG, "Geotracer main service is unbound!");


                }else{
                    //DISABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE

                    /* FIXME */
                    if(geotracerMainService != null)
                    {
                        boolean result = geotracerMainService.disableProximityWarnings();
                        if(result)
                            Log.d(SETTING_ACTIVITY_LOG, "Proximity Warning Notifications Disabled");
                        else
                            Log.d(SETTING_ACTIVITY_LOG, "Proximity Warning Notifications Already Disabled");
                    }
                    else
                        Log.w(SETTING_ACTIVITY_LOG, "Geotracer main service is unbound!");

                    /*
                    disableProximityWarnings()
                     */
                    Log.d(SETTING_ACTIVITY_LOG, "Proximity Notification Disabled");
                }
            }
        });

        Switch positivity_switch = (Switch) findViewById(R.id.positivity_report);
        positivity_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //ENABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE


                    RetStatus<List<BaseLocation>> userPositions = keyValueStore.positions.getAllPositions();
                    if(userPositions.getStatus() == OpStatus.OK){
                        firestoreManagement.insertInfectedLocations(userPositions.getValue());
                        notificationSender.infectionAlert();
                        Log.d(SETTING_ACTIVITY_LOG, "Positivity Report Enabled");
                    }


                }else{
                    //DISABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE
                    Log.d(SETTING_ACTIVITY_LOG, "Positivity Report Disabled");
                }
            }
        });

        initBottomMenu();

    }


    private void initBottomMenu() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.setting_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                TextView tv;
                Intent i;
                switch (item.getItemId()) {
                    case R.id.from_setting_to_main:
                        i = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(i);
                        return true;
                    case R.id.from_setting_to_testing:
                        i = new Intent(getApplicationContext(), TestingActivityOld.class);
                        startActivity(i);
                        return true;
                    case R.id.from_setting_to_info:
                        i = new Intent(getApplicationContext(), InfoActivity.class);
                        startActivity(i);
                        return true;

                    default:
                        return SettingActivity.super.onOptionsItemSelected(item);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to service for contact notifications
        Intent intent = new Intent(this, NotificationSender.class);
        bindService(intent, notificationService, Context.BIND_AUTO_CREATE);

        //Bind service for KeyValue management
        intent = new Intent(this, KeyValueManagement.class);
        bindService(intent, keyValueService, Context.BIND_AUTO_CREATE);

        //Bind service for firestore management
        intent = new Intent(this, FirestoreManagement.class);
        bindService(intent, firestoreService, Context.BIND_AUTO_CREATE);

        //Bind main application service
        intent = new Intent(this, GeotracerService.class);
        bindService(intent, geotracerService, Context.BIND_AUTO_CREATE);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onResume() {
        super.onResume();

        /* FIXME: Questo si assicura che il main service giri (non è un problema se gira già) */
        Intent geoTracerService = new Intent(this,GeotracerService.class);
        startForegroundService(geoTracerService);

        //REGISTRATION TO NOTIFICATION SERVICE
        IntentFilter iff= new IntentFilter(NotificationSender.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationBroadcast, iff);

        //CHECK THE CONTACT STATUS AND SET THE RELATIVE LAYOUT
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
        //DEREGISTER FROM THE NOTIFICATION SERVICE
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationBroadcast);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //UNBIND ALL SERVICES
        if(notificationService != null)
            unbindService(notificationService);

        if(keyValueService != null)
            unbindService(keyValueService);

        if(firestoreService != null)
            unbindService(firestoreService);

        if(geotracerMainService != null)
            unbindService(geotracerService);

    }

    //ESTALISH A CONNECTION WITH NOTIFICATION SERVICE AND DO BIND
    private ServiceConnection notificationService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder s) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationSender.LocalBinder binder = (NotificationSender.LocalBinder) s;
            notificationSender = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            notificationSender = null;
        }
    };



    //BIND TO KEY VALUE MANAGER SERVICE
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

    //BIND TO FIRESTORE MANAGEMENT SERVICE
    private final ServiceConnection firestoreService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            FirestoreManagement.LocalBinder binder = (FirestoreManagement.LocalBinder) service;
            firestoreManagement = binder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            firestoreManagement = null;

        }
    };

    /* FIXME */
    //BIND TO APPLICATION MAIN SERVICE
    private final ServiceConnection geotracerService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            GeotracerService.GeotracerBinder binder = (GeotracerService.GeotracerBinder) service;
            geotracerMainService = binder.getService();

            Switch s = findViewById(R.id.notification_proximity);
            if(geotracerMainService.isProximityNotificationEnabled()){
                //set the switch button to enabled.
                s.setChecked(true);
            }else{
                s.setChecked(false);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            geotracerMainService = null;

        }
    };


    //FUNCTION TO SHOW THE POPUP WINDOW IN CASE CONTACT NOTIFICATION IS ARISEN
    protected void showPopupWindow(TextView location, String message){

        //instantiate the popup.xml layout file
        LayoutInflater layoutInflater = (LayoutInflater) SettingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);

    }



}