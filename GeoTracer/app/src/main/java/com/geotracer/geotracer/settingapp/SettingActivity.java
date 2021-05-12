package com.geotracer.geotracer.settingapp;

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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.geotracer.geotracer.db.remote.FirestoreManagement;
import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.mainapp.MainActivity;
import com.geotracer.geotracer.notifications.NotificationSender;
import com.geotracer.geotracer.testingapp.LogService;
import com.geotracer.geotracer.testingapp.TestingActivity;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.data.Signature;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;

import java.util.List;

public class SettingActivity extends AppCompatActivity {


    NotificationSender notificationSender;
    boolean boundNotification;
    BroadcastReceiver onNotice;
    FirestoreManagement firestoreManagement;
    private KeyValueManagement keyValueStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        /*

        BROADCAST LISTENER FOR CONTACTS
         */

        LocalBroadcastManager.getInstance(SettingActivity.this).registerReceiver(
                onNotice = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        Log.d(this.getClass().getName(), "BROADCAST LISTENER FOR CONTACTS");
                        String toLog = intent.getStringExtra("Contact");


                        TextView tv = new TextView(SettingActivity.this);
                        if(tv == null)
                            Log.d(this.getClass().getName() + "BROADCAST LISTENER FOR CONTACTS", "Empty location");
                        else
                            showPopupWindow(tv, toLog);
                    }
                },new IntentFilter(LogService.ACTION_BROADCAST)

        );

        /*

        LISTENER FOR SWITCH BUTTONS
         */

        Switch contact_notifications = findViewById(R.id.notification_contact);
        contact_notifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //ENABLE THE RECEIVING OF NOTIFICATION FOR HAVING MET SOMEONE POSITIVE TO COVID19
                    Log.d(this.getClass().getName(), "Contact Notification Enabled");
                }else{
                    //DISABLE THE RECEIVING OF NOTIFICATION FOR HAVING MET SOMEONE POSITIVE TO COVID19
                    Log.d(this.getClass().getName(), "Contanct Notification Disabled");
                }
            }
        });
        Switch proximity_notifications = (Switch) findViewById(R.id.notification_proximity);
        proximity_notifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //ENABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE
                    Log.d(this.getClass().getName(), "Proximity Notification Enabled");
                }else{
                    //DISABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE
                    Log.d(this.getClass().getName(), "Proximity Notification Disabled");
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
                        Log.d(this.getClass().getName(), "Positivity Report Enabled");
                    }

                }else{
                    //DISABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE
                    Log.d(this.getClass().getName(), "Positivity Report Disabled");
                }
            }
        });


    }


    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, NotificationSender.class);
        bindService(intent, notificationService, Context.BIND_AUTO_CREATE);

        intent = new Intent(this, KeyValueManagement.class);
        bindService(intent, keyValueService, Context.BIND_AUTO_CREATE);
        intent = new Intent(this, FirestoreManagement.class);
        bindService(intent, firestoreService, Context.BIND_AUTO_CREATE);
    }

    protected void onResume() {
        super.onResume();

        IntentFilter iff= new IntentFilter(NotificationSender.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, iff);
    }

    @Override
    protected void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onNotice);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(notificationService);
        unbindService(keyValueService);
        unbindService(firestoreService);
        boundNotification = false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection notificationService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder s) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationSender.LocalBinder binder = (NotificationSender.LocalBinder) s;
            notificationSender = binder.getService();
            boundNotification = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            boundNotification = false;
        }
    };



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

    /*

        MENU BAR

     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.setting_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        TextView tv;
        Intent i;
        switch(item.getItemId()){
            case R.id.from_setting_to_main:
                i = new Intent(this, MainActivity.class);
                startActivity(i);
                return true;
            case R.id.from_setting_to_testing:
                i = new Intent(this, TestingActivity.class);
                startActivity(i);
                return true;
            case R.id.from_setting_to_info:
                i = new Intent(this, InfoActivity.class);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


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


}