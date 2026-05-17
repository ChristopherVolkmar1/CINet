package com.example.cinet.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.feature.clubs.ClubItem
import com.example.cinet.feature.home.news.NewsArticle

// Keeps all custom back-button behavior in one place.
@Composable
internal fun NavigationBackHandler(
    currentScreen: Screen,
    activeConversation: Conversation?,
    selectedProfile: UserProfile?,
    showNewConversation: Boolean,
    showSocialScreen: Boolean,
    showProfileEdit: Boolean,
    showCanvasScreen: Boolean,
    showCIView: Boolean,
    selectedNewsArticle: NewsArticle?,
    showClubs: Boolean,
    selectedClub: ClubItem?,
    onHideCanvas: () -> Unit,
    onClearSelectedNewsArticle: () -> Unit,
    onShowCIView: () -> Unit,
    onHideCIView: () -> Unit,
    onClearSelectedClub: () -> Unit,
    onHideClubs: () -> Unit,
    onClearActiveConversation: () -> Unit,
    onHideNewConversation: () -> Unit,
    onClearSelectedProfile: () -> Unit,
    onHideSocialScreen: () -> Unit,
    onHideProfileEdit: () -> Unit,
    onGoHome: () -> Unit,
) {
    val socialBackStackActive = currentScreen == Screen.Social &&
            (activeConversation != null || selectedProfile != null ||
                    showNewConversation || showSocialScreen)

    BackHandler(
        enabled = currentScreen != Screen.Home ||
                socialBackStackActive ||
                showProfileEdit ||
                showCanvasScreen ||
                showCIView ||
                selectedNewsArticle != null ||
                showClubs ||
                selectedClub != null ||
                selectedProfile != null
    ) {
        when {
            showCanvasScreen -> onHideCanvas()
            selectedNewsArticle != null -> {
                if (selectedNewsArticle.title == "Study Rooms") {
                    onClearSelectedNewsArticle()
                    onHideCIView()
                } else {
                    onClearSelectedNewsArticle()
                    onShowCIView()
                }
            }
            showCIView -> onHideCIView()
            selectedClub != null -> onClearSelectedClub()
            showClubs -> onHideClubs()
            activeConversation != null -> onClearActiveConversation()
            showNewConversation -> onHideNewConversation()
            selectedProfile != null -> onClearSelectedProfile()
            showSocialScreen -> onHideSocialScreen()
            showProfileEdit -> onHideProfileEdit()
            else -> onGoHome()
        }
    }
}
