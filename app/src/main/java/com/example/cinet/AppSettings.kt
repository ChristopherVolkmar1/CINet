package com.example.cinet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// settings stuff - Zack
/**
 * Global application settings that persist during the current session.
 */
object AppSettings {
    var isDarkMode by mutableStateOf(false)
    var isDarkMap by mutableStateOf(false)
    var notificationsEnabled by mutableStateOf(true)
}
