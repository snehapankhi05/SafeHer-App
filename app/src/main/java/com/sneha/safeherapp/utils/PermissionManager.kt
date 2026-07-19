package com.sneha.safeherapp.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionManager {

    val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllPermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getPermissionsForFeature(feature: Feature): List<String> {
        return when (feature) {
            Feature.SOS -> listOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            Feature.MAPS, Feature.LIVE_LOCATION, Feature.GUARDIAN_TRACKING -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            Feature.EMERGENCY_CONTACTS -> listOf(Manifest.permission.READ_CONTACTS)
            Feature.FAKE_CALL -> listOf(Manifest.permission.CALL_PHONE)
            Feature.VOICE_RECORDING -> listOf(Manifest.permission.RECORD_AUDIO)
            Feature.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else emptyList()
            }
        }
    }

    enum class Feature(val displayName: String, val description: String) {
        SOS("SOS", "SMS, Calling, and Location permissions are needed to alert your contacts in an emergency."),
        MAPS("Maps", "Location permission is needed to show your current position and safety zones."),
        LIVE_LOCATION("Live Location", "Location permission is needed to share your real-time position with your guardians."),
        GUARDIAN_TRACKING("Guardian Tracking", "Location permission is needed to monitor safety status."),
        EMERGENCY_CONTACTS("Emergency Contacts", "Contacts permission is needed to easily add trusted people from your phonebook."),
        FAKE_CALL("Fake Call", "Phone permission is needed to simulate a realistic incoming call."),
        VOICE_RECORDING("Voice Recording", "Microphone permission is needed to record custom voices for fake calls."),
        NOTIFICATIONS("Notifications", "Notification permission is needed to send you important safety alerts.")
    }
}
