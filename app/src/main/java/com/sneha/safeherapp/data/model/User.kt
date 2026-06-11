package com.sneha.safeherapp.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "", // "user" or "guardian"
    val createdAt: Timestamp? = null,
    val batteryLevel: Int? = null,
    val lastUpdated: Timestamp? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null
)
