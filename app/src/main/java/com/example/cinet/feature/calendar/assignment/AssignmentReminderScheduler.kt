package com.example.cinet.feature.calendar.assignment

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.cinet.core.notifications.AppNotification
import com.example.cinet.AppSettings
import com.example.cinet.NotificationHelper
import com.example.cinet.core.notifications.NotificationType
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

object AssignmentReminderScheduler {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    fun scheduleReminder(
        context: Context,
        date: String,
        classId: String,
        className: String,
        assignmentName: String,
        dueTime: String,
        minutesBefore: Long = AppSettings.assignmentReminderMinutesBefore
    ) {
        val dueDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            return
        }

        val parsedDueTime = try {
            LocalTime.parse(dueTime, timeFormatter)
        } catch (e: Exception) {
            return
        }

        val dueDateTime = LocalDateTime.of(dueDate, parsedDueTime)
        val triggerTime = dueDateTime.minusMinutes(minutesBefore)
        val now = LocalDateTime.now()

        if (dueDateTime.isBefore(now)) return

        if (triggerTime.isBefore(now)) {
            val minutesUntilDue = Duration.between(now, dueDateTime).toMinutes().coerceAtLeast(0)

            NotificationHelper.createChannel(context)
            NotificationHelper.showNotification(
                context = context,
                notification = AppNotification(
                    title = assignmentName,
                    message = "Due in $minutesUntilDue minutes at $dueTime for $className",
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

        val intent = Intent(context, AssignmentReminderReceiver::class.java).apply {
            putExtra("title", assignmentName)
            putExtra("message", "Due at $dueTime for $className")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(date, classId, assignmentName, dueTime),
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
        date: String,
        classId: String,
        assignmentName: String,
        dueTime: String
    ) {
        val intent = Intent(context, AssignmentReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(date, classId, assignmentName, dueTime),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun requestCodeFor(
        date: String,
        classId: String,
        assignmentName: String,
        dueTime: String
    ): Int {
        return "${date}_${classId}_${assignmentName}_${dueTime}".hashCode()
    }
}