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
import com.geotracer.geotracer.db.remote.FirestoreManagement;
import com.geotracer.geotracer.utils.data.ExtLocation;
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
    FirestoreManagement firestoreManagementService;
    boolean isFirestoreManagementBounded = false;

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
        //TODO: for testing i disabled the fixed zoom. I will decomment that
        // mMap.setMinZoomPreference(18f);
        //mMap.setMaxZoomPreference(18f);

        googleMap.setBuildingsEnabled(true);

        googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        getContext(), R.raw.map_style_conf));

        //TODO: this is a marker --> i will replace it's coordinates with the user one
        // Add a marker
        LatLng myPosition = new LatLng(-37.25, 145.76);
        mMap.addMarker(new MarkerOptions()
                .position(myPosition)
                .title("Me"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myPosition));

        /*

        TODO: Per Riccardo/Nicola --> questo è un esempio di come chiamare la funzione per convertire la locazione in città
        An example of location conversion
            Location temp = new Location(LocationManager.GPS_PROVIDER);
            temp.setLatitude(-37.25);
            temp.setLongitude(145.79);
            Log.d("GeocoderManager", "invio richiesta");
            GeocoderManager.convertLocationToPlace(temp,getContext());

         */

        if (!isFirestoreManagementBounded){
            //bind with firestoremanagement service
            Intent intent = new Intent(getContext(), FirestoreManagement.class);
            getActivity().bindService(intent, firestoreManagementConnection, Context.BIND_AUTO_CREATE);
        }
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
        if(firestoreManagementConnection != null) {
            getActivity().unbindService(firestoreManagementConnection);
            isFirestoreManagementBounded = false;
        }
    }

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
                public void onSuccess() {
                    //not used
                }

                @Override
                public void onFailure() {
                    //not used
                }

                @Override
                public void onDataCollected(List<ExtLocation> location) {
                    Log.d("HeatMap", "data collected");
                    for(int i=0; i<location.size();i++) {
                        Log.d("HeatMap", location.get(i)+"");
                    }

                    /*TODO: per Riccardo/Nicola: qui dovrei avere la lista delle coordinate, per adesso leggo da locale
                            ma appena hai caricato i dati togli pure
                     */

                    addHeatMap(location); //per adesso leggo da locale, ma li dentro stesso gestirai la lista remota

                }
            });
            LatLng myPosition = new LatLng(-37.25, 145.76);
            GeoPoint geoPoint = new GeoPoint(myPosition.latitude,myPosition.longitude);
            firestoreManagementService.getNearLocations(geoPoint,100);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("HeatMap", "HeatMap unbounded with firestoreManagement service");
            isFirestoreManagementBounded = false;
        }
    };

    private void addHeatMap(List<ExtLocation> location) {
        List<LatLng> list = null;

        /*
            TODO: per Riccardo/Nicola: qui leggo da locale tramite readItems
                  usate la lista passata come argomento per inserire i dati nella mappa
         */
        try {
            list = readItems(R.raw.heat_map_points); //da locale (sostituire con la lista passata)
        } catch (JSONException e) {
            Toast.makeText(getContext(), "There was a problem in reading the list of points", Toast.LENGTH_LONG).show();
        }

        HeatmapTileProvider provider = new HeatmapTileProvider.Builder().data(list).build();

        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
    }



    //TODO: per Nicola: quando usi i dati in remoto puoi anche eliminarla
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

