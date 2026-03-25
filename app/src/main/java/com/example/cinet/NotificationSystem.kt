package com.example.cinet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cinet.AppNotification

object NotificationHelper {
    // Unique ID For app's notification channel
    private const val CHANNEL_ID = "cinet_channel"
    // Creates Notification Channel as required for Android 8+
    fun createChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CINet Notifications",
                NotificationManager.IMPORTANCE_HIGH // High Importance = visible + heads up notification
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    // Builds and displays a notification based on AppNotification Data
    fun showNotification(context: Context, notification: AppNotification) {
        // Configure Notification Appearance and Behavior
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)    // Default System Icon
            .setContentTitle(notification.title)                // Notification Title
            .setContentText(notification.message)               // Notification Body Text
            .setPriority(
                // Set Priority Dynamically based on ntoificaiton Type
                when (notification.type) {
                    NotificationType.MESSAGE -> NotificationCompat.PRIORITY_HIGH
                    NotificationType.REMINDER -> NotificationCompat.PRIORITY_DEFAULT
                    NotificationType.EVENT -> NotificationCompat.PRIORITY_LOW
                }
            )
        // Send Notification to system
        val manager = NotificationManagerCompat.from(context)
        manager.notify(notification.timestamp.toInt(), builder.build())
        // Debug Log to confirm Notification Trigger
        Log.d("NotificationTest", "Showing notification: ${notification.title}")
    }
}