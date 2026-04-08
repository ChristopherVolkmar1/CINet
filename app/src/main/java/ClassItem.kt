package com.example.cinet

data class ClassItem(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val meetingDays: List<String>,
    val startTime: String,
    val endTime: String
)

// Creates a class (Like a actual class for class so the class is classified by the individual class where the class occurs and not the class of where classes on programs go but classes within the real world so it is the class.)