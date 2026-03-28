package com.example.cinet

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
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
        // Safely verify whether permissions have been granted through the PermissionManager
        if (!PermissionManager.hasAllPermissions(context)) {
            Log.e("NotificationHelper", "Cannot show notification: Permission Denied")
            return
        }
        // Configure Notification Appearance and Behavior
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)    // Default System Icon
            .setContentTitle(notification.title)                // Notification Title
            .setContentText(notification.message)               // Notification Body Text
            .setPriority(
                // Set Priority Dynamically based on notification Type
                when (notification.type) {
                    NotificationType.MESSAGE -> NotificationCompat.PRIORITY_HIGH
                    NotificationType.REMINDER -> NotificationCompat.PRIORITY_DEFAULT
                    NotificationType.EVENT -> NotificationCompat.PRIORITY_LOW
                }
            )
        // Send Notification to system
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {

            NotificationManagerCompat.from(context).notify(
                notification.timestamp.hashCode(),
                builder.build()
            )
            // Debug Log to confirm Notification Trigger
            Log.d("NotificationTest", "Showing notification: ${notification.title}")
        }
    }
}