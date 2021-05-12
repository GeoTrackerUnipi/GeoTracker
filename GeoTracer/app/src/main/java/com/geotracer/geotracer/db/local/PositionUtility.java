package com.geotracer.geotracer.db.local;

import com.geotracer.geotracer.utils.generics.RetStatus;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.generics.OpStatus;
import static android.content.ContentValues.TAG;
import java.util.Collections;
import com.google.gson.Gson;
import java.util.ArrayList;
import android.util.Log;
import io.paperdb.Book;
import java.util.List;


////// POSITIONS
//   Periodically user positions are stored inside the key value database in order to be able from
//   the other application's components to know the user position and, in case of infection, to
//   propagate all the user position inside the heat map
//   Data Format:    POSITION: { position, expire }

@SuppressWarnings("unused")
public class PositionUtility {


    private final Book positions;

    //  prevents the class to be instantiated outside the package
    PositionUtility(Book positions){
        this.positions = positions;
    }

    //  insert a new user position or update it in order to refresh the expiring time
    //  Returns:
    //      - OpStatus.OK: the position is inserted
    //      - OpStatus.REFRESH: the position is updated
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus insertOrUpdatePosition(BaseLocation location ){

        if( positions == null )
            return OpStatus.ERROR;

        String stringTag = location.getLocation().toString();
        try{

            if(positions.contains(stringTag)) {

                positions.delete(stringTag);
                Log.d(TAG,"Updating position: " + stringTag + " : " + location.toString() );
                positions.write(stringTag, location.toString());
                return OpStatus.UPDATED;

            }

            Log.d(TAG, "Putting new position " + stringTag );
            positions.write(stringTag, location.toString());

            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;
        }
    }

    //  returns all the valid positions inserted by the user
    //  Returns:
    //      - OpStatus.OK: all the valid signatures are given with the object
    //      - OpStatus.EMPTY: the operation went well but no signature is present
    //      - OpStatus.ERROR: an error has occurred during the request

    public RetStatus<List<BaseLocation>> getAllPositions(){
        try{

            Gson gson = new Gson();
            List<BaseLocation> collectedPositions = new ArrayList<>();

            //  collecting all the stored positions
            positions
                    .getAllKeys()
                    .forEach( s->collectedPositions.add( gson.fromJson((String)positions.read(s),BaseLocation.class)) );

            //  sorting all the position by their expiring time
            Collections.sort(collectedPositions);

            //  by the reordering we can get all the valid positions by just splitting the list
            //  from the first valid position encountered [invalid,invalid, valid, valid, valid..]
            for( int a = 0; a<collectedPositions.size(); a++)
                if( !collectedPositions.get(a).isExpired()) {
                    Log.d(TAG, "Getting all the positions. Number of positions: "+collectedPositions.size() +
                            " Effective positions: " + (collectedPositions.size()-a));
                    return new RetStatus<>(collectedPositions.subList(a, collectedPositions.size()), OpStatus.OK);
                }

            //  all the positions are expired or no position found
            Log.d(TAG, "Getting all the positions. Number of positions: " + collectedPositions.size() +
                    " Effective positions: 0");
            return new RetStatus<>(new ArrayList<>(),OpStatus.EMPTY);

        }catch(RuntimeException e){

            e.printStackTrace();
            return new RetStatus<>(new ArrayList<>(), OpStatus.ERROR);

        }
    }

}
