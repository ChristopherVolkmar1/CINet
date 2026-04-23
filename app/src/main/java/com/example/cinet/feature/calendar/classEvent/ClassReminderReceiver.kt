package com.example.cinet.feature.calendar.classEvent

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.cinet.app.MainActivity
import com.example.cinet.core.notifications.AppNotification
import com.example.cinet.core.notifications.NotificationHelper
import com.example.cinet.core.notifications.NotificationType

class ClassReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createChannel(context)

        val title = intent.getStringExtra("title") ?: "Upcoming Class"
        val message = intent.getStringExtra("message") ?: "Your class starts soon."
        val locationName = intent.getStringExtra("location")

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_MAP_FOR_LOCATION, locationName)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationHelper.showNotification(
            context = context,
            notification = AppNotification(
                title = title,
                message = message,
                type = NotificationType.REMINDER,
                timestamp = System.currentTimeMillis()
            ),
            contentIntent = contentPendingIntent
        )
    }
}