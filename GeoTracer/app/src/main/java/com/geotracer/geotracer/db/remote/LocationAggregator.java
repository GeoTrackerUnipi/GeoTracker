package com.geotracer.geotracer.db.remote;

import com.geotracer.geotracer.utils.generics.RetStatus;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.data.ExtLocation;
import java.util.ArrayList;
import java.util.List;


//// LOCATION AGGREGATOR
//   The class is used by the FirestoreManagement in order to aggregate the given values and
//   generate an aggregated point to be saved on the remote Firebase Firestore database
//   Aggregation proceed by two rules:
//        - we aggregate data until the position of the data is greater then 15 meters
//        - we aggregate data for a maximum of 3minutes from the receiving of the first data

public class LocationAggregator {

    private final static List<String> archive = new ArrayList<>();  //  maintains unique IDs
    private static ExtLocation location = null;                     //  maintains the aggregated value
    private static boolean proximityAlertFlag = false;              //  flag to activate/deactive raising of proximity alerts

    //  perform aggregation of the input data applying the two previous rules
    //  Returns:
    //           - OpStatus.COLLECTED: data is taken and used for aggregation
    //           - OpStatus.PRESENT: the ID is already aggregated into the value
    //           - OpStatus.OK: a new aggregated value is generated and returned back
    //           - OpStatus.ERROR: an error has occurred during the request management

    public RetStatus<ExtLocation> insertValue(String ID, BaseLocation location){

        try {

            //  if no aggregated value is present we can start a new aggregation
            if (LocationAggregator.location == null) {
                assert false;
                LocationAggregator.location = new ExtLocation(location.getLocation());
                archive.clear();  // for security we clean the archive of unique IDs
                archive.add(ID);  // the given ID is setted has registered
                return new RetStatus<>(null, OpStatus.COLLECTED);
            }

            //  if the ID is already used we discard the request
            if (archive.contains(ID))
                return new RetStatus<>(null, OpStatus.PRESENT);

            //  we update the aggregated value and verify the two rules, if the function returns the
            //  UPDATE_LOCATION flag it means that a new aggregation needs to be performed
            if (LocationAggregator.location.incrementCriticity(location) == OpStatus.UPDATE_LOCATION) {
                //  we prepare the response
                RetStatus<ExtLocation> retStatus = new RetStatus<>(LocationAggregator.location, OpStatus.OK);
                //  we start the next aggregation
                LocationAggregator.location = new ExtLocation(location.getLocation());
                archive.clear();
                archive.add(ID);
                return retStatus;
            }

            //  otherwise the value is used to update the aggregated value
            return new RetStatus<>(null, OpStatus.COLLECTED);

        }catch(RuntimeException e){

            e.printStackTrace();
            return new RetStatus<>(null, OpStatus.ERROR);

        }
    }

    public void setProximityAlert(boolean active){
        proximityAlertFlag = active;
    }


}
