package com.sneha.safeherapp.utils

import android.content.Context
import android.util.Log
import android.view.KeyEvent

class VolumeTriggerHandler(private val context: Context) {
    private val TAG = "SosTrigger"
    
    private val pattern = mutableListOf<Int>()
    private var lastPressTime = 0L
    private const val PATTERN_TIMEOUT_MS = 5000L
    
    // Required pattern: Vol Up (24), Vol Down (25), Vol Up (24)
    private val targetPattern = listOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP)

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        val currentTime = System.currentTimeMillis()
        
        // Reset pattern if timeout reached
        if (currentTime - lastPressTime > PATTERN_TIMEOUT_MS) {
            pattern.clear()
        }
        
        lastPressTime = currentTime
        pattern.add(keyCode)
        
        // Keep only the last 3 presses
        if (pattern.size > 3) {
            pattern.removeAt(0)
        }

        Log.d(TAG, "Volume Key Pressed: $keyCode, Current Pattern: $pattern")

        if (pattern == targetPattern) {
            Log.d(TAG, "Volume Pattern Detected")
            pattern.clear()
            SosManager.triggerSos(context, 
                onStarted = {
                    Log.d(TAG, "SOS Triggered via Volume Pattern")
                }
            )
            return true // Consume the event
        }
        
        return false
    }
}
