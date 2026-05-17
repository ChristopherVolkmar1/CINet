package com.example.cinet.feature.home

import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
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
import com.example.cinet.feature.map.BusScheduleSheet
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
    onClubsClick: () -> Unit = {},
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
    var showBusScheduleSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "Home screen launched")
        weatherInfo = WeatherHelper.fetchCampusWeather(context)
        newsArticles = newsRepository.fetchLatestNews()
    }

    HomeScreenContent(
        nickname = nickname,
        weatherInfo = weatherInfo,
        newsArticles = newsArticles,
        nextUpcomingEvent = displayUpcomingEventsItems.firstOrNull(),
        onMapClick = onMapClick,
        onCalendarClick = onCalendarClick,
        onSocialClick = onSocialClick,
        onClubsClick = onClubsClick,
        onNotificationClick = { showBusScheduleSheet = true },
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

    if (showBusScheduleSheet) {
        BusScheduleSheet(
            onDismiss = { showBusScheduleSheet = false }
        )
    }
}

/** Arranges the home screen from top to bottom. */
@Composable
private fun HomeScreenContent(
    nickname: String,
    weatherInfo: WeatherInfo,
    newsArticles: List<NewsArticle>,
    nextUpcomingEvent: HomeUpcomingEventItem?,
    onMapClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSocialClick: () -> Unit,
    onClubsClick: () -> Unit,
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
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 18.dp, bottom = 24.dp)
    ) {

        Spacer(modifier = Modifier.height(32.dp))

        WelcomeHeader(nickname = nickname)

        Spacer(modifier = Modifier.height(18.dp))

        HomeWeatherBanner(
            temp = weatherInfo.temp,
            condition = weatherInfo.condition
        )

        Spacer(modifier = Modifier.height(24.dp))

        LatestNewsCarousel(
            articles = newsArticles,
            onSeeAllClick = onSeeAllNewsClick,
            onArticleClick = onArticleClick
        )

        if (nextUpcomingEvent != null) {
            Spacer(modifier = Modifier.height(14.dp))

            NextUpcomingEventBanner(nextUpcomingEvent = nextUpcomingEvent)
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

        Spacer(modifier = Modifier.height(22.dp))

        QuickActionGrid(
            onMapClick = onMapClick,
            onCalendarClick = onCalendarClick,
            onSocialClick = onSocialClick,
            onClubsClick = onClubsClick,
            onStudyRoomsClick = onStudyRoomsClick,
            onProfileClick = onProfileClick,
            onNotificationClick = onNotificationClick
        )
    }
}

/** Displays the greeting and keeps it locked to one line by scaling long names down. */
@Composable
private fun WelcomeHeader(nickname: String) {
    val displayName = nickname.ifBlank { "there" }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Welcome back, $displayName 👋",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = greetingFontSizeFor(displayName),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Chooses a smaller greeting size when the name is longer so the greeting stays on one line. */
private fun greetingFontSizeFor(displayName: String) = when {
    displayName.length > 22 -> 20.sp
    displayName.length > 16 -> 22.sp
    displayName.length > 10 -> 24.sp
    else -> 26.sp
}

/** Displays the weather summary without the "Campus Weather" label. */
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
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = weatherIcon,
                contentDescription = displayCondition,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$temp • $displayCondition",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Camarillo, CA",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Displays the next upcoming calendar event using the calendar pill style with the home banner shape. */
@Composable
private fun NextUpcomingEventBanner(nextUpcomingEvent: HomeUpcomingEventItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.42f))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nextUpcomingEvent.title,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = nextUpcomingEventTimeText(nextUpcomingEvent),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.90f),
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = nextUpcomingEventLocationText(nextUpcomingEvent),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/** Pulls the date/time part from the Home event description when possible. */
private fun nextUpcomingEventTimeText(event: HomeUpcomingEventItem): String {
    return event.description
        .substringBefore("|")
        .trim()
        .ifBlank { if (event.isCampusEvent) "Campus Event" else "Upcoming Event" }
}

/** Pulls the location part from the Home event description when possible. */
private fun nextUpcomingEventLocationText(event: HomeUpcomingEventItem): String {
    val locationText = event.description
        .substringAfter("|", missingDelimiterValue = "")
        .trim()

    return locationText.ifBlank {
        if (event.isCampusEvent) "Campus Event • Reminder on" else "Calendar Event"
    }
}

/** Displays the latest news heading, carousel cards, and page indicators. */
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

        Spacer(modifier = Modifier.height(14.dp))

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

            Spacer(modifier = Modifier.height(14.dp))

            NewsCarouselIndicators(
                count = articles.size.coerceAtMost(5),
                activeIndex = activeIndex.coerceAtMost(4)
            )
        }
    }
}

/** Displays the latest news title row. */
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
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "See all",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.clickable(onClick = onSeeAllClick)
        )
    }
}

/** Displays a temporary news loading card. */
@Composable
private fun LoadingNewsCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
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

/** Displays one horizontally scrollable news article card. */
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
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Displays the small carousel page indicator dots. */
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

/** Displays the quick action shortcuts as circular buttons that match the calendar page. */
@Composable
private fun QuickActionGrid(
    onMapClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onSocialClick: () -> Unit,
    onClubsClick: () -> Unit,
    onStudyRoomsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            QuickActionCircleButton(
                icon = Icons.Default.Groups,
                title = "Clubs",
                onClick = onClubsClick
            )

            QuickActionCircleButton(
                icon = Icons.Default.MenuBook,
                title = "Study Rooms",
                onClick = onStudyRoomsClick
            )

            QuickActionCircleButton(
                icon = Icons.Default.Person,
                title = "Profile",
                onClick = onProfileClick
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            QuickActionCircleButton(
                icon = Icons.Default.DirectionsBus,
                title = "Bus Schedule",
                onClick = onNotificationClick
            )
        }
    }
}

/** Shows one circular quick action button with its label below it. */
@Composable
private fun QuickActionCircleButton(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(96.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(75.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 5.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Removes extra campus text from the weather condition. */
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

/** Chooses the best icon for the current weather condition. */
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
            nickname = "username",
            scheduleItems = emptyList(),
            manualUpcomingEventsItems = emptyList(),
            displayUpcomingEventsItems = emptyList(),
            onUpdateSchedule = {},
            onUpdateEvents = {},
            onMapClick = {},
            onSettingsClick = {},
            onCalendarClick = {},
            onAddClassClick = {},
            onCIViewClick = {},
            onClubsClick = {},
            onArticleClick = {},
            onSocialClick = {},
            onNotificationClick = {},
            onProfileClick = {},
            onNavigateToLocation = {}
        )
    }
}
