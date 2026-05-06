package com.example.cinet.feature.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.home.news.NewsArticle
import com.example.cinet.feature.home.news.NewsRepository
import com.example.cinet.ui.theme.CINetTheme
import java.util.Calendar

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    nickname: String,
    scheduleItems: List<Pair<String, String>>,
    manualUpcomingEventsItems: List<Pair<String, String>>,
    displayUpcomingEventsItems: List<HomeUpcomingEventItem>,
    onUpdateSchedule: (List<Pair<String, String>>) -> Unit,
    onUpdateEvents: (List<Pair<String, String>>) -> Unit,
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onAddClassClick: () -> Unit = {},
    onCIViewClick: (NewsArticle?) -> Unit = {},
    onArticleClick: (NewsArticle) -> Unit = {},
    onSocialClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: CampusRegistry = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToLocation: (String) -> Unit
) {
    val context = LocalContext.current
    val newsRepository = remember { NewsRepository() }

    var weatherInfo by remember { mutableStateOf(WeatherInfo("...", "Loading...")) }
    var newsArticles by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }

    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "Home screen launched")
        weatherInfo = WeatherHelper.fetchCampusWeather(context)
        newsArticles = newsRepository.fetchLatestNews()
    }

    HomeScreenContent(
        nickname = nickname,
        weatherInfo = weatherInfo,
        newsArticles = newsArticles,
        onMapClick = onMapClick,
        onCalendarClick = onCalendarClick,
        onSocialClick = onSocialClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onSeeAllNewsClick = { onCIViewClick(null) },
        onArticleClick = { article -> onCIViewClick(article) },
        onStudyRoomsClick = {
            onArticleClick(
                NewsArticle(
                    title = "Study Rooms",
                    date = "",
                    previewText = "Reserve a CSUCI library study room.",
                    url = "https://csuci.libcal.com/allspaces"
                )
            )
        },
        modifier = modifier
    )
}

/**
 * Arranges the full home screen from top to bottom.
 */
@Composable
private fun HomeScreenContent(
    nickname: String,
    weatherInfo: WeatherInfo,
    newsArticles: List<NewsArticle>,
    onMapClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSocialClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSeeAllNewsClick: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onStudyRoomsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(top = 0.dp, bottom = 4.dp)
    ) {
        HomeTopBar(
            onProfileClick = onProfileClick,
            onSettingsClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        HomeMainCard {
            WelcomeHeader(nickname = nickname)

            Spacer(modifier = Modifier.height(14.dp))

            HomeWeatherBanner(
                temp = weatherInfo.temp,
                condition = weatherInfo.condition
            )

            Spacer(modifier = Modifier.height(18.dp))

            LatestNewsCarousel(
                articles = newsArticles,
                onSeeAllClick = onSeeAllNewsClick,
                onArticleClick = onArticleClick
            )

            Spacer(modifier = Modifier.height(28.dp))

            QuickActionGrid(
                onMapClick = onMapClick,
                onCalendarClick = onCalendarClick,
                onSocialClick = onSocialClick,
                onStudyRoomsClick = onStudyRoomsClick,
                onProfileClick = onProfileClick,
                onNotificationClick = onNotificationClick
            )
        }
    }
}

/**
 * Displays the top CINET title and action buttons.
 */
@Composable
private fun HomeTopBar(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CINET",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            letterSpacing = 0.3.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        HeaderIconButton(
            icon = Icons.Default.Person,
            contentDescription = "Open profile",
            onClick = onProfileClick
        )

        Spacer(modifier = Modifier.width(12.dp))

        HeaderIconButton(
            icon = Icons.Default.MoreVert,
            contentDescription = "Open settings",
            onClick = onSettingsClick
        )
    }
}

/**
 * Displays one circular icon button in the top header.
 */
@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Holds all main home page content inside one large rounded card.
 */
@Composable
private fun HomeMainCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .fillMaxWidth()
            .height(600.dp),
        shape = RoundedCornerShape(38.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
        ){
            content()
        }
    }
}

/**
 * Displays the greeting at the top of the main card.
 */
