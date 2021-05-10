package com.geotracer.geotracer.db;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.data.ExtSignature;
import com.geotracer.geotracer.utils.data.Signature;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import io.paperdb.Paper;

public class DatabaseConsolidator extends Worker {

    private final Logger logger = Logger.getGlobal();

    public DatabaseConsolidator(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        Paper.init(context);
    }

    @Override
    public @NotNull Result doWork() {

        if( dropExpiredBeacons() == OpStatus.OK &&
                dropExpiredSignatures() == OpStatus.OK &&
                    dropExpiredPositions() == OpStatus.OK )
            return Result.success();
        return Result.failure();

    }

    public OpStatus dropExpiredBeacons(){

        try {

            logger.info( "Starting consolidation of stored beacons");
            List<String> keys = Paper.book("beacons").getAllKeys();
            int before = keys.size();
            keys.forEach(key -> {
                if ( new ExtSignature(Paper.book("beacons").read(key)).isExpired()){
                    Paper.book("beacons").delete(key);
                }
            });
            logger.info( "Removed " + (before - Paper.book("beacons").getAllKeys()
                    .size()) + " of " + before + " entries");

            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    public OpStatus dropExpiredSignatures(){

        try {

            logger.info( "Starting consolidation of stored signatures");
            List<String> keys = Paper.book("signatures").getAllKeys();
            List<String> removed = new ArrayList<>();
            int before = keys.size();
            keys.forEach(key -> {
                if ( new Signature( Paper.book("signatures").read(key)).isExpired()) {
                    Paper.book("signatures").delete(key);
                    removed.add(key);
                }
            });
            logger.info( "Removed " + (before - Paper.book("signatures").getAllKeys()
                    .size()) + " of " + before + " entries");

            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    public OpStatus dropExpiredPositions(){

        try {

            logger.info( "Starting consolidation of stored positions");
            List<String> keys = Paper.book("positions").getAllKeys();
            int before = keys.size();
            keys.forEach(key -> {
                if (new BaseLocation( Paper.book("positions").read(key)).isExpired()) {
                    Paper.book("positions").delete(key);
                }
            });
            logger.info( "Removed " + (before - Paper.book("beacons").getAllKeys()
                    .size()) + " of " + before + " entries");

            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }
}
