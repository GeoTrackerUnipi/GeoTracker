package com.geotracer.geotracer.utils;

import android.util.Log;

import com.geotracer.geotracer.utils.data.TestData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;


public class LogParsing {

    //HashMap<String, HashValues> log_values = new HashMap<>();
    TestData log_values = new TestData();
    private final String TAG = "LogParsing";




    public LogParsing(String toBeParsed){

        /*
        THIS IS THE STRING YOU GET
        Log.i(TAG,"Estimated distance from device \"" + key + "\": " + contactDistance + " (time = " +
                      new SimpleDateFormat("dd-MM-yyyy HH:mm:ss",Locale.getDefault()).format(new Date(contactTime)) + ")");


         */

        String log;
        String key = null;
        String timestamp = null;
        String distance_estimation = null;


        log = toBeParsed;
        String[] split = log.split("\\n");
        for(int i = 1; i < split.length; i++){
            String[] div1 = split[i].split("\"");
            if(div1.length > 2) {
                key = div1[1];
                String[] div2 = div1[2].split(": ");
                    if (div2.length > 1){
                        String[] div3 = div2[1].split("= ");
                        distance_estimation = div2[1].split(" ")[0];
                        if(div3.length > 1 ) {
                            timestamp = div3[1].split("\\)" )[0];
                            Log.d(TAG, i + " KEY: " + key + " DIST " + distance_estimation + " TS " + timestamp);
                        }
                }
            }
            if(key != null && distance_estimation != null && timestamp != null){
                HashValues hv = new HashValues(timestamp, distance_estimation);
                log_values.putData(key, distance_estimation);

            }

        }


        //String[] keys = log_values.getData().keySet().toArray(new String[0]);
        String[] keys = log_values.getData().keySet().toArray(new String[0]);
        for(int j = 0; j < keys.length; j++) {
            //Log.d(TAG, "KEY " + keys[j] + " VALUES " + log_values.get(keys[j]));
            Log.d(TAG, "KEY " + keys[j] + " VALUES " + log_values.getData(keys[j]));
        }




    }

    public TestData getLog_values(){
        return log_values;
    }


}




