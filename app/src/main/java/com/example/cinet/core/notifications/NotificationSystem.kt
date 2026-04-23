package com.example.cinet.core.notifications

import android.Manifest
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
import com.example.cinet.feature.settings.AppSettings

// Data model for representing notifications in the app
data class AppNotification(
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long,
    val conversationId: String = "", // used to route taps to the right conversation (future)
)

// Notification categories — each maps to its own channel
enum class NotificationType {
    MESSAGE,  // regular chat messages
    INVITE,   // study_invite / event_invite messages
    REMINDER, // class and assignment reminders
    EVENT     // campus events
}

object NotificationHelper {

    private const val CHANNEL_MESSAGES = "cinet_messages"
    private const val CHANNEL_INVITES = "cinet_invites"
    private const val CHANNEL_REMINDERS = "cinet_reminders"

    /** Creates all notification channels. Safe to call multiple times — Android deduplicates. */
    fun createChannels(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "New chat messages" }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_INVITES,
                "Invites",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Study and event invites from friends" }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Class and assignment reminders" }
        )
    }

    /** Legacy single-channel create — kept for compatibility with existing receivers. */
    fun createChannel(context: Context) = createChannels(context)

    fun showNotification(
        context: Context,
        notification: AppNotification,
        contentIntent: PendingIntent? = null
    ) {
        if (!AppSettings.notificationsEnabled) {
            Log.d("NotificationHelper", "Notifications disabled by user settings.")
            return
        }

        if (!PermissionManager.hasAllPermissions(context)) {
            Log.e("NotificationHelper", "Cannot show notification: permission denied.")
            return
        }

        val channelId = when (notification.type) {
            NotificationType.MESSAGE -> CHANNEL_MESSAGES
            NotificationType.INVITE -> CHANNEL_INVITES
            NotificationType.REMINDER, NotificationType.EVENT -> CHANNEL_REMINDERS
        }

        val priority = when (notification.type) {
            NotificationType.MESSAGE, NotificationType.INVITE -> NotificationCompat.PRIORITY_HIGH
            NotificationType.REMINDER -> NotificationCompat.PRIORITY_DEFAULT
            NotificationType.EVENT -> NotificationCompat.PRIORITY_LOW
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(priority)
            .setAutoCancel(true)

        contentIntent?.let { builder.setContentIntent(it) }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(
                notification.timestamp.hashCode(),
                builder.build()
            )
            Log.d("NotificationHelper", "Showed notification: ${notification.title}")
        }
    }
}