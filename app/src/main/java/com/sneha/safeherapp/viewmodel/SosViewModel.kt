package com.sneha.safeherapp.viewmodel

import android.content.Context
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

                // 2. Get Location
                val locationManager = SosLocationManager(context)
                val location = locationManager.getCurrentLocation()
                val locationLink = if (location != null) {
                    "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                } else {
                    "Location unavailable"
                }

                // 3. Get Emergency Contacts
                // Note: In current UI, contacts are local. 
                // For SOS to work, they must be in Firestore.
                // For now, let's assume they are stored under users/{userId}/contacts
                val contactsSnapshot = db.collection("users").document(userId)
                    .collection("contacts").get().await()
                
                val message = """
                    🚨 SAFEHER EMERGENCY ALERT 🚨
                    
                    $userName may be in danger and has triggered the SOS alert.
                    
                    📍 Live Location:
                    $locationLink
                    
                    Please contact them immediately.
                """.trimIndent()

                var sentCount = 0
                for (doc in contactsSnapshot.documents) {
                    val phone = doc.getString("phoneNumber")
                    if (!phone.isNullOrEmpty()) {
                        SmsHelper.sendSms(phone, message)
                        sentCount++
                    }
                }

                if (sentCount > 0) {
                    _sosState.value = SosState.Success
                } else {
                    _sosState.value = SosState.Error("No emergency contacts found in your profile.")
                }

            } catch (e: Exception) {
                _sosState.value = SosState.Error(e.localizedMessage ?: "Failed to send SOS")
            }
        }
    }

    fun resetState() {
        _sosState.value = SosState.Idle
    }
}
