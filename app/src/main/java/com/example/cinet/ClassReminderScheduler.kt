package com.example.cinet

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.cinet.ClassItem

object ClassReminderScheduler {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    fun scheduleNextReminder(
        context: Context,
        classItem: ClassItem,
        minutesBefore: Long = 10L
    ) {
        val nextClassDateTime = getNextClassDateTime(classItem) ?: return
        val triggerTime = nextClassDateTime.minusMinutes(minutesBefore)

        val minutesUntilClass = java.time.Duration.between(
            LocalDateTime.now(),
            nextClassDateTime
        ).toMinutes()

        if (triggerTime.isBefore(LocalDateTime.now())) {
            NotificationHelper.createChannel(context)
            NotificationHelper.showNotification(
                context = context,
                notification = AppNotification(
                    title = classItem.name,
                    message = "Starts in $minutesUntilClass minutes at ${classItem.startTime}",
                    type = NotificationType.REMINDER,
                    timestamp = System.currentTimeMillis()
                )
            )
            return
        }

        val triggerMillis = triggerTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, ClassReminderReceiver::class.java).apply {
            putExtra("title", classItem.name)
            putExtra("message", "Starts in $minutesBefore minutes at ${classItem.startTime}")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            classItem.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

    fun cancelReminder(
        context: Context,
        classItem: ClassItem
    ) {
        val intent = Intent(context, ClassReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            classItem.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun getNextClassDateTime(classItem: ClassItem): LocalDateTime? {
        val startTime = try {
            LocalTime.parse(classItem.startTime, timeFormatter)
        } catch (e: Exception) {
            return null
        }

        val validDays = classItem.meetingDays.mapNotNull { toDayOfWeek(it) }.toSet()
        if (validDays.isEmpty()) return null

        val now = LocalDateTime.now()

        for (offset in 0..7) {
            val date = LocalDate.now().plusDays(offset.toLong())
            if (date.dayOfWeek in validDays) {
                val candidate = LocalDateTime.of(date, startTime)
                if (candidate.isAfter(now)) {
                    return candidate
                }
            }
        }

        return null
    }

    private fun toDayOfWeek(day: String): DayOfWeek? {
        return when (day) {
            "Mon" -> DayOfWeek.MONDAY
            "Tue" -> DayOfWeek.TUESDAY
            "Wed" -> DayOfWeek.WEDNESDAY
            "Thu" -> DayOfWeek.THURSDAY
            "Fri" -> DayOfWeek.FRIDAY
            "Sat" -> DayOfWeek.SATURDAY
            "Sun" -> DayOfWeek.SUNDAY
            else -> null
        }
    }
}