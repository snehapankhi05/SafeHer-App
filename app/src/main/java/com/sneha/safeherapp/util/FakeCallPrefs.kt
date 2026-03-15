package com.sneha.safeherapp.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sneha.safeherapp.model.FakeCallerProfile

object FakeCallPrefs {
    private const val PREFS_NAME = "safeher_settings"
    private const val KEY_PROFILES = "fake_call_profiles"

    fun getProfiles(context: Context): List<FakeCallerProfile> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        val type = object : TypeToken<List<FakeCallerProfile>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun saveProfiles(context: Context, profiles: List<FakeCallerProfile>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(profiles)
        prefs.edit().putString(KEY_PROFILES, json).apply()
    }

    fun getActiveProfile(context: Context): FakeCallerProfile? {
        return getProfiles(context).find { it.isActive }
    }
}
