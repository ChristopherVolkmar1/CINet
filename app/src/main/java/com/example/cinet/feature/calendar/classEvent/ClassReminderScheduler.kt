package com.example.cinet.feature.calendar.classEvent


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.cinet.app.MainActivity
import com.example.cinet.core.notifications.AppNotification
import com.example.cinet.core.notifications.NotificationHelper
import com.example.cinet.core.notifications.NotificationType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


object ClassReminderScheduler {


    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")


    fun scheduleNextReminder(
        context: Context,
        classItem: ClassItem,
        minutesBefore: Int
    ) {
        if (!classItem.remindersEnabled) return


        val nextDateTime = getNextClassDateTime(classItem) ?: return
        val triggerTime = nextDateTime.minusMinutes(minutesBefore.toLong())
        val now = LocalDateTime.now()


        if (!triggerTime.isAfter(now)) {
            val actualMinutesUntilClass = java.time.Duration.between(now, nextDateTime)
                .toMinutes()
                .coerceAtLeast(0)

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_MAP_FOR_LOCATION, classItem.location)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val contentPendingIntent = PendingIntent.getActivity(
                context,
                classItem.id.hashCode(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            NotificationHelper.showNotification(
                context = context,
                notification = AppNotification(
                    title = classItem.name,
                    message = "Starts in $actualMinutesUntilClass minutes at ${classItem.startTime}",
                    type = NotificationType.REMINDER,
                    timestamp = System.currentTimeMillis()
                ),
                contentIntent = contentPendingIntent
            )
            return
        }


        val intent = Intent(context, ClassReminderReceiver::class.java).apply {
            putExtra("title", classItem.name)
            putExtra("message", "Starts in $minutesBefore minutes at ${classItem.startTime}")
            putExtra("location", classItem.location)
        }


        val pendingIntent = PendingIntent.getBroadcast(
            context,
            classItem.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val triggerMillis = triggerTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()


        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager


        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            else -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        }


        Log.d("ClassReminderScheduler", "Scheduled reminder for ${classItem.name} at $triggerTime")
    }


    fun cancelReminder(context: Context, classItem: ClassItem) {
        val intent = Intent(context, ClassReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            classItem.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }


    private fun getNextClassDateTime(classItem: ClassItem): LocalDateTime? {
        val now = LocalDateTime.now()
        val startTime = runCatching {
            LocalTime.parse(classItem.startTime, timeFormatter)
        }.getOrNull() ?: return null


        val meetingDays = classItem.meetingDays.mapNotNull { mapDayOfWeek(it) }
        if (meetingDays.isEmpty()) return null


        for (offset in 0..13) {
            val date = LocalDate.now().plusDays(offset.toLong())
            if (meetingDays.contains(date.dayOfWeek)) {
                val dateTime = LocalDateTime.of(date, startTime)
                if (dateTime.isAfter(now)) {
                    return dateTime
                }
            }
        }


        return null
    }


    private fun mapDayOfWeek(day: String): DayOfWeek? {
        return when (day.trim().lowercase()) {
            "mon", "monday" -> DayOfWeek.MONDAY
            "tue", "tues", "tuesday" -> DayOfWeek.TUESDAY
            "wed", "wednesday" -> DayOfWeek.WEDNESDAY
            "thu", "thurs", "thursday" -> DayOfWeek.THURSDAY
            "fri", "friday" -> DayOfWeek.FRIDAY
            "sat", "saturday" -> DayOfWeek.SATURDAY
            "sun", "sunday" -> DayOfWeek.SUNDAY
            else -> null
        }
    }
}
