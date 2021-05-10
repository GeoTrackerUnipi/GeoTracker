package com.geotracer.geotracer.utils.generics;

public enum OpStatus {

    OK,
    PRESENT,
    NOT_PRESENT,
    ERROR,
    EMPTY;

    @Override
    public String toString() throws IllegalArgumentException{

        switch(this){

            case OK:                 return "OK";
            case PRESENT:            return "BEACON_PRESENT";
            case NOT_PRESENT:        return "BEACON_NOT_PRESENT";
            case ERROR:              return "ERROR";
            case EMPTY:              return "EMPTY";
            default: throw new IllegalArgumentException();

        }
    }

}
