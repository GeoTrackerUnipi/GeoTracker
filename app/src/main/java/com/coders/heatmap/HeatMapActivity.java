package com.coders.heatmap;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.coders.heatmap.fragments.HeatMapFragment;

public class HeatMapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.heat_map_activity);

        //initialize heat map fragment
        Fragment heatMapFragment = new HeatMapFragment();

        //start heat map fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_heat_map_area, heatMapFragment) //insert heatMapFragment inside the area
        .commit();
    }
}
