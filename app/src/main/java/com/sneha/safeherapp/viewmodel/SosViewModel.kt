package com.sneha.safeherapp.viewmodel

import android.content.Context
import android.location.Location
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneha.safeherapp.location.SosLocationManager
import com.sneha.safeherapp.utils.SmsHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class SosState {
    object Idle : SosState()
    object Sending : SosState()
    object Success : SosState()
    data class Error(val message: String) : SosState()
}

class SosViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _sosState = mutableStateOf<SosState>(SosState.Idle)
    val sosState: State<SosState> = _sosState

    fun triggerSos(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _sosState.value = SosState.Sending
            try {
                // 1. Get User Name
                val userDoc = db.collection("users").document(userId).get().await()
                val userName = userDoc.getString("name") ?: "A SafeHer User"

                val locationManager = SosLocationManager(context)
                
                // 2. Immediate Last Known Location
                val lastLocation = locationManager.getLastKnownLocation()
                
                // 3. Get Emergency Contacts
                val contactsSnapshot = db.collection("users").document(userId)
                    .collection("contacts").get().await()
                
                if (contactsSnapshot.isEmpty) {
                    _sosState.value = SosState.Error("No emergency contacts found.")
                    return@launch
                }

                // Initial Send with last known (or unavailable)
                sendAlerts(userName, lastLocation, contactsSnapshot.documents)
                _sosState.value = SosState.Success

                // 4. Background: Request Fresh Location and update if needed
                launch {
                    val freshLocation = locationManager.getFreshLocation()
                    if (freshLocation != null && (lastLocation == null || isSignificantlyDifferent(lastLocation, freshLocation))) {
                        // Send update message if location improved
                        sendAlerts(userName, freshLocation, contactsSnapshot.documents, isUpdate = true)
                    }
                }

            } catch (e: Exception) {
                _sosState.value = SosState.Error(e.localizedMessage ?: "Failed to send SOS")
            }
        }
    }

    private fun sendAlerts(userName: String, location: Location?, contacts: List<com.google.firebase.firestore.DocumentSnapshot>, isUpdate: Boolean = false) {
        val locationLink = if (location != null) {
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Location currently unavailable"
        }

        val prefix = if (isUpdate) "📍 UPDATED LOCATION ALERT" else "🚨 SAFEHER EMERGENCY ALERT 🚨"
        
        val message = """
            $prefix
            
            $userName ${if (isUpdate) "is still at" else "may be in danger and has triggered an SOS alert"}.
            
            📍 ${if (isUpdate) "Latest" else "Live"} Location:
            $locationLink
            
            Please contact them immediately.
        """.trimIndent()

        for (doc in contacts) {
            val phone = doc.getString("phoneNumber")
            if (!phone.isNullOrEmpty()) {
                SmsHelper.sendSms(phone, message)
            }
        }
    }

    private fun isSignificantlyDifferent(old: Location, new: Location): Boolean {
        return old.distanceTo(new) > 50 // More than 50 meters difference
    }

    fun resetState() {
        _sosState.value = SosState.Idle
    }
}
