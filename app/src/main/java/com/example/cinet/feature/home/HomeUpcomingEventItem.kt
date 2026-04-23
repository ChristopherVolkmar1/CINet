package com.example.cinet.feature.home

/** Stores one row shown in the Home screen's Upcoming Events section. */
data class HomeUpcomingEventItem(
    val title: String,
    val description: String,
    val isCampusEvent: Boolean
)
