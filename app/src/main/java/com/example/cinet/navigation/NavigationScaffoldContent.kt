package com.example.cinet.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
//import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.clubs.ClubItem
import com.example.cinet.feature.home.news.NewsArticle
import com.example.cinet.feature.map.CampusLocation

// Draws the scaffold, persistent top bar, bottom bar, map layer, and active page route.
@Composable
internal fun NavigationScaffoldContent(
    uiState: NavigationUiState,
    routeCallbacks: NavigationRouteCallbacks,
    onBottomScreenSelected: (Screen) -> Unit,
    onTopBarBack: () -> Unit,
    onTopBarFriendsClick: () -> Unit,
    onTopBarNewMessageClick: () -> Unit,
    onMapBack: () -> Unit,
    onMapFinishedLoading: () -> Unit,
    onRemoveExtraLocation: (CampusLocation) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onNewsBack: () -> Unit,
    onClubClick: (ClubItem) -> Unit,
    onClubsBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                state = uiState.topBarState,
                onBack = onTopBarBack,
                isHomeScreen = uiState.currentScreen == Screen.Home &&
                        !uiState.isShowingNews &&
                        !uiState.isShowingClubs,
                nickname = uiState.userProfile.nickname,
                onFriendsClick = onTopBarFriendsClick,
                onNewMessageClick = onTopBarNewMessageClick
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
            NavigationMapLayer(
                currentScreen = uiState.currentScreen,
                preSelectedLocation = uiState.preSelectedMapLocation,
                autoRouteToPreSelectedLocation = uiState.autoRouteToPreSelectedMapLocation,
                extraLocations = uiState.sharedLocations,
                onBack = onMapBack,
                onFinishedLoading = onMapFinishedLoading,
                onRemoveExtraLocation = onRemoveExtraLocation
            )

            if (uiState.currentScreen != Screen.Map) {
                when {
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
                        callbacks = routeCallbacks
                    )
                }
            }
        }
    }
}
