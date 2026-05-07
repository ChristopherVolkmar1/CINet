package com.example.cinet.feature.home

import android.content.Context
import android.util.Log
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.calendar.event.CampusEventReminderPreferences
import com.example.cinet.feature.calendar.event.EventItem
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Builds the combined Home screen upcoming-event list from manual items, user events, and campus events. */
fun buildHomeUpcomingEventItems(
    context: Context,
    manualItems: List<Pair<String, String>>,
    campusEvents: List<EventItem>,
    userEvents: List<EventItem> = emptyList(),
    classItems: List<ClassItem> = emptyList(),
    currentTimeMillis: Long = System.currentTimeMillis()
): List<HomeUpcomingEventItem> {
    val campusUpcoming = buildCampusUpcomingEventItems(context, campusEvents, currentTimeMillis)
    val userUpcoming = buildUserUpcomingEventItems(userEvents, currentTimeMillis)
    val classUpcoming = buildClassUpcomingEventItems(classItems, currentTimeMillis)
    val manualUpcoming = buildManualUpcomingEventItems(manualItems)
    
    // Combine all and sort by time.
    val sortedEvents = (campusUpcoming + userUpcoming + classUpcoming).sortedBy { it.sortKey }
    
    val result = sortedEvents + manualUpcoming
    Log.d("EventsBuilder", "Built ${result.size} items. Campus: ${campusUpcoming.size}, User: ${userUpcoming.size}, Class: ${classUpcoming.size}, Manual: ${manualUpcoming.size}")
    return result
}

private fun buildManualUpcomingEventItems(
    manualItems: List<Pair<String, String>>
): List<HomeUpcomingEventItem> {
    return manualItems.map { (title, description) ->
        HomeUpcomingEventItem(
            title = title,
            description = description,
            isCampusEvent = false,
            sortKey = Long.MAX_VALUE
        )
    }
}

private fun buildUserUpcomingEventItems(
    userEvents: List<EventItem>,
    currentTimeMillis: Long
): List<HomeUpcomingEventItem> {
    return userEvents
        .mapNotNull { event ->
            val startMillis = event.startEpochMillis ?: parseEventToMillis(event.date, event.time)
            // For user events, we consider them "past" only if they've completely ended.
            // Since we don't have a reliable end time for all, we use a 1-hour default duration for filtering.
            val endMillis = event.endEpochMillis ?: (startMillis?.plus(3600000))
            
            if (startMillis == null || endMillis == null || endMillis < currentTimeMillis) return@mapNotNull null
            
            HomeUpcomingEventItem(
                title = event.name,
                description = buildEventHomeDescription(event, startMillis),
                isCampusEvent = false,
                sortKey = startMillis
            )
        }
}

private fun buildCampusUpcomingEventItems(
    context: Context,
    campusEvents: List<EventItem>,
    currentTimeMillis: Long
): List<HomeUpcomingEventItem> {
    return campusEvents
        .filter { shouldShowCampusEventOnHome(context, it, currentTimeMillis) }
        .mapNotNull { event ->
            val startMillis = event.startEpochMillis ?: parseEventToMillis(event.date, event.time) ?: return@mapNotNull null
            toHomeUpcomingCampusEventItem(event, startMillis)
        }
}

private fun buildClassUpcomingEventItems(
    classItems: List<ClassItem>,
    currentTimeMillis: Long
): List<HomeUpcomingEventItem> {
    return classItems
        .filter { it.remindersEnabled }
        .mapNotNull { classItem ->
            val nextMillis = findNextClassOccurrence(classItem, currentTimeMillis) ?: return@mapNotNull null
            HomeUpcomingEventItem(
                title = classItem.name,
                description = "Next Class • ${classItem.startTime} | ${classItem.location}",
                isCampusEvent = false,
                sortKey = nextMillis
            )
        }
}

private fun shouldShowCampusEventOnHome(
    context: Context,
    event: EventItem,
    currentTimeMillis: Long
): Boolean {
    if (!event.isCampusEvent) return false
    if (!CampusEventReminderPreferences.isReminderEnabled(context, event.id)) return false

    val startMillis = event.startEpochMillis ?: parseEventToMillis(event.date, event.time)
    val eventEndMillis = event.endEpochMillis ?: startMillis ?: return false
    
    return eventEndMillis >= currentTimeMillis
}

private fun toHomeUpcomingCampusEventItem(event: EventItem, startMillis: Long): HomeUpcomingEventItem {
    return HomeUpcomingEventItem(
        title = event.name,
        description = buildEventHomeDescription(event, startMillis),
        isCampusEvent = true,
        sortKey = startMillis
    )
}

private fun buildEventHomeDescription(event: EventItem, startMillis: Long): String {
    val dateText = formatEventDate(startMillis)
    val timeText = event.time.ifBlank { "All day" }
    val locationText = event.location.ifBlank { "TBA" }
    return "$dateText • $timeText | $locationText"
}

private fun formatEventDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}

private fun parseEventToMillis(dateStr: String, timeStr: String): Long? {
    if (dateStr.isBlank()) return null
    return try {
        val date = LocalDate.parse(dateStr)
        val time = parseLocalTime(timeStr) ?: LocalTime.MIDNIGHT
        date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
}

private fun parseLocalTime(timeStr: String): LocalTime? {
    if (timeStr.equals("All day", ignoreCase = true) || timeStr.isBlank()) return LocalTime.MIDNIGHT
    return try {
        val rawTime = timeStr.substringBefore("-").trim().uppercase()
        val cleanedTime = if (!rawTime.contains(" ") && (rawTime.endsWith("AM") || rawTime.endsWith("PM"))) {
            rawTime.replace("AM", " AM").replace("PM", " PM")
        } else {
            rawTime
        }
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        LocalTime.parse(cleanedTime, timeFormatter)
    } catch (e: Exception) {
        null
    }
}

private fun findNextClassOccurrence(classItem: ClassItem, currentTimeMillis: Long): Long? {
    val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault())
    val startTime = parseLocalTime(classItem.startTime) ?: return null
    
    val dayMap = mapOf(
        "Mon" to java.time.DayOfWeek.MONDAY,
        "Tue" to java.time.DayOfWeek.TUESDAY,
        "Wed" to java.time.DayOfWeek.WEDNESDAY,
        "Thu" to java.time.DayOfWeek.THURSDAY,
        "Fri" to java.time.DayOfWeek.FRIDAY,
        "Sat" to java.time.DayOfWeek.SATURDAY,
        "Sun" to java.time.DayOfWeek.SUNDAY
    )
    
    val meetingDays = classItem.meetingDays.mapNotNull { dayMap[it] }
    if (meetingDays.isEmpty()) return null
    
    for (i in 0..7) {
        val candidateDate = now.toLocalDate().plusDays(i.toLong())
        if (meetingDays.contains(candidateDate.dayOfWeek)) {
            val candidateDateTime = candidateDate.atTime(startTime)
            val candidateMillis = candidateDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            // Allow showing the class if it started within the last 45 minutes (it's "current")
            if (candidateMillis >= currentTimeMillis - 2700000) {
                return candidateMillis
            }
        }
    }
    return null
}