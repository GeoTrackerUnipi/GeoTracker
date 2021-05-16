package com.geotracer.geotracer.service;

import android.annotation.SuppressLint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Date;

// This class used the GPS location provided offered by Android OS for collecting the user's positions in time
class GeoLocator implements LocationListener
{
 /*=============================================================================================================================================*
 |                                                             ATTRIBUTES                                                                       |
 *=============================================================================================================================================*/

 /* =================== Constants =================== */
 private static final String TAG = "GeoLocation";               // TAG used for logging purposes

 // ------------- GPS Sampling Parameters -------------
 private static final long LOC_MIN_TIME_INTERVAL = 15*1000;     // The minimum time interval for GPS readings to be reported by the OS  TODO: Set to an acceptable value after testing
 private static final long LOC_MIN_DIST_CHANGE = 0;             // The minimum estimated distance of the user in meters
                                                                // for GPS readings to be reported by the OS TODO: set to 15 meters after testing

 // Other Devices Positions Accuracy Policy
 // ---------------------------------------------------
 // Whether the estimated positions of other devices should be added to the local database only if they are relatively accurate in time with respect
 // to the last known position of the user, meaning that the latter must differ from the average timestamp of their advertisements for no longer
 // than POS_HIGH_ACCURACY_MARGIN milliseconds.
 // It should also be noted that if both the localization and the GPS provider are active and LOC_MIN_DIST_CHANGE > 0 the last known position of the
 // user is considered valid regardless of its timestamp, being this condition associated to the user standing still and the android system not
 // reporting GPS readings for power savings purposes.
 private static final boolean POS_POLICY_HIGH_ACCURACY = false;
 private static final long POS_HIGH_ACCURACY_MARGIN = 30 * 1000;

 /* ==================== Objects ==================== */
 GeotracerService geotracerService;                             // A reference to the main service object
 private final LocationManager locationManager;                 // The location manager object used by the service
 private Location lastLocation;                                 // The user's last known location

 /* ============ Service Status Variables ============ */
 private boolean isLocalizing;                                  // Whether the localization system is enabled or not
 private boolean isGPSEnabled;                                  // Whether the GPS location provider is enabled or not

 /*=============================================================================================================================================*
 |                                                    PACKAGE-VISIBILITY METHODS                                                                |
 *=============================================================================================================================================*/

 // Constructor (the android dynamic location permission check is disabled for it was already checked in the main service)
 @SuppressLint("MissingPermission")
 GeoLocator(GeotracerService geotracerService,LocationManager locationManager)
  {
   // All these objects are non-null (otherwise this class is not instantiated by the main service)

   this.geotracerService = geotracerService;
   this.locationManager = locationManager;
   this.isLocalizing = false;

   // If the GPS provider is enabled
   if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
    {
     isGPSEnabled = true;

     // If the last user position is recent enough, use it as initial position and push it into the local database
     lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
     if(lastLocation != null && (lastLocation.getTime() > new Date().getTime() - POS_HIGH_ACCURACY_MARGIN))
      {
       Log.i(TAG,"Initial user position: "+lastLocation);
       // TODO: Push to nicola
      }
     else
      {
       Log.w(TAG,"The initial user position is too old and was discarded");
       lastLocation = null;
      }
    }

   // Otherwise, if the GPS provider is NOT enabled
   else
    {
     isGPSEnabled = false;
     this.lastLocation = null;
     Log.w(TAG,"The initial user position could not be determined (the GPS provider is disabled)");
    }
  }

