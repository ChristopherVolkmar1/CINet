package com.example.cinet.feature.home

import android.content.Context
import com.example.cinet.feature.calendar.event.CampusEventReminderPreferences
import com.example.cinet.feature.calendar.event.EventItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Builds the combined Home screen upcoming-event list from manual items and reminder-enabled campus events. */
fun buildHomeUpcomingEventItems(
    context: Context,
    manualItems: List<Pair<String, String>>,
    campusEvents: List<EventItem>,
    currentTimeMillis: Long = System.currentTimeMillis()
): List<HomeUpcomingEventItem> {
    val campusItems = buildCampusUpcomingEventItems(context, campusEvents, currentTimeMillis)
    val manualUpcomingItems = buildManualUpcomingEventItems(manualItems)
    return campusItems + manualUpcomingItems
}

/** Converts the user's existing manually managed home-page items into display models. */
private fun buildManualUpcomingEventItems(
    manualItems: List<Pair<String, String>>
): List<HomeUpcomingEventItem> {
    return manualItems.map { (title, description) ->
        HomeUpcomingEventItem(
            title = title,
            description = description,
            isCampusEvent = false
        )
    }
}

/** Converts reminder-enabled future campus events into Home screen display models. */
private fun buildCampusUpcomingEventItems(
    context: Context,
    campusEvents: List<EventItem>,
    currentTimeMillis: Long
): List<HomeUpcomingEventItem> {
    return campusEvents
        .filter { shouldShowCampusEventOnHome(context, it, currentTimeMillis) }
        .sortedBy { it.startEpochMillis ?: Long.MAX_VALUE }
        .map(::toHomeUpcomingCampusEventItem)
}

/** Returns true when a campus event is future-or-current and has reminders enabled. */
private fun shouldShowCampusEventOnHome(
    context: Context,
    event: EventItem,
    currentTimeMillis: Long
): Boolean {
    if (!event.isCampusEvent) return false
    if (!CampusEventReminderPreferences.isReminderEnabled(context, event.id)) return false

    val eventEndMillis = event.endEpochMillis ?: event.startEpochMillis ?: return false
    return eventEndMillis >= currentTimeMillis
}

/** Maps one campus event into the card content shown on the Home screen. */
private fun toHomeUpcomingCampusEventItem(event: EventItem): HomeUpcomingEventItem {
    return HomeUpcomingEventItem(
        title = event.name,
        description = buildCampusEventHomeDescription(event),
        isCampusEvent = true
    )
}

/** Builds the secondary text for a campus event row on the Home screen. */
private fun buildCampusEventHomeDescription(event: EventItem): String {
    val dateText = event.startEpochMillis?.let(::formatEventDate).orEmpty()
    val timeText = event.time.ifBlank { "All day" }
    val locationText = event.location.ifBlank { "TBA" }
    return "$dateText • $timeText | $locationText"
}

/** Formats one campus event date into a readable Home screen label. */
private fun formatEventDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}
