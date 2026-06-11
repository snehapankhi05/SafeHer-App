package com.sneha.safeherapp.model

import com.google.firebase.Timestamp

data class SafetyZone(
    val id: String = "",
    val placeName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 500.0,
    val zoneType: String = "Safe", // "Safe", "Risky", "Unsafe"
    val guardianId: String = "",
    val childId: String = "",
    val timestamp: Timestamp? = null
)
