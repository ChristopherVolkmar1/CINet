package com.example.cinet.feature.calendar.event

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.cinet.core.notifications.AppNotification
import com.example.cinet.core.notifications.NotificationHelper
import com.example.cinet.core.notifications.NotificationType
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Schedules and cancels reminder alarms for live campus events. */
object CampusEventReminderScheduler {
    private const val DEFAULT_MINUTES_BEFORE = 30L

    /** Schedules a reminder and returns true when the request was accepted. */
    fun scheduleReminder(
        context: Context,
        event: EventItem,
        minutesBefore: Long = DEFAULT_MINUTES_BEFORE
    ): Boolean {
        val eventStartDateTime = getEventStartDateTime(event) ?: return false
        if (!isEventInFuture(eventStartDateTime)) {
            return false
        }

        val triggerDateTime = buildTriggerDateTime(event, eventStartDateTime, minutesBefore)
        return if (shouldNotifyImmediately(triggerDateTime)) {
            showImmediateReminder(context, event, eventStartDateTime)
            true
        } else {
            scheduleAlarmReminder(context, event, triggerDateTime)
            true
        }
    }

    /** Cancels the reminder alarm for one campus event occurrence. */
    fun cancelReminder(context: Context, event: EventItem) {
        val pendingIntent = buildPendingIntent(context, event)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    /** Converts the event's start time into a local date time, or returns null when missing. */
    private fun getEventStartDateTime(event: EventItem): LocalDateTime? {
        val eventStartMillis = event.startEpochMillis ?: return null
        return toLocalDateTime(eventStartMillis)
    }

    /** Returns true when the event has not started yet. */
    private fun isEventInFuture(eventStartDateTime: LocalDateTime): Boolean {
        return eventStartDateTime.isAfter(LocalDateTime.now())
    }

    /** Returns true when the reminder time has already passed and should fire now. */
    private fun shouldNotifyImmediately(triggerDateTime: LocalDateTime): Boolean {
        return !triggerDateTime.isAfter(LocalDateTime.now())
    }

    /** Schedules the alarm-based reminder for one campus event. */
    private fun scheduleAlarmReminder(
        context: Context,
        event: EventItem,
        triggerDateTime: LocalDateTime
    ) {
        val pendingIntent = buildPendingIntent(context, event)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        scheduleAlarm(alarmManager, triggerMillis, pendingIntent)
    }

    /** Builds the broadcast PendingIntent used to show the reminder notification. */
    private fun buildPendingIntent(context: Context, event: EventItem): PendingIntent {
        val intent = buildReminderIntent(context, event)
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(event),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Picks the notification time for timed and all-day events. */
    private fun buildTriggerDateTime(
        event: EventItem,
        eventStartDateTime: LocalDateTime,
        minutesBefore: Long
    ): LocalDateTime {
        return if (event.allDay) {
            LocalDateTime.of(eventStartDateTime.toLocalDate(), LocalTime.of(9, 0))
        } else {
            eventStartDateTime.minusMinutes(minutesBefore)
        }
    }

    /** Creates the broadcast intent used by AlarmManager. */
    private fun buildReminderIntent(context: Context, event: EventItem): Intent {
        return Intent(context, CampusEventReminderReceiver::class.java).apply {
            putExtra(CampusEventReminderReceiver.EXTRA_TITLE, event.name)
            putExtra(CampusEventReminderReceiver.EXTRA_MESSAGE, buildReminderMessage(event))
        }
    }

    /** Builds the message text shown in the notification. */
    private fun buildReminderMessage(event: EventItem): String {
        val locationSuffix = event.location.takeIf { it.isNotBlank() }?.let { " at $it" } ?: ""

        return if (event.allDay) {
            "${event.name} is happening today$locationSuffix."
        } else {
            "${event.name} starts at ${event.time}$locationSuffix."
        }
    }

    /** Schedules one alarm using the same fallback strategy already used elsewhere in the app. */
    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    /** Shows an immediate reminder when the lead time already passed but the event has not started yet. */
    private fun showImmediateReminder(
        context: Context,
        event: EventItem,
        eventStartDateTime: LocalDateTime
    ) {
        val minutesUntilEvent = calculateMinutesUntilEvent(eventStartDateTime)
        val message = buildImmediateReminderMessage(event, minutesUntilEvent)

        NotificationHelper.createChannel(context)
        NotificationHelper.showNotification(
            context = context,
            notification = AppNotification(
                title = event.name,
                message = message,
                type = NotificationType.EVENT,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /** Calculates how many minutes remain until the event starts. */
    private fun calculateMinutesUntilEvent(eventStartDateTime: LocalDateTime): Long {
        return Duration.between(LocalDateTime.now(), eventStartDateTime)
            .toMinutes()
            .coerceAtLeast(0)
    }

    /** Builds the notification text for an immediate reminder. */
    private fun buildImmediateReminderMessage(event: EventItem, minutesUntilEvent: Long): String {
        val locationSuffix = event.location.takeIf { it.isNotBlank() }?.let { " at $it" } ?: ""

        return if (event.allDay) {
            "${event.name} is happening today$locationSuffix."
        } else {
            "${event.name} starts in $minutesUntilEvent minutes$locationSuffix."
        }
    }

    /** Converts epoch milliseconds into a local date time. */
    private fun toLocalDateTime(epochMillis: Long): LocalDateTime {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    /** Generates one stable request code per event occurrence. */
    private fun requestCodeFor(event: EventItem): Int {
        return event.id.hashCode()
    }
}
