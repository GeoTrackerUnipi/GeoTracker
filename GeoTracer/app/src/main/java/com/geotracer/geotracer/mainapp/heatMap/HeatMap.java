package com.geotracer.geotracer.mainapp.heatMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.geotracer.geotracer.R;
import com.geotracer.geotracer.db.local.KeyValueManagement;
import com.geotracer.geotracer.db.remote.FirestoreManagement;
import com.geotracer.geotracer.utils.data.ExtLocation;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//fragment of Heat Map
public class HeatMap extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private GeoPoint lastPosition = null;
    FirestoreManagement firestoreManagementService;
    KeyValueManagement keyValueService;
    boolean isFirestoreManagementBounded = false;
    boolean isKeyvalueManagementBounded = false;

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

        if (!isKeyvalueManagementBounded){
            //bind with firestoremanagement service
            Intent intent = new Intent(getContext(), KeyValueManagement.class);
            getActivity().bindService(intent, keyValueConnection, Context.BIND_AUTO_CREATE);
        }
        return view;
    }




    //When map is ready!
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;
        //TODO: for testing i disabled the fixed zoom. I will decomment that
        // mMap.setMinZoomPreference(18f);
        //mMap.setMaxZoomPreference(18f);

        googleMap.setBuildingsEnabled(true);

        googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        getContext(), R.raw.map_style_conf));

    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d("HeatMap", "HeatMap resumed");
        if (!isFirestoreManagementBounded){
            //bind with firestoremanagement service
            Intent intent = new Intent(getContext(), FirestoreManagement.class);
            getActivity().bindService(intent, firestoreManagementConnection, Context.BIND_AUTO_CREATE);
        }

        if (!isKeyvalueManagementBounded){
            //bind with firestoremanagement service
            Intent intent = new Intent(getContext(), KeyValueManagement.class);
            getActivity().bindService(intent, keyValueConnection, Context.BIND_AUTO_CREATE);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d("HeatMap", "HeatMap destroyed");
        if(isFirestoreManagementBounded) {
            getActivity().unbindService(firestoreManagementConnection);
            isFirestoreManagementBounded = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d("HeatMap", "HeatMap stopped");
        if(isFirestoreManagementBounded) {
            getActivity().unbindService(firestoreManagementConnection);
            isFirestoreManagementBounded = false;
        }
    }

    private final ServiceConnection keyValueConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("HeatMap", "HeatMap bounded with firestoreManagement service");
            isKeyvalueManagementBounded = true;
            KeyValueManagement.LocalBinder localBinder = (KeyValueManagement.LocalBinder) iBinder;
            keyValueService = localBinder.getService();
            RetStatus<GeoPoint> last = keyValueService.positions.getLastPosition();
            if( last.getStatus() == OpStatus.OK ) {
                lastPosition = last.getValue();
                LatLng position = new LatLng(lastPosition.getLatitude(), lastPosition.getLongitude());
                mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title("Me"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
            }

            if (!isFirestoreManagementBounded){
                //bind with firestoremanagement service
                Intent intent = new Intent(getContext(), FirestoreManagement.class);
                getActivity().bindService(intent, firestoreManagementConnection, Context.BIND_AUTO_CREATE);
            }else
                collectData();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("HeatMap", "HeatMap unbounded with firestoreManagement service");
            isFirestoreManagementBounded = false;
        }
    };


    private final ServiceConnection firestoreManagementConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("HeatMap", "HeatMap bounded with firestoreManagement service");
            isFirestoreManagementBounded = true;
            FirestoreManagement.LocalBinder localBinder = (FirestoreManagement.LocalBinder) iBinder;
            firestoreManagementService = localBinder.getService();
            //once is connected call

            firestoreManagementService.setFirestoreCallbackListener(new FirestoreManagement.FirestoreCallback() {

                @Override
                public void onDataCollected(List<ExtLocation> location) {
                    Log.d("HeatMap", "data collected");
                    for(int i=0; i<location.size();i++) {
                        Log.d("HeatMap", location.get(i)+"");
                    }

                    addHeatMap(location);

                }
            });
            if( lastPosition != null)
                firestoreManagementService.getNearLocations(lastPosition,100);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("HeatMap", "HeatMap unbounded with firestoreManagement service");
            isFirestoreManagementBounded = false;
        }
    };

    private OpStatus collectData(){
        if( isKeyvalueManagementBounded && isFirestoreManagementBounded){
            RetStatus<GeoPoint> geoPoint = keyValueService.positions.getLastPosition();
            if( geoPoint.getStatus() == OpStatus.OK)
                 return firestoreManagementService.getNearLocations(geoPoint.getValue(),100);
        }
        return OpStatus.ERROR;
    }

    private void addHeatMap(List<ExtLocation> location) {

        List<LatLng> list = new ArrayList<>();
        location.forEach( loc -> list.add(new LatLng(loc.getLocation().getLatitude(), loc.getLocation().getLongitude())));
        HeatmapTileProvider provider = new HeatmapTileProvider.Builder().data(list).build();

        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
    }

}

