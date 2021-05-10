package com.geotracer.geotracer.utils.data;

import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

public class ExtLocation extends BaseLocation{

    private boolean infected;
    private int criticity;

    public ExtLocation(GeoPoint location, int criticity, boolean infected ){
        super(location);
        this.infected = infected;
        this.criticity = criticity;
    }

    public ExtLocation( String jsonObject ){
        super();
        Gson gson = new Gson();
        ExtLocation converter = gson.fromJson(jsonObject,ExtLocation.class);
        this.location = converter.location;
        this.expire = converter.expire;
        this.infected = converter.infected;
        this.criticity = converter.criticity;
    }

    public boolean getInfected(){
        return infected;
    }

    public int getCriticity(){
        return criticity;
    }
}
