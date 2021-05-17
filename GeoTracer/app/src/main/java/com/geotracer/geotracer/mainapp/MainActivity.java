package com.geotracer.geotracer.mainapp;

import android.Manifest;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.UsageTestActivity;

import com.geotracer.geotracer.UserStatus;
import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.notifications.NotificationSender;
import com.geotracer.geotracer.service.GeotracerService;
import com.geotracer.geotracer.settingapp.SettingActivity;
import com.geotracer.geotracer.testingapp.LogService;
import com.geotracer.geotracer.testingapp.TestingActivity;
import com.geotracer.geotracer.utils.generics.OpStatus;


public class MainActivity extends AppCompatActivity {

    NotificationSender notificationSender;
    BroadcastReceiver notificationReceiver;

    public static final String MAIN_ACTIVITY_LOG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                notificationReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        Log.d(MAIN_ACTIVITY_LOG, "BROADCAST LISTENER FOR CONTACTS");
                        String toLog = intent.getStringExtra("Contact");

                        TextView tv = new TextView(MainActivity.this);
                        if(tv == null)
                            Log.d(MAIN_ACTIVITY_LOG, "BROADCAST LISTENER FOR CONTACTS: Empty location");
                        else
                            showPopupWindow(tv, toLog);

                        FrameLayout frameLayout = findViewById(R.id.contact_frame);
                        frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
                        TextView contact_text = findViewById(R.id.contact_text);
                        contact_text.setText(getResources().getString(R.string.contacts));
                        ((UserStatus) MainActivity.this.getApplication()).setContacts(true);
                    }
                },new IntentFilter(NotificationSender.ACTION_BROADCAST)

        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, NotificationSender.class);
        bindService(intent, notificationService, Context.BIND_AUTO_CREATE);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onResume() {
        super.onResume();

        /* Questo è richiesto altrimenti il main service non può partire */
        // Dynamic ACCESS_FINE_LOCATION permission check (required for API 23+)
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
                Toast.makeText(this,"Permission to access the device's location is required for using the service",Toast.LENGTH_SHORT).show();
            else  //Compatibility purposes
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        /* FIXME: Questo si assicura che il main service giri (non è un problema se gira già) */
        Intent geoTracerService = new Intent(this,GeotracerService.class);
        startForegroundService(geoTracerService);


        IntentFilter iff= new IntentFilter(NotificationSender.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, iff);
        if(((UserStatus) this.getApplication()).getContacts()) {
            FrameLayout frameLayout = findViewById(R.id.contact_frame);
            frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
            TextView tv = findViewById(R.id.contact_text);
            tv.setText(getResources().getString(R.string.contacts));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(notificationService != null)
            unbindService(notificationService);
    }

    @Override
    protected void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection notificationService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder s) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationSender.LocalBinder binder = (NotificationSender.LocalBinder) s;
            notificationSender = binder.getService();

            Log.d(MAIN_ACTIVITY_LOG, "AM I INFECTED: " + String.valueOf(notificationSender.canIbeInfected()));
            if(notificationSender.canIbeInfected() == OpStatus.INFECTED){
                FrameLayout frameLayout = findViewById(R.id.contact_frame);
                frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
                TextView contact_text = findViewById(R.id.contact_text);
                contact_text.setText(getResources().getString(R.string.contacts));
                ((UserStatus) MainActivity.this.getApplication()).setContacts(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            notificationService = null;

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        TextView tv;
        Intent i;
        switch(item.getItemId()){
            case R.id.from_main_to_testing:
                i = new Intent(this, TestingActivity.class);
                startActivity(i);
                return true;
            case R.id.from_main_to_settings:
                i = new Intent(this, SettingActivity.class);
                startActivity(i);
                return true;
            case R.id.from_main_to_info:
                i = new Intent(this, InfoActivity.class);
                startActivity(i);
                return true;
            case R.id.from_main_to_db:
                i = new Intent(this, UsageTestActivity.class);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);

    }

    protected void showPopupWindow(TextView location, String message){

        //instantiate the popup.xml layout file
        LayoutInflater layoutInflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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