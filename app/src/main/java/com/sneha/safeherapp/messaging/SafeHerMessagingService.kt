package com.sneha.safeherapp.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sneha.safeherapp.utils.NotificationHelper

class SafeHerMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.let { data ->
            val title = data["title"] ?: remoteMessage.notification?.title ?: "SafeHer Alert"
            val body = data["body"] ?: remoteMessage.notification?.body ?: ""
            val type = data["type"] ?: "DEFAULT"
            val childId = data["childId"] ?: ""

            NotificationHelper.showNotification(
                context = applicationContext,
                title = title,
                message = body,
                type = type,
                childId = childId
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
    }
}
