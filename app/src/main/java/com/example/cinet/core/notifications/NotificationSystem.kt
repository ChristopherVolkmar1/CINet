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
import com.example.cinet.feature.settings.AppSettings


data class AppNotification(
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long,
    val conversationId: String = "",
)


enum class NotificationType {
    MESSAGE,
    INVITE,
    REMINDER,
    EVENT
}


object NotificationHelper {


    private const val CHANNEL_MESSAGES = "cinet_messages"
    private const val CHANNEL_INVITES = "cinet_invites"
    private const val CHANNEL_REMINDERS = "cinet_reminders"


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
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Class and assignment reminders" }
        )
    }


    fun createChannel(context: Context) = createChannels(context)


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


        val channelId = when (notification.type) {
            NotificationType.MESSAGE -> CHANNEL_MESSAGES
            NotificationType.INVITE -> CHANNEL_INVITES
            NotificationType.REMINDER -> CHANNEL_REMINDERS
            NotificationType.EVENT -> CHANNEL_REMINDERS
        }


        val priority = when (notification.type) {
            NotificationType.MESSAGE -> NotificationCompat.PRIORITY_HIGH
            NotificationType.INVITE -> NotificationCompat.PRIORITY_HIGH
            NotificationType.REMINDER -> NotificationCompat.PRIORITY_HIGH
            NotificationType.EVENT -> NotificationCompat.PRIORITY_DEFAULT
        }


        val category = when (notification.type) {
            NotificationType.MESSAGE -> NotificationCompat.CATEGORY_MESSAGE
            NotificationType.INVITE -> NotificationCompat.CATEGORY_SOCIAL
            NotificationType.REMINDER -> NotificationCompat.CATEGORY_REMINDER
            NotificationType.EVENT -> NotificationCompat.CATEGORY_EVENT
        }


        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(priority)
            .setCategory(category)
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
