package com.example.cinet

import com.example.cinet.NotificationHelper.showNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FireBaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            val notification = AppNotification(
                title = it.title ?: "New Message",
                message = it.body ?: "",
                type = NotificationType.MESSAGE,
                timestamp = System.currentTimeMillis()
            )
            showNotification(this, notification) // 'this' = Context
        }
    }

    override fun onNewToken(token: String) {
        saveTokenToDatabase(token)
    }
    private fun saveTokenToDatabase(token: String) {
        val db = FirebaseFirestore.getInstance()
        val userId = "current_user_id" // Replace with FirebaseAuth current user ID
        db.collection("users")
            .document(userId)
            .update("fcmToken", token)
    }
}