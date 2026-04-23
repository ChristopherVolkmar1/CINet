package com.example.cinet.feature.calendar.schedule

import com.example.cinet.feature.calendar.event.EventItem

/** Builds the short reminder summary text shown in the schedule section. */
fun campusEventReminderScheduleText(event: EventItem): String {
    return if (event.allDay) {
        "Reminder: 9:00 AM on event day"
    } else {
        "Reminder: 30 minutes before"
    }
}
