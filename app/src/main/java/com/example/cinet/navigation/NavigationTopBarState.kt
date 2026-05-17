package com.example.cinet.navigation

// Holds the display values needed by the persistent app top bar.
internal data class NavigationTopBarState(
    val title: String,
    val showBackButton: Boolean,
    val showSocialActions: Boolean,
    val pendingRequestCount: Int,
)
