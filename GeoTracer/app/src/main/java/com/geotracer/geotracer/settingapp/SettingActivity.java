package com.geotracer.geotracer.settingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.mainapp.MainActivity;
import com.geotracer.geotracer.testingapp.TestingActivity;

public class SettingActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Switch contact_notifications = findViewById(R.id.notification_contact);
        contact_notifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //ENABLE THE RECEIVING OF NOTIFICATION FOR HAVING MET SOMEONE POSITIVE TO COVID19
                    Log.d(this.getClass().getName(), "Contanct Notification Enabled");
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
        proximity_notifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //ENABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE
                    Log.d(this.getClass().getName(), "Positivity Report Enabled");
                }else{
                    //DISABLE THE RECEIVING OF NOTIFICATION FOR BEING TOO CLOSE TO OTHER PEOPLE
                    Log.d(this.getClass().getName(), "Positivity Report Disabled");
                }
            }
        });


    }




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




}