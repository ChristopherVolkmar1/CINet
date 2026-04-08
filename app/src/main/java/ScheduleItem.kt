package com.example.cinet

data class ScheduleItem(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val classId: Long,
    val className: String,
    val assignmentName: String,
    val dueTime: String
)
// To schedule a item