package com.geotracer.geotracer.service;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.util.Log;

import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;

class GeoAdvertiser
{
 /*=============================================================================================================================================*
 |                                                             ATTRIBUTES                                                                       |
 *=============================================================================================================================================*/

 /* =================== Constants =================== */
 private static final String TAG = "GeoAdvertiser";                       // TAG used for logging purposes
 private static final int SIGNATURE_SIZE = 27;                            // Size in bytes of the advertised random signature
 private static final long SIGNATURE_VALIDITY_PERIOD = 15 * (1000 * 60);  // The period after which the random signature should be changed

 /* ==================== Objects ==================== */
 private final GeotracerService geotracerService;                         // A reference to the main service object
 private final BluetoothLeAdvertiser bluetoothAdvertiser;                 // Bluetooth Advertiser object
 private byte[] signature;                                                // The current user signature being broadcast
 private Timer signatureChanger;                                          // Timer used to periodically change the signature

 /* ================ Other Variables ================ */
 private boolean isAdvertising;                                           // Whether the user random signature is currently being advertised
 private boolean isAdvertisingStarting;                                   // Whether a start advertising request was forwarded to Android OS

 /*=============================================================================================================================================*
 |                                                   PACKAGE-VISIBILITY METHODS                                                                 |
 *=============================================================================================================================================*/

 // Constructor
 GeoAdvertiser(GeotracerService geotracerService, BluetoothLeAdvertiser bluetoothAdvertiser)
  {
   this.geotracerService = geotracerService;
   this.bluetoothAdvertiser = bluetoothAdvertiser;
   isAdvertising = false;
   isAdvertisingStarting = false;
  }

 // Starts the advertising using a new random signature, returning the result of the operation
 boolean startAdvertising()
  {
   // Check the Advertising not to be already started or starting
   if(isAdvertising)
    {
     Log.w(TAG,"Signature Advertising already started");
     return false;
    }

   if(isAdvertisingStarting)
    {
     Log.w(TAG,"Signature Advertising is already starting");
     return false;
    }

   // Assert the Bluetooth Advertiser object to be present
   if(bluetoothAdvertiser == null)
    {
     geotracerService.exitWithError(TAG,"The Bluetooth Advertiser is unexpectedly NULL, aborting the GeoTracer service");
     return false;
    }
   else
    {
     isAdvertisingStarting = true;

     // Generate a new random signature to broadcast
     generateRandomSignature();

     // Build the BLE advertising settings
     AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
             .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)      // Corresponds to an advertising period of approximately 100ms
             .setConnectable(false)                                               // Non-connectable advertising
             .setTimeout(0)                                                       // No advertising time limit
             .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)        // Use medium power
             .build();

     // Build the BLE advertising data
     AdvertiseData advertiseData = new AdvertiseData.Builder()
             .setIncludeDeviceName(false)                                         // Do not append the device name
             .setIncludeTxPowerLevel(false)                                       // Do not append the transmission power level (the Measured Power at 1m instead would be great...)
             .addServiceData(GeotracerService.CONTACT_TRACING_SERVICE,signature)  // Append the signature associated to the Contact Tracing Service UUID
             .build();

     // Start advertising the signature (indefinitely)
     bluetoothAdvertiser.startAdvertising(advertiseSettings,advertiseData,advertisingCallback);
     return true;
    }
  }

 // Stop advertising the signature, returning the result of the operation
 boolean stopAdvertising()
  {
   // Check the Advertising to be started
   if(!isAdvertising)
    {
     Log.w(TAG,"Signature Advertising is already stopped");
     return false;
    }

   // Assert the Bluetooth Advertiser object to be present
   if(bluetoothAdvertiser == null)
    {
     geotracerService.exitWithError(TAG,"The Bluetooth Advertiser is unexpectedly NULL, aborting the GeoTracer service");
     return false;
    }
   else
    {
     // Stop the TimerTask for changing the signature
     if(signatureChanger != null)
      signatureChanger.cancel();

     // Clear the current signature
     signature = null;

     // Stop the Bluetooth advertising
     bluetoothAdvertiser.stopAdvertising(advertisingCallback);
     isAdvertising = false;
     Log.i(TAG,"Bluetooth Advertising Stopped");
     return true;
    }
  }

 // Changes the random signature being advertised, returning the result of the operation
 boolean resetSignature()
  {
   // The advertising service must be enabled for changing the signature
   if(!stopAdvertising())
    Log.e(TAG,"Cannot change signature if the advertising service is not enabled");
   else

    // If the advertising service cannot be restarted, raise an error (this should NEVER happen)
    if(!startAdvertising())
     Log.e(TAG,"Cannot restart advertising with new signature (probable error in the logic");
    else
     {
      Log.i(TAG,"Device Signature successfully reset (new signature = " + GeotracerService.byteArrayToHex(signature));
      return true;
     }
   return false;
  }

 // Returns the currently advertised user signature as a String in hexadecimal format
 // NOTE: Returns null if advertising is not started
 String getSignature()
  { return GeotracerService.byteArrayToHex(signature); }

 // Returns true is the service is currently advertising the user's random signature
 boolean isAdvertising()
  { return isAdvertising; }

 /*=============================================================================================================================================*
  |                                                         PRIVATE METHODS                                                                     |
  *=============================================================================================================================================*/

 // Generates a new random signature on SIGNATURE_SIZE bits
 private void generateRandomSignature()
  {
   // Generate a secure RNG object
   SecureRandom random = new SecureRandom();

   // Generate a random signature
   signature = new byte[SIGNATURE_SIZE];
   random.nextBytes(signature);
   Log.i(TAG,"Generated signature  = " + GeotracerService.byteArrayToHex(signature));
  }

 // Callback Object used by the Android OS to notify on bluetooth advertisements status changes
 private final AdvertiseCallback advertisingCallback = new AdvertiseCallback()
 {
  // Bluetooth advertising successfully started
  @Override
  public void onStartSuccess(AdvertiseSettings settingsInEffect)
   {
    isAdvertising = true;
    isAdvertisingStarting = false;

    /* TODO: Push the new signature into the "Own Signatures" bucket
    Nicola.OwnSignatures.push(signature) */

    // Start the TimerTask for changing the user random signature at a fixed-rate of SIGNATURE_VALIDITY_PERIOD
    signatureChanger = new Timer();
    signatureChanger.scheduleAtFixedRate(new signatureChangerTask(),SIGNATURE_VALIDITY_PERIOD,SIGNATURE_VALIDITY_PERIOD);

    Log.i(TAG,"Bluetooth Advertising Started");
    super.onStartSuccess(settingsInEffect);
   }

  // Bluetooth advertising failed
  @Override
  public void onStartFailure(int errorCode)
   {
    // The ADVERTISE_FAILED_ALREADY_STARTED error should NEVER occur
    if(errorCode == ADVERTISE_FAILED_ALREADY_STARTED)
     Log.e(TAG,"Bluetooth Advertising Already Started! (error in the logic)");
    else
     {
      isAdvertisingStarting = false;

      // Clear the current signature
      signature = null;

      Log.e(TAG,"Bluetooth Advertising Failed! Code = "+errorCode);
     }
    super.onStartFailure(errorCode);
   }
 };

 // Inner class for changing the signature every SIGNATURE_VALIDITY_PERIOD
 private class signatureChangerTask extends TimerTask
  {
   public void run()
    { resetSignature(); }
  }

}