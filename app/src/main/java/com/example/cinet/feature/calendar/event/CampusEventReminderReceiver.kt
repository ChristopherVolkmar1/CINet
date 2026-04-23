package com.example.cinet.feature.calendar.event

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.cinet.core.notifications.AppNotification
import com.example.cinet.core.notifications.NotificationHelper
import com.example.cinet.core.notifications.NotificationType

/** Receives scheduled campus event reminder alarms and shows the notification. */
class CampusEventReminderReceiver : BroadcastReceiver() {

    /** Builds and displays the reminder notification when the alarm fires. */
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createChannel(context)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()

        NotificationHelper.showNotification(
            context = context,
            notification = AppNotification(
                title = title,
                message = message,
                type = NotificationType.EVENT,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    companion object {
        const val EXTRA_TITLE = "campus_event_title"
        const val EXTRA_MESSAGE = "campus_event_message"
    }
}
