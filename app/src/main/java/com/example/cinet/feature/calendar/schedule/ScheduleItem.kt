package com.example.cinet.feature.calendar.schedule

data class ScheduleItem(
    val id: String = "",
    val date: String,
    val classId: String,
    val className: String,
    val assignmentName: String,
    val dueTime: String
)