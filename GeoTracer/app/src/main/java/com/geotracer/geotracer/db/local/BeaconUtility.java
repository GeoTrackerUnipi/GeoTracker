package com.geotracer.geotracer.db.local;

import com.geotracer.geotracer.utils.data.ExtSignature;
import com.geotracer.geotracer.utils.generics.OpStatus;
import static android.content.ContentValues.TAG;
import android.util.Log;
import io.paperdb.Book;


////// BEACONS
//   A beacon is a signature obtained from other applications. All the received beacons are stored inside
//   the key-value database with an expire time after which data will be removed. The beacons are used by
//   the contact trace mechanism to identify if the user was near to an infected person
//   Data Format:    BEACON: { beacon, distance, expire }

public class BeaconUtility {

    private final Book beacons;

    // prevents the class to be instantiated outside the package
    BeaconUtility(Book beacons){
        this.beacons = beacons;
    }

    //  insert a new captured position
    //  Returns:
    //      - OpStatus.OK: all the valid signatures are given with the object
    //      - OpStatus.ERROR: an error has occurred during the request

    public OpStatus insertBeacon(ExtSignature beacon){

        String beaconString = beacon.toString();
        String beaconTag = beacon.getSignature();

        try {

            //  we verify the beacon is present inside the database
            if (beacons.contains(beaconTag)) {
                //  we maintains the minimum distance from each beacon, so if the distance is less
                //  than the stored one we update the beacon
                if(new ExtSignature(beacons.read(beaconTag)).toBeUpDated(beacon.getDistance())) {
                    beacons.delete(beaconTag);
                    Log.d(TAG,"Updating beacon " + beaconTag);
                }else
                    return OpStatus.OK;
            }else
                Log.d(TAG,"Putting new beacon " + beaconTag );

            beacons.write(beaconTag, beaconString);
            return OpStatus.OK;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }

    //  verify if a beacon is present inside the store and if it is valid
    //  Returns:
    //      - OpStatus.PRESENT: the beacon is present and valid
    //      - OpStatus.NOT_PRESENT: the beacon is not present or it is present but expired
    //      - OpStatus.ERROR: an error has occurred during the managing of the request

    public OpStatus beaconPresent(String beacon){

        try {
            //  we verify that the beacon is present and not expired
            return beacons
                    .contains(beacon) && !new ExtSignature(beacons.read(beacon)).isExpired() ?
                    OpStatus.PRESENT : OpStatus.NOT_PRESENT;

        }catch(RuntimeException e){

            e.printStackTrace();
            return OpStatus.ERROR;

        }
    }
}
