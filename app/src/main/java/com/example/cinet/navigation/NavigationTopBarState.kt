package com.example.cinet.navigation

import com.example.cinet.feature.calendar.calendarFiles.CalendarTopBarState
import com.example.cinet.feature.map.MapTopBarState
import com.example.cinet.feature.social.ConversationTopBarState
import com.example.cinet.feature.social.NewConversationTopBarState

// Holds the display values needed by the persistent app top bar.
internal data class NavigationTopBarState(
    val title: String,
    val showBackButton: Boolean,
    val showSocialActions: Boolean,
    val showCanvasMessagesAction: Boolean = false,
    val showSettingsActions: Boolean = false,
    val pendingRequestCount: Int,
    val isHomeScreen: Boolean = false,
    val nickname: String = "",
    val newConversationTopBarState: NewConversationTopBarState? = null,
    val conversationTopBarState: ConversationTopBarState? = null,
    val mapTopBarState: MapTopBarState? = null,
    val calendarTopBarState: CalendarTopBarState? = null,
)