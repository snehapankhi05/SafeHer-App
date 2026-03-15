package com.sneha.safeherapp.model

data class FakeCallerProfile(
    val id: String,
    var name: String,
    var audioPath: String? = null,
    var isActive: Boolean = false
)
