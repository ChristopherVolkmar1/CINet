package com.example.cinet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ClassReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createChannel(context)

        val title = intent.getStringExtra("title") ?: "Upcoming Class"
        val message = intent.getStringExtra("message") ?: "Your class starts soon."

        NotificationHelper.showNotification(
            context = context,
            notification = AppNotification(
                title = title,
                message = message,
                type = NotificationType.REMINDER,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}