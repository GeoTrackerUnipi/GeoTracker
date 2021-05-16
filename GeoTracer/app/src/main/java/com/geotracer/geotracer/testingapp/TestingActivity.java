package com.geotracer.geotracer.testingapp;

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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.UsageTestActivity;
import com.geotracer.geotracer.UserStatus;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.geotracer.geotracer.db.remote.FirestoreManagement;
import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.mainapp.MainActivity;
import com.geotracer.geotracer.notifications.NotificationSender;
import com.geotracer.geotracer.settingapp.SettingActivity;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class TestingActivity extends AppCompatActivity {


    LogService service;
    boolean boundLog;
    boolean boundNotification;
    BroadcastReceiver notificationReceiver;
    BroadcastReceiver logServiceReceiver;
    NotificationSender notificationSender;
    private FirestoreManagement firestore;
    private KeyValueManagement keyValueManagement;

    public static final String TESTING_ACTIVITY_LOG = "TestingActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);


        TextView tv = (TextView) findViewById(R.id.log_text);
        tv.setText("");
/*
        LocalBroadcastManager.getInstance(TestingActivity.this).registerReceiver(
                logServiceReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        Log.d(TESTING_ACTIVITY_LOG, "BROADCAST LISTENER");
                        String toLog = intent.getStringExtra("LogMessage");
                        tv.append(toLog);
                        ScrollView sv = (ScrollView) findViewById(R.id.scrollview);
                        sv.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                },new IntentFilter(LogService.ACTION_BROADCAST)

        );      */


        LocalBroadcastManager.getInstance(TestingActivity.this).registerReceiver(
                notificationReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        Log.d(TESTING_ACTIVITY_LOG, "BROADCAST LISTENER FOR CONTACTS");
                        String toLog = intent.getStringExtra("Contact");

                        TextView tv = new TextView(TestingActivity.this);
                        if(tv == null)
                            Log.d(TESTING_ACTIVITY_LOG + "BROADCAST LISTENER FOR CONTACTS", "Empty location");
                        else {
                            showPopupWindow(tv, toLog);
                        }

                        FrameLayout frameLayout = findViewById(R.id.contact_frame);
                        frameLayout.setBackgroundColor(getResources().getColor(R.color.red));
                        TextView contact_text = findViewById(R.id.contact_text);
                        contact_text.setText(getResources().getString(R.string.contacts));
                        ((UserStatus) TestingActivity.this.getApplication()).setContacts(true);
                    }
                },new IntentFilter(NotificationSender.ACTION_BROADCAST)

        );

        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (IOException e) {
            // Handle Exception
        }




    }


    private ServiceConnection notificationService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            NotificationSender.LocalBinder binder = (NotificationSender.LocalBinder) service;
            notificationSender = binder.getService();
            boundNotification = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            notificationSender = null;
            boundNotification = false;

        }
    };
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection logServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder s) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LogService.LocalBinder binder = (LogService.LocalBinder) s;
            service = binder.getService();
            boundLog = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            boundLog = false;
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

    private final ServiceConnection keyValueService = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            KeyValueManagement.LocalBinder binder = (KeyValueManagement.LocalBinder) service;
            keyValueManagement = binder.getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            keyValueManagement = null;

        }
    };


    protected void onResume() {
        super.onResume();

        //NOTIFICATION RECEIVER
        IntentFilter iff= new IntentFilter(NotificationSender.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, iff);

        //FIRESTORE MANAGEMENT
        Intent intent = new Intent(this, FirestoreManagement.class);
        bindService(intent, firestoreService, Context.BIND_AUTO_CREATE);

        //KEYVALUE MANAGEMENT
        intent = new Intent(this, KeyValueManagement.class);
        bindService(intent, keyValueService, Context.BIND_AUTO_CREATE);
/*
        iff= new IntentFilter(LogService.ACTION_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(logServiceReceiver, iff);  */

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

        //NOTIFICATION
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);

        //FIRESTORE
        unbindService(firestoreService);

        //KEYVALUE
        unbindService(keyValueService);
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(logServiceReceiver);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, LogService.class);
        bindService(intent, logServiceConnection, Context.BIND_AUTO_CREATE);

        //BIND NOTIFICATION SERVICE
        Intent intent2 = new Intent(this, NotificationSender.class);
        bindService(intent2, notificationService, Context.BIND_AUTO_CREATE);

        //BIND FIRESTORE SERVICE
        intent = new Intent(this, FirestoreManagement.class);
        bindService(intent, firestoreService, Context.BIND_AUTO_CREATE);

        //BIND KEYVALUE SERVICE
        intent = new Intent(this, KeyValueManagement.class);
        bindService(intent, keyValueService, Context.BIND_AUTO_CREATE);

        TextView tv = (TextView) findViewById(R.id.log_text);
        tv.setText("");
    }


    @Override
    protected void onStop() {
        super.onStop();
        unbindService(logServiceConnection);
        unbindService(notificationService);
        boundNotification = false;
        boundLog = false;
    }



    public void startDissemination(View view) {

        /*

            START DISSEMINATING MY SIGNATURE.
            IT MUST E CALLED A FUNCTION WHICH STARTING THE SIGNATURE DISSEMINATION

            THEN THAT PROCESS MUST BE CHECKED
         */

        //if(/*   THE DISSEMINATION STARTED   */){

        String s = "DEVICE STARTED DISSEMINATING ITS SIGNATURE\n";

        TextView tv = new TextView(TestingActivity.this);
        Log.d(TESTING_ACTIVITY_LOG, s);
        showPopupWindow(tv, s);
        //service.printLog(TESTING_ACTIVITY_LOG, s);


        /*

        }else{

        /*

            service.printLog(TESTING_ACTIVITY_LOG, "Error in starting dissemination\n");
        }
        */


    }

    public void stopDissemination(View view) {

        /*

        THIS FUNCTION BLOCK THE PROCESS OF DISSEMINATING MY SIGNATURE
        A FUNCTION THAT STOP THIS PROCESS SHOULD BE CALLED

        BELOW I CHECK THAT EVERYTHING WENT WELL AND I WRITE IT IN A POPUP WINDOW
         */
        //if(/*   THE DISSEMINATION STOPPED   */){

        String s = "DISSEMINATION STOPPED";
        TextView tv = new TextView(TestingActivity.this);
        showPopupWindow(tv, s);
        Log.d(TESTING_ACTIVITY_LOG, s);
        //service.printLog(TESTING_ACTIVITY_LOG, "DEVICE STOPPED DISSEMINATING ITS SIGNATURE\n");



        /*

        }else{
           service.printLog(TESTING_ACTIVITY_LOG, "Error in stop dissemination\n");
        }
        */
    }

    public void startCollection(View view) {

        /*

        START TAKING THE OTHER DEVICES' SIGNATURES
        A FUNCTION THAT DOES THIS MUST BE CALLED

        IN THE IF STATEMENT I HAVE TO CHECK THE RESULT OF THAT CALL
         */
        //if(/*   THE COLLECTION STARTED   */){

        /*

        I SHOW THE COLLECTION INFORMATION IN THE LOG BOX

         */
        String s = "SIGNATURE COLLECTION STARTED";
        TextView tv = new TextView(TestingActivity.this);
        showPopupWindow(tv, s);
        Log.d(TESTING_ACTIVITY_LOG, s);
        //service.printLog(TESTING_ACTIVITY_LOG, "DEVICE STARTED COLLECTING SIGNATURES\n");


        /*

        }else{


          service.printLog(TESTING_ACTIVITY_LOG, "Error in starting signature collection\n");

        }
        */
    }

    public void stopCollection(View view) {

        /*

            STOP TAKING THE OTHER DEVICES' SIGNATURES
            THE PROCEDURE RESULT WILL BE SHOWN IN A POPUP WINDOW


         */
        //if(/*   THE COLLECTION STOPPED   */){
        String s = "SIGNATURE COLLECTION STOPPED";
        TextView tv = new TextView(TestingActivity.this);
        showPopupWindow(tv, s);
        Log.d(TESTING_ACTIVITY_LOG, s);
        //service.printLog(TESTING_ACTIVITY_LOG, "DEVICE STOPPED COLLECTING SIGNATURES\n");


        /*

        }else{
            service.printLog(TESTING_ACTIVITY_LOG, "Error in stopping the signature collection\n");

        }
        */
    }

    public void infected(View view) {
        /*
            TAG THE USER AS INFECTED
            THE RESULT WILL BE SHOWN IN A POPUP WINDOW IN ADDITION TO THE LOG SECTION
         */
        TextView tv = new TextView(TestingActivity.this);

        RetStatus<List<BaseLocation>> userPositions = keyValueManagement.positions.getAllPositions();
        if(userPositions.getStatus() == OpStatus.OK){
            firestore.insertInfectedLocations(userPositions.getValue());
            notificationSender.infectionAlert();
            Log.d(TESTING_ACTIVITY_LOG, "POSITIVITY REPORT ENABLED");
            showPopupWindow(tv, "USER TAGGED AS INFECTED");
        }
        else {
            showPopupWindow(tv, "ERROR! USER NOT TAGGED AS INFECTED");
            Log.d(TESTING_ACTIVITY_LOG, "ERROR! USER NOT TAGGED AS INFECTED");
        }

    }


    /*
    IT DELETES THE OLD SIGNATURES FROM THE DB
     */

    public void delete(View view) {

        /*

        PERFORM THE ACTIONS NECESSARY TO THE DELETE FROM THE DB THE DATA OLDER THAN 14 DAYS.
        IF THE TESTING INTERFACE IS AVAILABLE TO EVERYONE I SUGGEST TO DELETE ONLY ITS DATA.
        A POPUP WINDOW WITH THE RESULT OF THE OPERATION WILL BE SHOWN
         */

        if(firestore.dropExpiredLocations()== OpStatus.OK) {
            TextView tv = new TextView(TestingActivity.this);
            showPopupWindow(tv, "Old data has been deleted from the database");
            Log.d(TESTING_ACTIVITY_LOG, "Old data has been deleted from the database");
        }else{
            TextView tv = new TextView(TestingActivity.this);
            showPopupWindow(tv, "Error! Data not deleted");
            Log.d(TESTING_ACTIVITY_LOG, "Error! Old Data not deleted");
        }


    }

    /*
    IT CLEANS THE LOG WINDOW
     */

    public void emptyLog(View view) {
        /*

        THIS FUNCTION DELETE THE CONTENT FROM THE LOG WINDOW

         */
        TextView tv = (TextView) findViewById(R.id.log_text);
        tv.setText("");

        Process logcat;
        final StringBuilder log = new StringBuilder();
        try {
            Runtime.getRuntime().exec("logcat -b all -c");


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
    IT SHOWS THE POPUP WINDOW
     */

    protected void showPopupWindow(TextView location, String message){

        //instantiate the popup.xml layout file
        LayoutInflater layoutInflater = (LayoutInflater) TestingActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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




    /*


    TOOLBAR MENU ITEMS


     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.testing_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Intent i;
        switch(item.getItemId()){
            case R.id.from_testing_to_main:
                i = new Intent(this, MainActivity.class);
                startActivity(i);
                return true;
            case R.id.from_testing_to_settings:
                i = new Intent(this, SettingActivity.class);
                startActivity(i);
                return true;
            case R.id.from_testing_to_info:
                i = new Intent(this, InfoActivity.class);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /*
            SAVE STATE BEFORE CHANGING ACTIVITY

     */



    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {

        super.onSaveInstanceState(outState);
/*
        //save the display status
        String log = ((TextView)findViewById(R.id.log_text)).getText().toString();
        outState.putString("log", log);

        Log.d(TESTING_ACTIVITY_LOG, "Instance State Saved");
*/
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
/*
        //restore the display
        String log = savedInstanceState.getString("log");
        ((TextView)findViewById(R.id.log_text)).setText(log);
        Log.d(TESTING_ACTIVITY_LOG, "Instance State Restored"); */
    }

    /*
    It manages delete menu. It discriminates between the choices and call the corrisponding drop function.
     */
    public void manageDelete(View view) {

        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(TestingActivity.this, view);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.delete_menu, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

                TextView tv = new TextView(TestingActivity.this);
                switch(item.getItemId()){
                    case R.id.delete_my_positions:
                        //DELETE MY POSITIONS
                        if(keyValueManagement.positions.dropAllPositions() != OpStatus.OK)
                            showPopupWindow(tv, "Error in removing positions");
                        else {
                            showPopupWindow(tv, "All positions removed");
                            FrameLayout frameLayout = findViewById(R.id.contact_frame);
                            frameLayout.setBackgroundColor(getResources().getColor(R.color.white));
                            TextView contact_text = findViewById(R.id.contact_text);
                            contact_text.setText(getResources().getString(R.string.contacts));
                            ((UserStatus) TestingActivity.this.getApplication()).setContacts(false);
                        }
                        return true;
                    case R.id.delete_my_signatures:
                        //DELETE MY SIGNATURES
                        if(keyValueManagement.signatures.removeAllSignatures() != OpStatus.OK)
                            showPopupWindow(tv, "Error in removing signatures");
                        else
                            showPopupWindow(tv, "All signatures removed");
                        return true;
                    case R.id.delete_rec_beacons:
                        //DELETE RECEIVED BEACONS
                        if(keyValueManagement.beacons.dropAllBeacons() != OpStatus.OK)
                            showPopupWindow(tv, "Error in removing beacons");
                        else {
                            showPopupWindow(tv, "All beacons removed");
                            FrameLayout frameLayout = findViewById(R.id.contact_frame);
                            frameLayout.setBackgroundColor(getResources().getColor(R.color.white));
                            TextView contact_text = findViewById(R.id.contact_text);
                            contact_text.setText(getResources().getString(R.string.contacts));
                            ((UserStatus) TestingActivity.this.getApplication()).setContacts(false);
                        }
                        return true;
                    case R.id.delete_all:
                        //DELETE EVERYTHING
                        if(!keyValueManagement.cleanLocalStore()) {
                            showPopupWindow(tv, "Error in cleaning the local database");
                        }
                        else {
                            showPopupWindow(tv, "All beacons removed");
                            FrameLayout frameLayout = findViewById(R.id.contact_frame);
                            frameLayout.setBackgroundColor(getResources().getColor(R.color.white));
                            TextView contact_text = findViewById(R.id.contact_text);
                            contact_text.setText(getResources().getString(R.string.contacts));
                            ((UserStatus) TestingActivity.this.getApplication()).setContacts(false);
                        }
                        return true;
                    default:
                        return false;
                }

            }
        });

        popup.show();//showing popup menu

    }

    public void showLog(View view) {
        TextView tv = findViewById(R.id.log_text);

        Process logcat;
        final StringBuilder log = new StringBuilder();
        try {

            String cmd = "logcat -d " + TESTING_ACTIVITY_LOG + ":D" + " *:S";
            logcat = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(logcat.getInputStream()));
            String line;
            String separator = System.getProperty("line.separator");
            while ((line = br.readLine()) != null) {
                log.append(line);
                log.append(separator);
            }
            tv.append(log.toString());
            ScrollView sv = (ScrollView) findViewById(R.id.scrollview);
            sv.fullScroll(ScrollView.FOCUS_DOWN);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
