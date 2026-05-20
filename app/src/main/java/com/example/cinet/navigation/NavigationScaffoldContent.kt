package com.example.cinet.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.example.cinet.data.remote.canvas.CanvasConversation
import com.example.cinet.feature.clubs.ClubItem
import com.example.cinet.feature.home.news.NewsArticle
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.MapTopBarControls
import com.example.cinet.feature.map.MapTopBarState
import com.example.cinet.feature.calendar.calendarFiles.CalendarTopBarActions
import com.example.cinet.feature.calendar.calendarFiles.CalendarTopBarState

// Draws the scaffold, persistent top bar, bottom bar, map layer, and active page route.
@Composable
internal fun NavigationScaffoldContent(
    uiState: NavigationUiState,
    routeCallbacks: NavigationRouteCallbacks,
    onBottomScreenSelected: (Screen) -> Unit,
    onTopBarBack: () -> Unit,
    onTopBarFriendsClick: () -> Unit,
    onTopBarCanvasMessagesClick: () -> Unit,
    onTopBarNewMessageClick: () -> Unit,
    onMapBack: () -> Unit,
    onMapFinishedLoading: () -> Unit,
    onRemoveExtraLocation: (CampusLocation) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onNewsBack: () -> Unit,
    onClubClick: (ClubItem) -> Unit,
    onClubsBack: () -> Unit,
    onCanvasConversationClick: (CanvasConversation) -> Unit,
) {
    var mapTopBarState by remember { mutableStateOf<MapTopBarState?>(null) }
    var calendarTopBarState by remember { mutableStateOf<CalendarTopBarState?>(null) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                state = uiState.topBarState,
                onBack = onTopBarBack,
                isHomeScreen = uiState.currentScreen == Screen.Home &&
                        !uiState.isShowingNews &&
                        !uiState.isShowingClubs &&
                        !uiState.isShowingCanvasInbox,
                nickname = uiState.userProfile.nickname,
                mapTopBarContent = if (uiState.currentScreen == Screen.Map && mapTopBarState != null) {
                    {
                        MapTopBarControls(state = mapTopBarState!!)
                    }
                } else {
                    null
                },
                calendarTopBarContent = if (uiState.currentScreen == Screen.Calendar && calendarTopBarState != null) {
                    {
                        CalendarTopBarActions(state = calendarTopBarState!!)
                    }
                } else {
                    null
                },
                onFriendsClick = onTopBarFriendsClick,
                onNewMessageClick = onTopBarNewMessageClick,
                onCanvasMessagesClick = onTopBarCanvasMessagesClick
            )
        },
        bottomBar = {
            NavigationBottomBar(
                isVisible = !uiState.isShowingNews && !uiState.hideBottomBarForConversationTyping,
                currentScreen = uiState.currentScreen,
                onScreenSelected = onBottomScreenSelected
            )
        }
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val hideBottomPadding = uiState.isShowingNews || uiState.hideBottomBarForConversationTyping
        val contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(layoutDirection),
            bottom = if (hideBottomPadding) 0.dp else innerPadding.calculateBottomPadding()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (uiState.currentScreen == Screen.Map) {
                NavigationMapLayer(
                    preSelectedLocation = uiState.preSelectedMapLocation,
                    autoRouteToPreSelectedLocation = uiState.autoRouteToPreSelectedMapLocation,
                    extraLocations = uiState.sharedLocations,
                    onBack = onMapBack,
                    onFinishedLoading = onMapFinishedLoading,
                    onRemoveExtraLocation = onRemoveExtraLocation,
                    onTopBarStateChanged = { mapTopBarState = it }
                )
            }

            if (uiState.currentScreen != Screen.Map) {
                when {
                    // Canvas messaging overlay takes precedence over news/clubs
                    // because the user explicitly opened it from a home tile;
                    // they expect to see it regardless of what was open before.
                    uiState.isShowingCanvasInbox -> NavigationCanvasMessagesRoute(
                        selectedConversation = uiState.selectedCanvasConversation,
                        onOpenConversation = onCanvasConversationClick
                    )

                    uiState.isShowingNews -> NavigationNewsRoute(
                        selectedNewsArticle = uiState.selectedNewsArticle,
                        onArticleClick = onArticleClick,
                        onBack = onNewsBack
                    )

                    uiState.isShowingClubs -> NavigationClubsRoute(
                        selectedClub = uiState.selectedClub,
                        onClubClick = onClubClick,
                        onBack = onClubsBack
                    )

                    else -> NavigationScreenRoute(
                        uiState = uiState,
                        callbacks = routeCallbacks,
                        onCalendarTopBarStateChanged = { calendarTopBarState = it }
                    )
                }
            }
        }
    }
}
