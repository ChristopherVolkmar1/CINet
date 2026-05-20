package com.example.cinet.feature.calendar.calendarFiles

// Holds the click actions for the calendar buttons shown in the persistent top bar.
data class CalendarTopBarState(
    val onClassesClick: () -> Unit,
    val onStudyClick: () -> Unit,
    val onEventsClick: () -> Unit,
)