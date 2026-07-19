package com.sneha.safeherapp.utils

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sneha.safeherapp.location.SosLocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

object SosManager {
    private const val TAG = "SosTrigger"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sosScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var lastTriggerTime = 0L
    private const val COOLDOWN_MS = 15000L

    fun triggerSos(context: Context, onStarted: () -> Unit = {}, onComplete: (Boolean) -> Unit = {}) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTriggerTime < COOLDOWN_MS) {
            Log.d(TAG, "SOS Triggered: Cooldown active. Ignoring.")
            return
        }
        
        val userId = auth.currentUser?.uid ?: run {
            Log.e(TAG, "SOS Triggered: User not authenticated")
            onComplete(false)
            return
        }

        lastTriggerTime = currentTime
        Log.d(TAG, "SOS Triggered for user: $userId")
        vibrate(context)
        onStarted()

        sosScope.launch {
            try {
                // 1. Get User Name
                val userDoc = db.collection("users").document(userId).get().await()
                val userName = userDoc.getString("fullName") ?: userDoc.getString("name") ?: "A SafeHer User"

                val locationManager = SosLocationManager(context)
                
                // 2. Location
                var location = locationManager.getLastKnownLocation()
                if (location == null) {
                    Log.d(TAG, "Last known location unavailable, fetching fresh location")
                    location = locationManager.getFreshLocation()
                }
                
                if (location != null) {
                    Log.d(TAG, "Location Retrieved: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.w(TAG, "Location unavailable for SOS")
                }

                // 3. Record SOS event in Firebase
                val sosEvent = hashMapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "latitude" to location?.latitude,
                    "longitude" to location?.longitude,
                    "status" to "active"
                )
                val eventRef = db.collection("sos_events").add(sosEvent).await()
                Log.d(TAG, "SOS event recorded in Firebase: ${eventRef.id}")

                // 4. Notify Guardians and send SMS in parallel
                launch {
                    notifyGuardians(userId, userName, location)
                }

                // 5. Get Contacts and Send SMS
                val contactsSnapshot = db.collection("users").document(userId)
                    .collection("contacts").get().await()
                
                if (!contactsSnapshot.isEmpty) {
                    sendAlerts(userName, location, contactsSnapshot.documents)
                } else {
                    Log.w(TAG, "No emergency contacts found for SOS")
                }

                onComplete(true)

                // 6. Background: Request Fresh Location and update if improved
                launch {
                    val freshLocation = locationManager.getFreshLocation()
                    if (freshLocation != null && (location == null || isSignificantlyDifferent(location, freshLocation))) {
                        Log.d(TAG, "Improved Location Retrieved: ${freshLocation.latitude}, ${freshLocation.longitude}")
                        eventRef.update(mapOf(
                            "latitude" to freshLocation.latitude,
                            "longitude" to freshLocation.longitude,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ))
                        sendAlerts(userName, freshLocation, contactsSnapshot.documents, isUpdate = true)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "SOS Error: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    private suspend fun notifyGuardians(userId: String, userName: String, location: Location?) {
        try {
            val connections = db.collection("user_connections")
                .whereEqualTo("childUid", userId)
                .whereEqualTo("status", "accepted")
                .get()
                .await()

            for (doc in connections.documents) {
                val guardianUid = doc.getString("guardianUid")
                if (guardianUid != null) {
                    db.collection("guardian_users").document(guardianUid)
                        .collection("children").whereEqualTo("childUid", userId).get()
                        .addOnSuccessListener { childSnapshot ->
                            for (childDoc in childSnapshot.documents) {
                                childDoc.reference.update(mapOf(
                                    "isSafe" to false,
                                    "lastSosTime" to FieldValue.serverTimestamp(),
                                    "latitude" to location?.latitude,
                                    "longitude" to location?.longitude
                                ))
                            }
                        }
                    
                    val alert = hashMapOf(
                        "childUid" to userId,
                        "childName" to userName,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "latitude" to location?.latitude,
                        "longitude" to location?.longitude,
                        "type" to "SOS"
                    )
                    db.collection("guardian_users").document(guardianUid)
                        .collection("alerts").add(alert)
                    
                    Log.d(TAG, "Guardian Notified: $guardianUid")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify guardians: ${e.message}")
        }
    }

    private fun sendAlerts(userName: String, location: Location?, contacts: List<com.google.firebase.firestore.DocumentSnapshot>, isUpdate: Boolean = false) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val locationLink = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Location currently unavailable"
        }

        val prefix = if (isUpdate) "📍 UPDATED LOCATION ALERT" else "🚨 SAFEHER EMERGENCY ALERT 🚨"
        
        val message = """
            $prefix
            
            $userName ${if (isUpdate) "is still at" else "may be in danger and has triggered an SOS alert"}.
            
            Time: $timestamp
            📍 ${if (isUpdate) "Latest" else "Live"} Location:
            $locationLink
            ${if (location != null) "Lat: ${location.latitude}\nLong: ${location.longitude}" else ""}
            
            Please contact them immediately.
        """.trimIndent()

        for (doc in contacts) {
            val phone = doc.getString("phoneNumber")
            if (!phone.isNullOrEmpty()) {
                try {
                    SmsHelper.sendSms(phone, message)
                    Log.d(TAG, "SMS Sent to $phone")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send SMS to $phone: ${e.message}")
                }
            }
        }
    }

    private fun isSignificantlyDifferent(old: Location, new: Location): Boolean {
        return old.distanceTo(new) > 50
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }
}
