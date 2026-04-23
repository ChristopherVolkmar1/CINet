package com.example.cinet.core.notifications

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cinet.core.permissions.PermissionManager
import com.example.cinet.feature.settings.*

object NotificationHelper {
    private const val CHANNEL_ID = "cinet_channel"

    fun createChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CINet Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notification: AppNotification,
        contentIntent: PendingIntent? = null
    ) {
        if (!AppSettings.notificationsEnabled) {
            Log.d("NotificationHelper", "Notifications are currently disabled by user settings.")
            return
        }

        if (!PermissionManager.hasAllPermissions(context)) {
            Log.e("NotificationHelper", "Cannot show notification: Permission Denied")
            return
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(
                when (notification.type) {
                    NotificationType.MESSAGE -> NotificationCompat.PRIORITY_HIGH
                    NotificationType.REMINDER -> NotificationCompat.PRIORITY_DEFAULT
                    NotificationType.EVENT -> NotificationCompat.PRIORITY_LOW
                }
            )
            .setAutoCancel(true)

        contentIntent?.let { builder.setContentIntent(it) }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(
                notification.timestamp.hashCode(),
                builder.build()
            )
            Log.d("NotificationTest", "Showing notification: ${notification.title}")
        }
    }
}