package com.sneha.safeherapp.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object SosPrefs {
    private const val BASE_PREFS_NAME = "sos_prefs"
    private const val KEY_HARDWARE_TRIGGER_ENABLED = "hardware_trigger_enabled"

    private fun getUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun getPrefsName(): String {
        val userId = getUserId() ?: "default_user"
        return "${BASE_PREFS_NAME}_$userId"
    }

    fun isHardwareTriggerEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(getPrefsName(), Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HARDWARE_TRIGGER_ENABLED, false)
    }

    fun setHardwareTriggerEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(getPrefsName(), Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HARDWARE_TRIGGER_ENABLED, enabled).apply()
    }
}
