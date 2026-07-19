package com.sneha.safeherapp.viewmodel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.sneha.safeherapp.data.model.User
import com.sneha.safeherapp.model.SafetyZone
import com.sneha.safeherapp.utils.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChildDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val context = application.applicationContext

    private val _childUser = MutableStateFlow<User?>(null)
    val childUser = _childUser.asStateFlow()

    private val _safetyZones = MutableStateFlow<List<SafetyZone>>(emptyList())
    val safetyZones = _safetyZones.asStateFlow()

    private var childListener: ListenerRegistration? = null
    private var zonesListener: ListenerRegistration? = null

    // Track geofencing state: Map of ZoneID to "isInside"
    private val zoneStates = mutableMapOf<String, Boolean>()

    fun startListening(childUid: String) {
        // Listen to child's live data
        childListener?.remove()
        childListener = db.collection("users").document(childUid)
            .addSnapshotListener { snapshot, _ ->
                val user = snapshot?.toObject(User::class.java)
                _childUser.value = user
                user?.let { checkGeofencing(it) }
            }

        // Listen to safety zones for this child
        zonesListener?.remove()
        zonesListener = db.collection("safety_zones")
            .whereEqualTo("childId", childUid)
            .addSnapshotListener { snapshot, _ ->
                val zones = snapshot?.toObjects(SafetyZone::class.java) ?: emptyList()
                _safetyZones.value = zones
            }
    }

    private fun checkGeofencing(user: User) {
        val childLat = user.latitude ?: return
        val childLng = user.longitude ?: return
        val childName = user.fullName.ifBlank { "Child" }

        _safetyZones.value.forEach { zone ->
            val results = FloatArray(1)
            Location.distanceBetween(childLat, childLng, zone.latitude, zone.longitude, results)
            val distance = results[0]
            val isInside = distance <= zone.radius

            val previousInside = zoneStates[zone.id]
            
            if (previousInside != null && previousInside != isInside) {
                // State changed
                val title = if (isInside) "Entry Alert" else "Exit Alert"
                val zoneType = zone.zoneType // "Safe" or "Unsafe"
                
                val message = when {
                    isInside && zoneType == "Safe" -> "$childName has entered the Safe Zone: ${zone.placeName}"
                    !isInside && zoneType == "Safe" -> "$childName has left the Safe Zone: ${zone.placeName}"
                    isInside && zoneType == "Unsafe" -> "⚠️ Alert: $childName has entered an Unsafe Zone: ${zone.placeName}"
                    !isInside && zoneType == "Unsafe" -> "$childName has left the Unsafe Zone: ${zone.placeName}"
                    else -> ""
                }

                if (message.isNotEmpty()) {
                    NotificationHelper.showZoneNotification(context, title, message)
                    Log.d("Geofencing", message)
                }
            }
            
            zoneStates[zone.id] = isInside
        }
    }

    override fun onCleared() {
        super.onCleared()
        childListener?.remove()
        zonesListener?.remove()
    }
}
