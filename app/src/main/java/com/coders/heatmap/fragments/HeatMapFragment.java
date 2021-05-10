package com.coders.heatmap.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.coders.heatmap.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HeatMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //use the fragment_heat_map.xml as layout for this fragment
        View view = inflater.inflate(R.layout.fragment_heat_map,container,false);

        //get support map fragment: initialize it with the fragment inside the fragment_heat_map.xml with id="map"
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        /*
            start async notification when map is ready
            when the map is ready it will trigger onMapReady (in async way)
         */
        supportMapFragment.getMapAsync((OnMapReadyCallback) this);
        return view;
    }


    //When map is ready!
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;
        addHeatMap();
        //TODO: for testing i disabled the fixed zoom. I will decomment that
       // mMap.setMinZoomPreference(18f);
        //mMap.setMaxZoomPreference(18f);

        googleMap.setBuildingsEnabled(true);

        googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        getContext(), R.raw.map_style_conf));

        //TODO: this is a marker --> i will replace it's coordinates with the user one
        // Add a marker
        LatLng myPosition = new LatLng(-37.1886, 145.708);
        mMap.addMarker(new MarkerOptions()
                .position(myPosition)
                .title("Me"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));
    }

    private void addHeatMap() {
        List<LatLng> list = null;

        /*
            TODO: this is a prototype version --> we will retrieve the data from firebase instead of local heat_map_points.json
         */
        try {
            list = readItems(R.raw.heat_map_points);
        } catch (JSONException e) {
            Toast.makeText(getContext(), "There was a problem in reading the list of points", Toast.LENGTH_LONG).show();
        }

        HeatmapTileProvider provider = new HeatmapTileProvider.Builder().data(list).build();

        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
    }


    private ArrayList<LatLng> readItems(int resource) throws JSONException {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        InputStream inputStream = getResources().openRawResource(resource);
        @SuppressWarnings("resource")
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("lng");
            list.add(new LatLng(lat, lng));
        }

        return list;
    }
}
