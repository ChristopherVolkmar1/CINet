package com.example.cinet.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Social("Social", Icons.Default.People),

    Map("Map", Icons.Default.LocationOn),
    Calendar("Calendar", Icons.Default.CalendarMonth),
    Settings("Settings", Icons.Default.Settings)
}
