package com.example.cinet.data.model

import com.example.cinet.ui.theme.AppThemeColor
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val nickname: String = "",
    val nicknameLower: String = "",  // lowercase copy used for case-insensitive search
    val major: String = "",
    val minor: String = "",
    val pronouns: String = "",
    val year: String = "",           // Freshman / Sophomore / Junior / Senior / Graduate / Transfer
    val bio: String = "",            // short free-text blurb
    val interests: List<String> = emptyList(), // e.g. ["Gaming", "Hiking", "Music"]
    val photoUrl: String = "",
    val fcmToken: String? = null,    // used to send push notifications to this user's device
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val selectedTheme: AppThemeColor = AppThemeColor.Green,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val lastLoginAt: Date? = null,
)