@Composable
private fun WelcomeHeader(nickname: String) {
    val displayName = nickname.ifBlank { "there" }

    Text(
        text = "Welcome back, $displayName 👋",
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Displays the weather summary without the "Campus Weather" label.
 */
@Composable
private fun HomeWeatherBanner(
    temp: String,
    condition: String
) {
    val displayCondition = remember(condition) { cleanedWeatherCondition(condition) }
    val weatherIcon = remember(displayCondition) { weatherIconFor(displayCondition) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = weatherIcon,
                contentDescription = displayCondition,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$temp • $displayCondition",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Camarillo, CA",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Displays the latest news heading, carousel cards, and page indicators.
 */
@Composable
private fun LatestNewsCarousel(
    articles: List<NewsArticle>,
    onSeeAllClick: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit
) {
    val listState = rememberLazyListState()
    val activeIndex by remember(articles.size) {
        derivedStateOf {
            if (articles.isEmpty()) {
                0
            } else {
                listState.firstVisibleItemIndex.coerceIn(0, articles.lastIndex)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        LatestNewsHeader(onSeeAllClick = onSeeAllClick)

        Spacer(modifier = Modifier.height(12.dp))

        if (articles.isEmpty()) {
            LoadingNewsCard()
        } else {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                itemsIndexed(articles) { _, article ->
                    HomeNewsCard(
                        article = article,
                        onClick = { onArticleClick(article) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NewsCarouselIndicators(
                count = articles.size.coerceAtMost(5),
                activeIndex = activeIndex.coerceAtMost(4)
            )
        }
    }
}

/**
 * Displays the latest news title row.
 */
@Composable
private fun LatestNewsHeader(
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CI View - Latest News",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "See all",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onSeeAllClick)
        )
    }
}

/**
 * Displays a temporary news loading card.
 */
@Composable
private fun LoadingNewsCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Loading latest campus news...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Displays one simplified purple news card.
 */
@Composable
private fun HomeNewsCard(
    article: NewsArticle,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(165.dp)
            .height(165.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = article.title,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Displays the small carousel page indicator dots.
 */
@Composable
private fun NewsCarouselIndicators(
    count: Int,
    activeIndex: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .width(if (index == activeIndex) 36.dp else 24.dp)
                    .height(7.dp)
                    .background(
                        color = if (index == activeIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        },
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

/**
 * Displays a thin divider between the news and quick actions.
 */
@Composable
private fun HomeDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
    )
}

/**
 * Displays the circular quick action buttons.
 */
@Composable
private fun QuickActionGrid(
    onMapClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSocialClick: () -> Unit,
    onStudyRoomsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionCircleButton(
                icon = Icons.Default.Map,
                contentDescription = "Open campus map",
                onClick = onMapClick
            )

            QuickActionCircleButton(
                icon = Icons.Default.CalendarMonth,
                contentDescription = "Open calendar",
                onClick = onCalendarClick
            )

            QuickActionCircleButton(
                icon = Icons.Default.Groups,
                contentDescription = "Open social",
                onClick = onSocialClick
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionCircleButton(
                icon = Icons.Default.MenuBook,
                contentDescription = "Open study rooms",
                onClick = onStudyRoomsClick
            )

            QuickActionCircleButton(
                icon = Icons.Default.Person,
                contentDescription = "Open profile",
                onClick = onProfileClick
            )

            QuickActionCircleButton(
                icon = Icons.Default.Notifications,
                contentDescription = "Open notifications",
                onClick = onNotificationClick
            )
        }
    }
}

/**
 * Displays one circular quick action button.
 */
@Composable
private fun QuickActionCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(75.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 5.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

/**
 * Removes extra campus text from the weather condition.
 */
private fun cleanedWeatherCondition(condition: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isNight = hour < 6 || hour >= 19
    val campusText = condition
        .replace(" - Camarillo, CA", "", ignoreCase = true)
        .trim()

    return if (isNight && campusText.contains("Sunny", ignoreCase = true)) {
        "Clear Sky"
    } else {
        campusText.ifBlank { "Loading..." }
    }
}

/**
 * Chooses the best icon for the current weather condition.
 */
private fun weatherIconFor(condition: String): ImageVector {
    return when {
        condition.contains("Clear Sky", ignoreCase = true) -> Icons.Default.NightsStay
        condition.contains("Sunny", ignoreCase = true) -> Icons.Default.WbSunny
        condition.contains("Clear", ignoreCase = true) -> Icons.Default.WbSunny
        condition.contains("Partly Cloudy", ignoreCase = true) -> Icons.Default.WbCloudy
        condition.contains("Cloudy", ignoreCase = true) -> Icons.Default.Cloud
        condition.contains("Cloud", ignoreCase = true) -> Icons.Default.Cloud
        condition.contains("Rain", ignoreCase = true) -> Icons.Default.Umbrella
        condition.contains("Shower", ignoreCase = true) -> Icons.Default.Umbrella
        condition.contains("Thunder", ignoreCase = true) -> Icons.Default.Thunderstorm
        else -> Icons.Default.Cloud
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CINetTheme {
        HomeScreen(
            nickname = "Maddi",
            scheduleItems = emptyList(),
            manualUpcomingEventsItems = emptyList(),
            displayUpcomingEventsItems = emptyList(),
            onUpdateSchedule = {},
            onUpdateEvents = {},
            onNavigateToLocation = { _ -> }
        )
    }
}