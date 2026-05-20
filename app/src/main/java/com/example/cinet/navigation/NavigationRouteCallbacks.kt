package com.example.cinet.navigation

import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.canvas.CanvasConversation
import com.example.cinet.feature.home.news.NewsArticle
import com.example.cinet.feature.social.ConversationTopBarState
import com.example.cinet.feature.social.NewConversationTopBarState
import com.example.cinet.ui.theme.AppThemeColor

// Groups page-specific callbacks so route functions stay readable.
internal data class NavigationRouteCallbacks(
    val onUpdateManualEvents: (List<Pair<String, String>>) -> Unit,
    val onGoToScreen: (Screen) -> Unit,
    val onShowAddClass: () -> Unit,
    val onShowClubs: () -> Unit,
    val onShowCIView: () -> Unit,
    val onSelectNewsArticle: (NewsArticle) -> Unit,
    val onOpenHomeProfile: () -> Unit,
    val onNavigateToLocation: (String) -> Unit,
    val onClearActiveConversation: () -> Unit,
    val onResetSocialStack: () -> Unit,
    val onNavigateToCoordinates: (Double, Double, String, String) -> Unit,
    val onClearSelectedProfile: () -> Unit,
    val onShowProfileEdit: () -> Unit,
    val onHideNewConversation: () -> Unit,
    val onOpenConversationFromNew: (Conversation) -> Unit,
    val onOpenProfile: (UserProfile) -> Unit,
    val onOpenConversationWithFriend: (UserProfile) -> Unit,
    val onOpenConversationFromList: (Conversation) -> Unit,
    val onShowNewConversation: () -> Unit,
    val onShowSocialScreen: () -> Unit,
    val onSeedTimestamps: (List<String>) -> Unit,
    val onCalendarBack: () -> Unit,
    val onHideCanvas: () -> Unit,
    val onCanvasSyncComplete: () -> Unit,
    val onHideProfileEdit: () -> Unit,
    val onProfileSaved: () -> Unit,
    val onSettingsConversationOpened: (Conversation) -> Unit,
    val onSettingsSelectedProfileBack: () -> Unit,
    val onSignOut: () -> Unit,
    val onSettingsChange: (Boolean, Boolean, AppThemeColor) -> Unit,
    val onViewProfile: () -> Unit,
    val onOpenCanvas: () -> Unit,
    val onNewConversationTopBarChange: (NewConversationTopBarState?) -> Unit,
    // Restored from the read-receipts / profile-from-home refactor — these
    // were inadvertently dropped from an earlier replacement of this file.
    val onReadReceiptsChange: (Boolean) -> Unit,
    val onConversationTopBarChange: (ConversationTopBarState?) -> Unit,
    val onOpenChatFromHome: (UserProfile) -> Unit,
    // Canvas messaging callbacks. Inbox is opened from the home page; the
    // conversation is selected from inside the inbox; back actions are
    // dispatched through NavigationBackHandler in the usual pattern.
    val onShowCanvasInbox: () -> Unit,
    val onOpenCanvasConversation: (CanvasConversation) -> Unit,
)
