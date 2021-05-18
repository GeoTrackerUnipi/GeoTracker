package com.geotracer.geotracer.utils.data;

import java.util.HashMap;

public class TestData{

    private final HashMap<String,String> data = new HashMap<>();

    //  insert a new data
    public void putData(String key, String value){
        data.put(key,value);
    }

    public Object getData(String key){
        if( data.containsKey(key))
            return data.get(key);
        return "";
    }

    public HashMap<String,String> getData(){
        return data;
    }

}
