package com.geotracer.geotracer.utils;

import android.util.Log;

import com.geotracer.geotracer.utils.data.TestData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;


public class LogParsing {

    ArrayList<TestData> log_values = new ArrayList<>();
    private final String TAG = "LogParsing";




    public LogParsing(String toBeParsed, String phy_distance){

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
                TestData testData = new TestData();
                testData.putData("KEY", key);
                testData.putData("Estimated Distance", distance_estimation);
                testData.putData("Physical Distance", phy_distance);
                testData.putData("Timestamp", timestamp);
                log_values.add(testData);

            }

        }

        for(int k = 0; k < log_values.size(); k++) {
            String[] keys = log_values.get(k).getData().keySet().toArray(new String[0]);
            for (int j = 0; j < keys.length; j++) {
                Log.d(TAG, "KEY " + keys[j] + " VALUES " + log_values.get(k).getData(keys[j]));
            }
        }




    }

    public ArrayList<TestData> getLog_values(){
        return log_values;
    }


}




