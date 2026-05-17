package com.example.cinet.navigation

import androidx.compose.runtime.Composable
import com.example.cinet.feature.clubs.ClubItem
import com.example.cinet.feature.clubs.ClubsScreen
import com.example.cinet.feature.home.news.CIViewScreen
import com.example.cinet.feature.home.news.NewsArticle

// Shows the CI View/news overlay.
@Composable
internal fun NavigationNewsRoute(
    selectedNewsArticle: NewsArticle?,
    onArticleClick: (NewsArticle) -> Unit,
    onBack: () -> Unit,
) {
    CIViewScreen(
        selectedArticleUrl = selectedNewsArticle?.url,
        selectedArticleTitle = selectedNewsArticle?.title,
        onArticleClick = onArticleClick,
        onBack = onBack,
        showTopBar = false
    )
}

// Shows the clubs overlay.
@Composable
internal fun NavigationClubsRoute(
    selectedClub: ClubItem?,
    onClubClick: (ClubItem) -> Unit,
    onBack: () -> Unit,
) {
    ClubsScreen(
        selectedClubUrl = selectedClub?.url,
        selectedClubTitle = selectedClub?.title,
        onClubClick = onClubClick,
        onBack = onBack,
        showTopBar = false
    )
}
