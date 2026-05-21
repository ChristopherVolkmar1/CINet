package com.example.cinet.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.canvas.CanvasConversation
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
    showCanvasInbox: Boolean,
    selectedCanvasConversation: CanvasConversation?,
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
    onCloseCanvasConversation: () -> Unit,
    onHideCanvasInbox: () -> Unit,
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
                selectedProfile != null ||
                showCanvasInbox
    ) {
        // Order matters — deepest sub-state first, so each back press unwinds
        // one level. Canvas messaging slots in front of news/clubs because the
        // user explicitly opened the messaging surface from the home tile.
        when {
            selectedCanvasConversation != null -> onCloseCanvasConversation()
            showCanvasInbox -> onHideCanvasInbox()
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
            showAddClassOnCalendar -> onHideAddClass()
            else -> onGoBack()
        }
    }
}
