package com.example.cinet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AssignmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createChannel(context)

        val title = intent.getStringExtra("title") ?: "Assignment Due Soon"
        val message = intent.getStringExtra("message") ?: "You have an assignment due soon."

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
