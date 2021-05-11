package com.geotracer.geotracer.mainapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.UsageTestActivity;

import com.geotracer.geotracer.infoapp.InfoActivity;
import com.geotracer.geotracer.settingapp.SettingActivity;
import com.geotracer.geotracer.testingapp.TestingActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testing_main);
    }

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


}