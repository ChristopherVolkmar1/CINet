package com.example.cinet.core.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FireBaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val type = remoteMessage.data["type"] ?: "text"
        val conversationId = remoteMessage.data["conversationId"] ?: ""

        val isInvite = type == "study_invite" || type == "event_invite"

        // Data payload — used when app is in foreground (system ignores notification payload)
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.notification?.title
                ?: if (isInvite) "New Invite" else "New Message"
            val body = remoteMessage.notification?.body ?: ""

            val notificationType = if (isInvite) NotificationType.INVITE else NotificationType.MESSAGE

            NotificationHelper.createChannels(this)
            NotificationHelper.showNotification(
                context = this,
                notification = AppNotification(
                    title = title,
                    message = body,
                    type = notificationType,
                    timestamp = System.currentTimeMillis(),
                    conversationId = conversationId
                )
            )
            return
        }

        // Notification-only payload fallback (app in background, system delivered it — log only)
        remoteMessage.notification?.let {
            android.util.Log.d("FCM", "Background notification received: ${it.title}")
        }
    }

    /**
     * Called when FCM assigns a new token to this device.
     * Saves it to the current user's Firestore document so the Cloud Function
     * can look it up when sending push notifications.
     */
    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnFailureListener { e ->
                android.util.Log.e("FCM", "Failed to save FCM token: ${e.message}")
            }
    }
}