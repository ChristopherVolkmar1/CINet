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
    backStackSize: Int,
    activeConversation: Conversation?,
    selectedProfile: UserProfile?,
    showNewConversation: Boolean,
    showSocialScreen: Boolean,
    showProfileEdit: Boolean,
    showCanvasScreen: Boolean,
    showAddClassOnCalendar: Boolean,
    showCIView: Boolean,
    selectedNewsArticle: NewsArticle?,
    showClubs: Boolean,
    selectedClub: ClubItem?,
    onHideCanvas: () -> Unit,
    onHideAddClass: () -> Unit,
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
    onGoBack: () -> Unit,
) {
    val socialBackStackActive = currentScreen == Screen.Social &&
            (activeConversation != null || selectedProfile != null ||
                    showNewConversation || showSocialScreen)

    BackHandler(
        enabled = backStackSize > 1 ||
                socialBackStackActive ||
                showProfileEdit ||
                showCanvasScreen ||
                showAddClassOnCalendar ||
                showCIView ||
                selectedNewsArticle != null ||
                showClubs ||
                selectedClub != null ||
                selectedProfile != null
    ) {
        when {
            showCanvasScreen -> onHideCanvas()
            showAddClassOnCalendar -> onHideAddClass()
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
            // selectedProfile is checked BEFORE activeConversation so that
            // pressing system-back while viewing a profile opened from inside
            // a conversation returns to the conversation, not the list.
            selectedProfile != null -> onClearSelectedProfile()
            activeConversation != null -> onClearActiveConversation()
            showNewConversation -> onHideNewConversation()
            showSocialScreen -> onHideSocialScreen()
            showProfileEdit -> onHideProfileEdit()
            else -> onGoBack()
        }
    }
}