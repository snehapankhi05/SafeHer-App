package com.sneha.safeherapp.location

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sneha.safeherapp.R

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val NOTIFICATION_ID = 2002
    private val CHANNEL_ID = "location_sharing_channel"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationInFirebase(location)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SafeHer Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeHer is active")
            .setContentText("Sharing live location with your guardians")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000L) // 30 seconds
            .setMinUpdateIntervalMillis(15000L)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
        
        return START_STICKY
    }

    private fun updateLocationInFirebase(location: Location) {
        val userId = auth.currentUser?.uid ?: return
        
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val updates = hashMapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "batteryLevel" to batteryLevel,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        // Update main user profile
        db.collection("users").document(userId).update(updates as Map<String, Any>)
            .addOnFailureListener { e -> Log.e("LocationService", "Update failed", e) }

        // Sync to guardian records
        db.collection("user_connections")
            .whereEqualTo("childUid", userId)
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { connDoc ->
                    val guardianUid = connDoc.getString("guardianUid") ?: return@forEach
                    
                    // The guardian's view of this child uses different field names (battery instead of batteryLevel)
                    val guardianUpdates = hashMapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "battery" to batteryLevel,
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )

                    db.collection("guardian_users").document(guardianUid)
                        .collection("children")
                        .whereEqualTo("childUid", userId)
                        .get()
                        .addOnSuccessListener { childSnapshot ->
                            childSnapshot.documents.forEach { doc ->
                                doc.reference.update(guardianUpdates as Map<String, Any>)
                            }
                        }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
