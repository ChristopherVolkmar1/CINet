package com.example.cinet.core.notifications

import android.app.PendingIntent
import android.content.Intent
import com.example.cinet.app.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FireBaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val type = remoteMessage.data["type"] ?: "text"
        val conversationId = remoteMessage.data["conversationId"] ?: ""
        val isInvite = type == "study_invite" || type == "event_invite"

        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.notification?.title
                ?: if (isInvite) "New Invite" else "New Message"
            val body = remoteMessage.notification?.body ?: ""
            val notificationType =
                if (isInvite) NotificationType.INVITE else NotificationType.MESSAGE

            // Build a PendingIntent so tapping the notification opens MainActivity
            // and passes conversationId so the app navigates straight to that conversation.
            val tapIntent = Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_CONVERSATION_ID, conversationId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                conversationId.hashCode(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            NotificationHelper.createChannels(this)
            NotificationHelper.showNotification(
                context = this,
                notification = AppNotification(
                    title = title,
                    message = body,
                    type = notificationType,
                    timestamp = System.currentTimeMillis(),
                    conversationId = conversationId
                ),
                contentIntent = pendingIntent
            )
            return
        }

        remoteMessage.notification?.let {
            android.util.Log.d("FCM", "Background notification received: ${it.title}")
        }
    }

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