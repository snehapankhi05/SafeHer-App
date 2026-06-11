package com.sneha.safeherapp.ui.home

import com.google.firebase.Timestamp

data class Child(
    val id: String = "",
    val childUid: String = "", // Real Firebase UID of the child
    val name: String = "",      // Name assigned by guardian
    val fullName: String = "",  // Full name from child's profile
    val email: String = "",
    val phone: String = "",
    val relationship: String = "",
    val status: String = "Pending Connection",
    val inviteCode: String = "",
    val createdAt: Timestamp? = null,
    val lastUpdated: Timestamp? = null,
    // Maintaining existing UI fields
    val location: String = "Location Hidden",
    val battery: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isSafe: Boolean = true
)
