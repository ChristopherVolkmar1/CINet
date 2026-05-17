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
    val isSubPageActive = activeConversation != null ||
            selectedProfile != null ||
            showNewConversation ||
            showSocialScreen ||
            showProfileEdit ||
            showCanvasScreen ||
            showAddClassOnCalendar ||
            showCIView ||
            selectedNewsArticle != null ||
            showClubs ||
            selectedClub != null

    val isAtRoot = backStackSize <= 1 && !isSubPageActive

    BackHandler(enabled = !isAtRoot) {
        when {
            // Priority 1: Dialogs and deepest terminal screens
            showCanvasScreen -> onHideCanvas()
            showProfileEdit -> onHideProfileEdit()
            showAddClassOnCalendar -> onHideAddClass()
            
            // Priority 2: Main content pages (Chat, Article, Club details)
            // We clear the active conversation first to support "Profile -> Chat -> Back to Profile"
            activeConversation != null -> onClearActiveConversation()
            
            selectedNewsArticle != null -> {
                if (selectedNewsArticle.title == "Study Rooms") {
                    onClearSelectedNewsArticle()
                    onHideCIView()
                } else {
                    onClearSelectedNewsArticle()
                    onShowCIView()
                }
            }
            selectedClub != null -> onClearSelectedClub()

            // Priority 3: Detail views
            selectedProfile != null -> onClearSelectedProfile()

            // Priority 4: Functional sub-lists (New Chat, Friends List, News List, Clubs List)
            showNewConversation -> onHideNewConversation()
            showSocialScreen -> onHideSocialScreen()
            showCIView -> onHideCIView()
            showClubs -> onHideClubs()

            // Priority 5: Global backstack (Tab history)
            else -> onGoBack()
        }
    }
}
