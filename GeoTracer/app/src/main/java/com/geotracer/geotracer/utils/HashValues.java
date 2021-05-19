package com.geotracer.geotracer.utils;

public class HashValues {
    String timestamp = null;
    String distance = null;

    public HashValues(String ts, String d){
        timestamp = ts;
        distance = d;
    }

    public String getTimestamp(){
        return timestamp;
    }

    public String getDistance(){
        return distance;
    }

    public void setTimestamp(String ts){
        timestamp = ts;
    }

    public void setDistance(String d){
        distance = d;
    }

}
