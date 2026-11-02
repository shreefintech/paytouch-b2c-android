package com.shreefintech.paytouchconsumer.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shreefintech.paytouchconsumer.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Skipped inside syncToken when logged out — login re-syncs later
        NotificationHelper.syncToken(this, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: getString(R.string.app_name)

        val message = remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: ""

        NotificationHelper.showNotification(this, title, message)
    }
}
