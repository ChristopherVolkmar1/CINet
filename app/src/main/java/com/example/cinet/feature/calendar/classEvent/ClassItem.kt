package com.example.cinet.feature.calendar.classEvent

data class ClassItem(
    val id: String = "",
    val name: String,
    val meetingDays: List<String>,
    val startTime: String,
    val endTime: String,
    val location: String,
    val remindersEnabled: Boolean = true, // per-class reminder toggle
)