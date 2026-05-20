package com.example.cinet.navigation

import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.canvas.CanvasConversation
import com.example.cinet.feature.clubs.ClubItem
import com.example.cinet.feature.home.news.NewsArticle
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.home.HomeUpcomingEventItem

// Holds the current navigation values that the scaffold needs to render.
internal data class NavigationUiState(
    val topBarState: NavigationTopBarState,
    val currentScreen: Screen,
    val userProfile: UserProfile,
    val calendarScheduleItems: List<Pair<String, String>>,
    val manualUpcomingEventsItems: List<Pair<String, String>>,
    val displayUpcomingEventsItems: List<HomeUpcomingEventItem>,
    val activeConversation: Conversation?,
    val selectedProfile: UserProfile?,
    val showNewConversation: Boolean,
    val showSocialScreen: Boolean,
    val showAddClassOnCalendar: Boolean,
    val showCanvasScreen: Boolean,
    val showProfileEdit: Boolean,
    val openedConversationTimestamps: Map<String, Long>,
    val showCIView: Boolean,
    val selectedNewsArticle: NewsArticle?,
    val showClubs: Boolean,
    val selectedClub: ClubItem?,
    val preSelectedMapLocation: CampusLocation?,
    val autoRouteToPreSelectedMapLocation: Boolean,
    val sharedLocations: List<CampusLocation>,
    val isShowingNews: Boolean,
    val isShowingClubs: Boolean,
    val hideBottomBarForConversationTyping: Boolean,
    // Canvas messaging overlay state. `showCanvasInbox` controls whether
    // the overlay is open at all; `selectedCanvasConversation` distinguishes
    // the inbox list (null) from the thread view (non-null).
    val showCanvasInbox: Boolean,
    val selectedCanvasConversation: CanvasConversation?,
    val isShowingCanvasInbox: Boolean,
)
