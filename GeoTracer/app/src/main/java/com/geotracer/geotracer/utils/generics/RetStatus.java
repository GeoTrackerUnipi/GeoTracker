package com.geotracer.geotracer.utils.generics;

public class RetStatus<T>{

    private T value;
    private OpStatus result;

    public RetStatus(T value, OpStatus error){
        this.value = value;
        this.result = error;
    }

    public RetStatus<String> buildError(OpStatus error){
        try {
            return new RetStatus<>(error.toString(), error);
        }catch(IllegalArgumentException e){
            e.printStackTrace();
            return new RetStatus<>("ERROR", OpStatus.ERROR);
        }
    }

    public T getValue(){
        if( value == null || result != OpStatus.OK)
            return null;
        return value;
    }

    public OpStatus getStatus(){
        return this.result;
    }

}
