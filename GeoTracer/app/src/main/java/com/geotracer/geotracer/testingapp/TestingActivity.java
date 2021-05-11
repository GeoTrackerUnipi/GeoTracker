package com.geotracer.geotracer.testingapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.mainapp.MainActivity;
import com.geotracer.geotracer.settingapp.SettingActivity;


public class TestingActivity extends AppCompatActivity {


    LogService service;
    boolean bound;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);


        TextView tv = (TextView) findViewById(R.id.log_text);

        LocalBroadcastManager.getInstance(TestingActivity.this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {

                        Log.d(this.getClass().getName(), "BROADCAST LISTENER");
                        String toLog = intent.getStringExtra("LogMessage");
                        tv.append(toLog);
                        ScrollView sv = (ScrollView) findViewById(R.id.scrollview);
                        sv.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                },new IntentFilter(LogService.ACTION_BROADCAST)

        );



    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, LogService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        bound = false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder s) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LogService.LocalBinder binder = (LogService.LocalBinder) s;
            service = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    public void startDissemination(View view) {

        /*

            START DISSEMINATING MY SIGNATURE.
            IT MUST E CALLED A FUNCTION WHICH STARTING THE SIGNATURE DISSEMINATION

            THEN THAT PROCESS MUST BE CHECKED
         */

        //if(/*   THE DISSEMINATION STARTED   */){

        String s = "DEVICE STARTED DISSEMINATING ITS SIGNATURE\n";

        String name = this.getClass().getName();

        service.printLog(name, s);


        /*

        }else{

        /*

            service.printLog(name, "Error in starting dissemination\n");
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


        showPopupWindow((TextView) findViewById(R.id.signature_dissemination), "Dissemination Stopped");
        service.printLog(this.getClass().getName(), "DEVICE STOPPED DISSEMINATING ITS SIGNATURE\n");



        /*

        }else{
           service.printLog(name, "Error in stop dissemination\n");
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

        service.printLog(this.getClass().getName(), "DEVICE STARTED COLLECTING SIGNATURES\n");


        /*

        }else{


          service.printLog(name, "Error in starting signature collection\n");

        }
        */
    }

    public void stopCollection(View view) {

        /*

            STOP TAKING THE OTHER DEVICES' SIGNATURES
            THE PROCEDURE RESULT WILL BE SHOWN IN A POPUP WINDOW

         */
        //if(/*   THE COLLECTION STOPPED   */){
        showPopupWindow((TextView) findViewById(R.id.signature_collection), "Signature Collection Stopped");
        service.printLog(this.getClass().getName(), "DEVICE STOPPED COLLECTING SIGNATURES\n");


        /*

        }else{
            service.printLog(name, "Error in stopping the signature collection\n");

        }
        */
    }

    public void infected(View view) {
        /*
            TAG THE USER AS INFECTED
            THE RESULT WILL BE SHOWN IN A POPUP WINDOW IN ADDITION TO THE LOG SECTION
         */
        //if(/*   DONE IT   */){

        service.printLog(this.getClass().getName(), "USER TAGGED AS INFECTED\n");


        /*

        }else{
            service.printLog(name, "Error! Unable to set the user as infected\n");

        }
        */
    }

    public void healed(View view) {

        /*
            UNTAG THE USER AS INFECTED
            THE RESULT WILL BE SHOWN IN A POPUP WINDOW IN ADDITION TO THE LOG SECTION
         */

        //if(/*   DONE IT   */){

        service.printLog(this.getClass().getName(), "USER IS NOT CONSIDERED INFECTED ANYMORE\n");


        /*

        }else{
           service.printLog(name, "Error! Unable to untag the user as infected\n");
        }
        */
    }

    public void delete(View view) {

        /*

        PERFORM THE ACTIONS NECESSARY TO THE DELETE FROM THE DB THE DATA OLDER THAN 14 DAYS.
        IF THE TESTING INTERFACE IS AVAILABLE TO EVERYONE I SUGGEST TO DELETE ONLY ITS DATA.
        A POPUP WINDOW WITH THE RESULT OF THE OPERATION WILL BE SHOWN
         */
        showPopupWindow((TextView) findViewById(R.id.deleteSignature), "Old data has been deleted from the database");

    }

    public void emptyLog(View view) {
        /*

        THIS FUNCTION DELETE THE CONTENT FROM THE LOG WINDOW

         */
        TextView tv = (TextView) findViewById(R.id.log_text);
        tv.setText("");

    }


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


    MENU ITEMS


     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.testing_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        TextView tv;
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

        Log.d(this.getLocalClassName(), "Instance State Saved");
*/
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
/*
        //restore the display
        String log = savedInstanceState.getString("log");
        ((TextView)findViewById(R.id.log_text)).setText(log);
        Log.d(this.getLocalClassName(), "Instance State Restored"); */
    }


}
