package com.example.cinet.navigation

import androidx.compose.runtime.Composable
import com.example.cinet.feature.home.HomeScreen
import com.example.cinet.feature.home.news.NewsArticle
import com.example.cinet.feature.home.HomeUpcomingEventItem

// Shows the home page and passes home navigation actions back to MainScaffold.
@Composable
internal fun NavigationHomeRoute(
    nickname: String,
    scheduleItems: List<Pair<String, String>>,
    manualUpcomingEventsItems: List<Pair<String, String>>,
    displayUpcomingEventsItems: List<HomeUpcomingEventItem>,
    onUpdateEvents: (List<Pair<String, String>>) -> Unit,
    onMapClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAddClassClick: () -> Unit,
    onClubsClick: () -> Unit,
    onCIViewClick: (NewsArticle?) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onSocialClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToLocation: (String) -> Unit,
) {
    HomeScreen(
        nickname = nickname,
        scheduleItems = scheduleItems,
        manualUpcomingEventsItems = manualUpcomingEventsItems,
        displayUpcomingEventsItems = displayUpcomingEventsItems,
        onUpdateSchedule = { },
        onUpdateEvents = onUpdateEvents,
        onMapClick = onMapClick,
        onSettingsClick = onSettingsClick,
        onCalendarClick = onCalendarClick,
        onAddClassClick = onAddClassClick,
        onClubsClick = onClubsClick,
        onCIViewClick = onCIViewClick,
        onArticleClick = onArticleClick,
        onSocialClick = onSocialClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onNavigateToLocation = onNavigateToLocation
    )
}
