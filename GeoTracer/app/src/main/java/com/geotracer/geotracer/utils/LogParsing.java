package com.geotracer.geotracer.utils;

import android.util.Log;

import java.util.HashMap;


public class LogParsing {


    String log = null;
    String key = null;
    String timestamp = null;
    String distance_estimation = null;
    private final String TAG = "LogParsing";




    public LogParsing(String toBeParsed){

        /*
        THIS IS THE STRING YOU GET
        Log.i(TAG,"Estimated distance from device \"" + key + "\": " + contactDistance + " (time = " +
                      new SimpleDateFormat("dd-MM-yyyy HH:mm:ss",Locale.getDefault()).format(new Date(contactTime)) + ")");


         */
        log = toBeParsed;
        String split[] = log.split("\\n");
        for(int i = 1; i < split.length; i++){
            String div1[] = split[i].split("\"");
            if(div1.length > 2) {
                key = div1[1];
                String div2[] = div1[2].split(": ");
                    if (div2.length > 1){
                        String div3 [] = div2[1].split("= ");
                        distance_estimation = div2[1].split(" ")[0];
                        if(div3.length > 1 ) {
                            //for (int j = 0; j < div3.length; j++)
                            timestamp = div3[1].split("\\)" )[0];
                            Log.d("QUIIIIII ", i + " KEY: " + key + " DIST " + distance_estimation + " TS " + timestamp);
                        }
                }
            }

        }
    }


}