 // Start the localization service by registering this object as a listener for GPS location updates, returning the result of the operation
 // NOTE: The actual rate and timing of GPS readings are performed by the Android OS according to its own policies, this is just a listener
 @SuppressLint("MissingPermission")
 boolean startLocalization()
  {
   // Check the localization not to be already started
   if(isLocalizing)
    {
     Log.w(TAG,"Localization service already started");
     return false;
    }
   else
    {
     isLocalizing = true;
     locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,LOC_MIN_TIME_INTERVAL,LOC_MIN_DIST_CHANGE,this);
     return true;
    }
  }

 // Stops the localization service, returning the result of the operation
 boolean stopLocalization()
  {
   // Check the localization to be started
   if(!isLocalizing)
    {
     Log.w(TAG,"Localization service is already stopped");
     return false;
    }
   else
    {
     locationManager.removeUpdates(this);
     isLocalizing = false;
     return true;
    }
  }

 // This function is called by the GeoScanner service for adding the estimated position of another device into the database
 void injectDevicePosition(double contactDistance,long contactTime)
  {
   // Check at least one valid position to be available
   if(lastLocation == null)
    Log.e(TAG,"No user position is known, cannot add device position into the database");
   else
    {
     // If the last known user position is accurate enough, add the device's estimated position into the local database
     // NOTE: This is better explained in the "Other Devices Positions Accuracy Policies" section in the class constants declarations
     if(!POS_POLICY_HIGH_ACCURACY || (isGPSEnabled && isLocalizing && LOC_MIN_DIST_CHANGE > 0) ||
             (lastLocation.getTime() > contactTime - POS_HIGH_ACCURACY_MARGIN))
      {
       //TODO: Nicola.OtherPositions.push(lastLocation,contactDistance,contactTime);
       Log.w(TAG,"Added device location to the database (contactDistance = " + contactDistance + ", contactTime =  " + contactTime + ")");
      }
     else
      Log.e(TAG,"The last known user position is too old for adding the device location into the database");
    }
  }

 // Returns the last known user location, if any
 // NOTE: May return NULL if no user position is known
 Location getLastLocation()
  { return lastLocation; }

 // Returns true if the localization service is enabled
 boolean isLocalizing()
  { return isLocalizing; }

 // Returns true if the GPS location provider is enabled
 boolean isGPSEnabled()
  { return isGPSEnabled; }

 /*=============================================================================================================================================*
 |                                           LOCATIONLISTENER INTERFACE CALLBACK FUNCTIONS                                                      |
 *=============================================================================================================================================*/

 // Callback function invoked by the Android OS to report GPS readings
 @Override
 public void onLocationChanged(@NonNull Location location)
  {
   lastLocation = location;
   Log.i(TAG,"New GPS Location! (lat = " + location.getLatitude() + ", long = " + location.getLongitude() + ")");
   // TODO: Give to Nicola
  }

 // Callback function invoked by the Android OS when the GPS provider is enabled
 @Override
 public void onProviderEnabled(String provider)
  {
   if(provider.equals(LocationManager.GPS_PROVIDER))
    {
     Log.w(TAG,"GPS Provider now enabled");
     isGPSEnabled = true;
    }
  }

 // Callback function invoked by the Android OS when the GPS provider is disabled
 @Override
 public void onProviderDisabled(String provider)
  {
   if(provider.equals(LocationManager.GPS_PROVIDER))
    {
     Log.w(TAG,"GPS Provider Disabled");
     isGPSEnabled = false;
    }
  }

 // Callback function invoked by the Android OS when the GPS *interface* temporarily changes status (mainly useful for debugging purposes)
 @Override
 public void onStatusChanged(String provider, int status, Bundle extras)
  {
   if(provider.equals(LocationManager.GPS_PROVIDER))
    switch(status)
     {
      // This should NEVER happen
      case 0:
       Log.d(TAG,"GPS Status Changed: OUT_OF_SERVICE");
       isGPSEnabled = false;
       break;

      // In the context of this service this occurs when LOC_MIN_DIST_CHANGE > 0 and the user is standing still in a position (power saving purposes)
      case 1:
       Log.d(TAG,"GPS Status Changed: TEMPORARILY_UNAVAILABLE");
       isGPSEnabled = false;
       break;

      // This occurs when the GPS interface is waken up
      case 2:
       Log.d(TAG,"GPS Status Changed: AVAILABLE");
       isGPSEnabled = true;
       break;
     }
  }
}