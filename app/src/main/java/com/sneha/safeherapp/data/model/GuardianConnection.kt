package com.sneha.safeherapp.data.model

import com.google.firebase.Timestamp

data class GuardianConnection(
    val guardianUid: String = "",
    val guardianName: String = "",
    val guardianEmail: String = "",
    val relationship: String = "",
    val childUid: String = "",
    val connectedAt: Timestamp? = null,
    val status: String = "connected",
    val accepted: Boolean = false
)

data class GuardianInvite(
    val inviteCode: String = "",
    val guardianUid: String = "",
    val guardianName: String = "",
    val guardianEmail: String = "",
    val childName: String = "",
    val childPhone: String = "",
    val relationship: String = "",
    val createdAt: Timestamp? = null,
    val status: String = "pending"
)
