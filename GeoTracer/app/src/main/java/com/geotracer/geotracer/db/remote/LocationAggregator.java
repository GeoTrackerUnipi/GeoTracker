package com.geotracer.geotracer.db.remote;

import com.geotracer.geotracer.utils.data.BaseLocation;
import com.geotracer.geotracer.utils.data.ExtLocation;
import com.geotracer.geotracer.utils.generics.OpStatus;
import com.geotracer.geotracer.utils.generics.RetStatus;

import java.util.ArrayList;
import java.util.List;

public class LocationAggregator {

    private final static List<String> archive = new ArrayList<>();
    private static ExtLocation location = null;

    public RetStatus<ExtLocation> insertValue(String ID, BaseLocation location){

        try {
            if (LocationAggregator.location == null) {

                assert false;
                LocationAggregator.location = new ExtLocation(location.getLocation());
                archive.clear();
                archive.add(ID);
                return new RetStatus<>(null, OpStatus.COLLECTED);
            }

            if (archive.contains(ID))
                return new RetStatus<>(null, OpStatus.PRESENT);

            if (LocationAggregator.location.incrementCriticity(location) == OpStatus.UPDATE_LOCATION) {
                archive.clear();
                RetStatus<ExtLocation> retStatus = new RetStatus<>(LocationAggregator.location, OpStatus.OK);
                LocationAggregator.location = null;
                return retStatus;
            }

            return new RetStatus<>(null, OpStatus.COLLECTED);

        }catch(RuntimeException e){
            e.printStackTrace();
            return new RetStatus<>(null, OpStatus.ERROR);
        }
    }
}
