package com.example.cinet.feature.calendar.event

/** Identifies where a calendar event came from. */
enum class EventSource {
    USER,
    CAMPUS
}

/** Stores one event row shown in the calendar event section. */
data class EventItem(
    val id: String = "",
    val date: String = "",
    val name: String = "",
    val time: String = "",
    val location: String = "",
    val description: String = "",
    val source: EventSource = EventSource.USER,
    val startEpochMillis: Long? = null,
    val endEpochMillis: Long? = null,
    val allDay: Boolean = false
) {
    /** Returns true when this event came from the live campus ICS feed. */
    val isCampusEvent: Boolean
        get() = source == EventSource.CAMPUS
}
