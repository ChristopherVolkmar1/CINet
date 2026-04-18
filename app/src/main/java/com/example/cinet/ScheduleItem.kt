package com.example.cinet

data class ScheduleItem(
    val id: String = "",
    val date: String,
    val classId: String,
    val className: String,
    val assignmentName: String,
    val dueTime: String
)