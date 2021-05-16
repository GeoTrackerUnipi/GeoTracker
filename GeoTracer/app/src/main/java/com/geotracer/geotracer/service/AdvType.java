package com.geotracer.geotracer.service;

// The type of a received bluetooth advertisement
enum AdvType
 {
  ADV_TYPE_SIG  { @Override public String toString() { return "ADV_TYPE_SIG"; } },   // Signature Bluetooth Advertisement
  ADV_TYPE_MAC  { @Override public String toString() { return "ADV_TYPE_MAC"; } }    // Other Bluetooth Advertisement
 }