package com.sneha.safeherapp.utils

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.sneha.safeherapp.util.SosPrefs

class SosAccessibilityService : AccessibilityService() {

    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false
    private val TAG = "SosTrigger"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Monitoring Started: Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for key events
    }

    override fun onInterrupt() {
        Log.d(TAG, "Monitoring Interrupted")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!SosPrefs.isHardwareTriggerEnabled(this)) {
            return super.onKeyEvent(event)
        }

        val keyCode = event.keyCode
        val action = event.action

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Log.d(TAG, "Buttons Pressed: KeyCode=$keyCode, Action=$action")

            if (action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) isVolumeUpPressed = true
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) isVolumeDownPressed = true
            } else if (action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) isVolumeUpPressed = false
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) isVolumeDownPressed = false
            }

            if (isVolumeUpPressed && isVolumeDownPressed) {
                Log.d(TAG, "Pattern Detected: Both volume buttons pressed")
                
                // Trigger SOS
                SosManager.triggerSos(this, 
                    onStarted = {
                        Log.d(TAG, "SOS Triggered via hardware buttons")
                    },
                    onComplete = { success ->
                        if (success) Log.d(TAG, "SOS Completed successfully")
                        else Log.e(TAG, "SOS Failed to complete")
                    }
                )
                
                // We reset the states to prevent continuous triggering if held
                isVolumeUpPressed = false
                isVolumeDownPressed = false
                
                // Consume the event so volume UI doesn't pop up during emergency
                return true
            }
        }

        return super.onKeyEvent(event)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Monitoring Stopped: Service Unbound")
        return super.onUnbind(intent)
    }
}
