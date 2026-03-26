package com.example.cinet.data.model

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val createdAt: Timestamp? = null,
)