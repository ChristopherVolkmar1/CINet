package com.example.cinet.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val nickname: String = "",
    val major: String = "",
    val pronouns: String = "",
    val photoUrl: String = "",
    val fcmToken: String? = null, // used to send push notifications to this user's device
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val lastLoginAt: Date? = null,
)