package com.geotracer.geotracer.service;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

// This class represents a set of advertisements received from the same Signature or MAC address
class AdvList
{
 /*=============================================================================================================================================*
 |                                                             ATTRIBUTES                                                                       |
 *=============================================================================================================================================*/
 AdvType type;                     // The type of the advertisement stored in the "samples" list
 ArrayList<AdvSample> samples;     // The list of advertising samples
 ReentrantLock mutex;              // A mutual exclusion semaphore used for handling concurrency on the "samples" list
 // Object signatureInDB;          // The information on the signature stored in the database, if any  TODO: Fetch from the database when the advList is created (only if type == ADV_TYPE_SIG)

 /*=============================================================================================================================================*
 |                                                    PACKAGE-VISIBILITY METHODS                                                                |
 *=============================================================================================================================================*/

 // Constructor
 AdvList(AdvType type)
  {
   this.type = type;
   this.samples = new ArrayList<>();
   this.mutex = new ReentrantLock();
   // this.signatureInDB = ... TODO: Fetch from the database when the advList is created (only if type == ADV_TYPE_SIG)
  }

 // String serializer
 @Override
 public String toString()
  { return "{type = " + type + ", samples = [" + samples + "]}"; }
}
