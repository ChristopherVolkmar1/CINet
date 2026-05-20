package com.example.cinet.feature.social

// Holds the title and actions that ConversationScreen sends to the persistent top bar.
data class ConversationTopBarState(
    val title: String,
    val photoUrl: String,                       // DM: other user's photo; blank for groups
    val isGroup: Boolean,
    val onTitleClick: (() -> Unit)?,            // DM only — opens the other user's ProfileScreen
    val onSearchClick: () -> Unit,              // toggles in-conversation search bar
    val onInfoClick: (() -> Unit)?,             // group only — opens GroupInfoSheet
    val onRemoveFriendClick: (() -> Unit)?,     // DM only — triggers remove-friend confirmation
    val onPinnedClick: (() -> Unit)? = null,    // non-null when any messages are pinned
)