package com.sneha.safeherapp.util

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sneha.safeherapp.model.FakeCallerProfile

object FakeCallPrefs {
    private const val BASE_PREFS_NAME = "fake_call_profiles"
    private const val KEY_PROFILES = "profiles_list"

    private fun getUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun getPrefsName(): String {
        val userId = getUserId() ?: "default_user"
        return "${BASE_PREFS_NAME}_$userId"
    }

    fun getProfiles(context: Context): List<FakeCallerProfile> {
        val userId = getUserId()
        if (userId == null) {
            return emptyList()
        }
        val prefs = context.getSharedPreferences(getPrefsName(), Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        val type = object : TypeToken<List<FakeCallerProfile>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun saveProfiles(context: Context, profiles: List<FakeCallerProfile>) {
        val userId = getUserId()
        if (userId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val prefs = context.getSharedPreferences(getPrefsName(), Context.MODE_PRIVATE)
        val json = Gson().toJson(profiles)
        prefs.edit().putString(KEY_PROFILES, json).apply()
    }

    fun getActiveProfile(context: Context): FakeCallerProfile? {
        return getProfiles(context).find { it.isActive }
    }
}
