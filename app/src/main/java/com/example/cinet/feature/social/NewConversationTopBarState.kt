package com.example.cinet.feature.social

// Holds the title and actions that NewConversationScreen sends to the persistent top bar.
data class NewConversationTopBarState(
    val title: String,
    val actionLabel: String,
    val actionEnabled: Boolean,
    val isActionLoading: Boolean,
    val onBackClick: () -> Unit,
    val onActionClick: () -> Unit,
    val onPinnedClick: (() -> Unit)? = null,
)