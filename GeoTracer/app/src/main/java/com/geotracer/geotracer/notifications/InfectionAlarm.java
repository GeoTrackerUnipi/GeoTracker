package com.geotracer.geotracer.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.data.Signature;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import io.paperdb.Paper;

public class InfectionAlarm extends Worker {

    private final Logger logger = Logger.getGlobal();
    private final FirebaseFirestore db;

    public InfectionAlarm(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
        Paper.init(context);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public @NotNull Result doWork() {

        try {

            logger.info("[Infection Alert] Starting collect user beacons");
            RetStatus<List<Signature>> signatures = getAllSignatures();
            if (signatures.getStatus() != OpStatus.OK)
                return Result.failure();

            RetStatus<String> result = identifyBucket();
            if( result.getStatus() != OpStatus.OK )
                return Result.retry();

            String actualBucket = result.getValue();

            signatures.getValue().forEach(signature -> db.collection(actualBucket).add(signature));
            Paper.book("signatures").destroy();
            return Result.success();

        }catch(RuntimeException e){

            e.printStackTrace();
            return Result.failure();

        }
    }

    public RetStatus<List<Signature>> getAllSignatures(){

        try{

            List<Signature> signatures = new ArrayList<>();
            Paper.book("signatures").getAllKeys().forEach(
                    s -> signatures.add(new Signature(Paper.book("signatures").read(s))));
            Collections.sort(signatures);
            logger.info( "Getting all the signatures. Number of signatures: "+signatures.size());
            for( int a = 0; a<signatures.size(); a++)
                if( !signatures.get(a).isExpired()){
                    logger.info( "Effective signatures: " + (signatures.size()-a));
                    if( a > 0)
                        return new RetStatus<>(signatures.subList(a, signatures.size()), OpStatus.OK);
                    else
                        return new RetStatus<>(signatures, OpStatus.OK);
                }
            logger.info( "Effective signatures: 0");
            return new RetStatus<>(new ArrayList<>(),OpStatus.EMPTY);

        }catch(RuntimeException e){

            e.printStackTrace();
            return new RetStatus<>(null,OpStatus.ERROR);

        }
    }

    public RetStatus<String> identifyBucket(){

        try {
            RetStatus<BaseLocation> result = getLastPosition();
            if (result.getStatus() != OpStatus.OK)
                return new RetStatus<>(null, result.getStatus());

            // TODO Geocoding
            return new RetStatus<>("Pisa", OpStatus.OK);
        }catch(RuntimeException e){
            e.printStackTrace();
            return new RetStatus<>("Pisa", OpStatus.OK);
        }
    }

    //  returns the last position inserted by the user
    //  Returns:
    //      - OpStatus.OK: all the valid signatures are given with the object
    //      - OpStatus.EMPTY: the operation went well but no signature is present
    //      - OpStatus.ERROR: an error has occurred during the request

    public RetStatus<BaseLocation> getLastPosition(){
        try{

            List<BaseLocation> positions = new ArrayList<>();
            Gson gson = new Gson();
            Paper.book("positions").getAllKeys().forEach(s->positions.add(gson.fromJson(s,BaseLocation.class)));

            if( positions.size() == 0 )
                return new RetStatus<>(null,OpStatus.OK); //  TODO To reactivate when position could be insert -> OPSTATUS.EMPTY

            Collections.sort(positions);
            logger.info("Getting last position: "+positions.get(0).getLocation().toString());
            return new RetStatus<>(positions.get(0), OpStatus.OK);
        }catch(RuntimeException e){
            e.printStackTrace();
            return new RetStatus<>(null, OpStatus.ERROR);
        }
    }

}
