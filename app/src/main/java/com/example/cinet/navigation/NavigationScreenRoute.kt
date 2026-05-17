package com.example.cinet.navigation

import androidx.compose.runtime.Composable
import com.example.cinet.feature.calendar.calendarFiles.CalendarScreen

// Routes the selected main tab to the correct smaller navigation file.
@Composable
internal fun NavigationScreenRoute(
    uiState: NavigationUiState,
    callbacks: NavigationRouteCallbacks,
) {
    when (uiState.currentScreen) {
        Screen.Home -> NavigationHomeRoute(
            nickname = uiState.userProfile.nickname,
            scheduleItems = uiState.calendarScheduleItems,
            manualUpcomingEventsItems = uiState.manualUpcomingEventsItems,
            displayUpcomingEventsItems = uiState.displayUpcomingEventsItems,
            onUpdateEvents = callbacks.onUpdateManualEvents,
            onMapClick = { callbacks.onGoToScreen(Screen.Map) },
            onSettingsClick = { callbacks.onGoToScreen(Screen.Settings) },
            onCalendarClick = { callbacks.onGoToScreen(Screen.Calendar) },
            onAddClassClick = callbacks.onShowAddClass,
            onClubsClick = callbacks.onShowClubs,
            onCIViewClick = { article ->
                if (article != null) callbacks.onSelectNewsArticle(article) else callbacks.onShowCIView()
            },
            onArticleClick = callbacks.onSelectNewsArticle,
            onSocialClick = {
                callbacks.onGoToScreen(Screen.Social)
                callbacks.onResetSocialStack()
            },
            onNotificationClick = { callbacks.onGoToScreen(Screen.Settings) },
            onProfileClick = callbacks.onOpenHomeProfile,
            onNavigateToLocation = callbacks.onNavigateToLocation,
            onOpenChatFromHome = callbacks.onOpenChatFromHome
        )

        Screen.Social -> NavigationSocialRoute(
            activeConversation = uiState.activeConversation,
            selectedProfile = uiState.selectedProfile,
            showNewConversation = uiState.showNewConversation,
            showSocialScreen = uiState.showSocialScreen,
            userProfile = uiState.userProfile,
            openedConversationTimestamps = uiState.openedConversationTimestamps,
            onConversationBack = callbacks.onClearActiveConversation,
            onNavigateToLocation = callbacks.onNavigateToLocation,
            onNavigateToCoordinates = callbacks.onNavigateToCoordinates,
            onProfileBack = callbacks.onClearSelectedProfile,
            onEditProfile = callbacks.onShowProfileEdit,
            onNewConversationBack = callbacks.onHideNewConversation,
            onOpenConversationFromNew = callbacks.onOpenConversationFromNew,
            onOpenProfile = callbacks.onOpenProfile,
            onOpenConversationWithFriend = callbacks.onOpenConversationWithFriend,
            onOpenConversationFromList = callbacks.onOpenConversationFromList,
            onNewConversation = callbacks.onShowNewConversation,
            onOpenFriends = callbacks.onShowSocialScreen,
            onSeedTimestamps = callbacks.onSeedTimestamps,
        )

        Screen.Map -> Unit

        Screen.Calendar -> CalendarScreen(
            onBack = callbacks.onCalendarBack,
            initialShowClassDialog = uiState.showAddClassOnCalendar
        )

        Screen.Settings -> NavigationSettingsRoute(
            showCanvasScreen = uiState.showCanvasScreen,
            showProfileEdit = uiState.showProfileEdit,
            selectedProfile = uiState.selectedProfile,
            userProfile = uiState.userProfile,
            onCanvasBack = callbacks.onHideCanvas,
            onCanvasSyncComplete = callbacks.onCanvasSyncComplete,
            onProfileEditBack = callbacks.onHideProfileEdit,
            onProfileSaved = callbacks.onProfileSaved,
            onOpenConversation = callbacks.onSettingsConversationOpened,
            onSelectedProfileBack = callbacks.onSettingsSelectedProfileBack,
            onEditProfile = callbacks.onShowProfileEdit,
            onSettingsBack = { callbacks.onGoToScreen(Screen.Home) },
            onSignOut = callbacks.onSignOut,
            onSettingsChange = callbacks.onSettingsChange,
            onViewProfile = callbacks.onViewProfile,
            onOpenCanvas = callbacks.onOpenCanvas,
        )
    }
}
