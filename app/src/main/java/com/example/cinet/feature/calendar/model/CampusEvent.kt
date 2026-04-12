package com.example.cinet.com.example.cinet.feature.calendar.model

import com.google.firebase.Timestamp

data class CampusEvent(
    val id: String = "",
    val title: String = "",
    val location: String = "",
    val description: String = "",
    val createdAt: Timestamp? = null,
)