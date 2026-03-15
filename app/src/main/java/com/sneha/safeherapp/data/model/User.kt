package com.sneha.safeherapp.data.model

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "" // "User" or "Guardian"
)
