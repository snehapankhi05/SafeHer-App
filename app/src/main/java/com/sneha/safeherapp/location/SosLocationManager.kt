package com.sneha.safeherapp.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class SosLocationManager(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            // Try last known location first for speed
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null && isLocationFresh(lastLocation)) {
                lastLocation
            } else {
                // Request fresh high accuracy location
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isLocationFresh(location: Location): Boolean {
        val age = System.currentTimeMillis() - location.time
        return age < 60_000 // Less than 1 minute old
    }
}
