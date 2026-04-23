package com.example.cinet.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.CIViewScreen
import com.example.cinet.NewsArticle
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import com.example.cinet.feature.auth.AuthState
import com.example.cinet.feature.auth.ErrorScreen
import com.example.cinet.feature.auth.LoadingScreen
import com.example.cinet.feature.auth.LoginScreen
import com.example.cinet.feature.auth.ProfileSetupScreen
import com.example.cinet.feature.auth.viewmodel.AuthViewModel
import com.example.cinet.feature.calendar.calendarFiles.CalendarScreen
import com.example.cinet.feature.calendar.calendarFiles.CalendarViewModel
import com.example.cinet.feature.home.HomeScreen
import com.example.cinet.feature.home.buildHomeUpcomingEventItems
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.CampusMapScreen
import com.example.cinet.feature.profile.ProfileScreen
import com.example.cinet.feature.settings.AppSettings
import com.example.cinet.feature.settings.SettingScreen
import com.example.cinet.feature.social.ConversationScreen
import com.example.cinet.feature.social.SocialScreen
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class Screen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Social("Social", Icons.Default.People),
    Map("Map", Icons.Default.LocationOn),
    Calendar("Calendar", Icons.Default.CalendarMonth),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun NavigationHandler(
    authState: AuthState,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
    onSaveProfile: (String, String, String) -> Unit
) {
    when (authState) {
        is AuthState.Loading -> LoadingScreen()
        is AuthState.Unauthenticated -> LoginScreen()
        is AuthState.ProfileSetup -> ProfileSetupScreen(
            onSaveProfile = onSaveProfile
        )
        is AuthState.Error -> ErrorScreen(
            message = authState.message,
            onRetry = onRetry
        )
        is AuthState.Authenticated -> MainScaffold(
            userProfile = authState.userProfile,
            onSignOut = onSignOut
        )
    }
}

@Composable
private fun MainScaffold(
    userProfile: UserProfile,
    onSignOut: () -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val calendarViewModel: CalendarViewModel = viewModel()
    val campusRegistryViewModel: CampusRegistry = viewModel()
    val campusRegistry by campusRegistryViewModel.campusRegistry.collectAsState()
    var preSelectedMapLocation by remember { mutableStateOf<CampusLocation?>(null) }

    val repository = remember { SocialRepository() }
    val scope = rememberCoroutineScope()

    // Sync global AppSettings object with the user profile from Firebase
    LaunchedEffect(userProfile) {
        AppSettings.isDarkMode = userProfile.isDarkMode
        AppSettings.notificationsEnabled = userProfile.notificationsEnabled
    }

    val calendarScheduleItems = remember(calendarViewModel.classItems, calendarViewModel.scheduleItems) {
        val cal = Calendar.getInstance()
        val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US) ?: ""
        val dateStr = "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val list = mutableListOf<Pair<String, String>>()
        calendarViewModel.classItems
            .filter { it.meetingDays.contains(dayName) }
            .forEach { list.add(it.name to "${it.startTime} - ${it.endTime} | ${it.location}") }
        // Add assignments/tasks for today
        calendarViewModel.scheduleItems
            .filter { it.date == dateStr }
            .forEach { list.add(it.assignmentName to "Due: ${it.dueTime} (${it.className})") }
        list
    }

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var showAddClassOnCalendar by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }

    // tracks news list and single article
    var showCIView by remember { mutableStateOf(false) }
    var selectedNewsArticle by remember { mutableStateOf<NewsArticle?>(null) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("cinet_prefs", android.content.Context.MODE_PRIVATE) }

    fun loadItems(key: String): List<Pair<String, String>> {
        val saved = sharedPrefs.getString(key, null) ?: return emptyList()
        return saved.split("||").filter { it.contains("|") }.map {
            val parts = it.split("|")
            parts[0] to parts[1]
        }
    }

    fun saveItems(key: String, items: List<Pair<String, String>>) {
        val stringified = items.joinToString("||") { "${it.first}|${it.second}" }
        sharedPrefs.edit().putString(key, stringified).apply()
    }

    var manualUpcomingEventsItems by remember { mutableStateOf(loadItems("event_items")) }

    // Use the HomeUpcomingEventsBuilder function to combine manual and campus events
    val displayUpcomingEventsItems = remember(manualUpcomingEventsItems, calendarViewModel.campusEventItems) {
        buildHomeUpcomingEventItems(context, manualUpcomingEventsItems, calendarViewModel.campusEventItems)
    }

    // true if news is open
    val isShowingNews = showCIView || selectedNewsArticle != null

    // handles android back button
    BackHandler(enabled = currentScreen != Screen.Home || selectedProfile != null || activeConversation != null || isShowingNews) {
        when {
            // close article, go back to list
            selectedNewsArticle != null -> {
                selectedNewsArticle = null
                showCIView = true
            }
            // close list, go home
            showCIView -> showCIView = false
            activeConversation != null -> activeConversation = null
            selectedProfile != null -> selectedProfile = null
            else -> currentScreen = Screen.Home
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // hide bar when reading news
            if (!isShowingNews) {
                NavigationBar {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                currentScreen = screen
                                // clear news when switching tabs
                                showCIView = false
                                selectedNewsArticle = null
                                if (screen != Screen.Social) {
                                    selectedProfile = null
                                    activeConversation = null
                                }
                                if (screen != Screen.Calendar) {
                                    showAddClassOnCalendar = false
                                }
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // full screen for news
                .padding(if (isShowingNews) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            if (isShowingNews) {
                CIViewScreen(
                    selectedArticleUrl = selectedNewsArticle?.url,
                    onArticleClick = { selectedNewsArticle = it },
                    onBack = { 
                        // back button on screen logic
                        if (selectedNewsArticle != null) {
                            selectedNewsArticle = null
                            showCIView = true
                        } else {
                            showCIView = false
                        }
                    }
                )
            } else {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        nickname = userProfile.nickname,
                        scheduleItems = calendarScheduleItems,
                        manualUpcomingEventsItems = manualUpcomingEventsItems,
                        displayUpcomingEventsItems = displayUpcomingEventsItems,
                        onUpdateSchedule = { },
                        onUpdateEvents = {
                            manualUpcomingEventsItems = it
                            saveItems("event_items", it)
                        },
                        onMapClick = { currentScreen = Screen.Map },
                        onSettingsClick = { currentScreen = Screen.Settings },
                        onCalendarClick = { currentScreen = Screen.Calendar },
                        onAddClassClick = {
                            showAddClassOnCalendar = true
                            currentScreen = Screen.Calendar
                        },
                        onCIViewClick = { article ->
                            // show list or article from home
                            if (article != null) {
                                selectedNewsArticle = article
                            } else {
                                showCIView = true
                            }
                        },
                        onArticleClick = { article ->
                            selectedNewsArticle = article
                        },
                        onNavigateToLocation = { locationName ->
                            val location = campusRegistry["academic"]?.find { it.name == locationName }
                            preSelectedMapLocation = location
                            currentScreen = Screen.Map
                        }
                    )
                    Screen.Social -> when {
                        activeConversation != null -> ConversationScreen(
                            conversation = activeConversation!!,
                            onBack = { activeConversation = null }
                        )
                        selectedProfile != null -> ProfileScreen(
                            user = selectedProfile!!,
                            currentUserProfile = userProfile,
                            onOpenConversation = { activeConversation = it },
                            onBack = { selectedProfile = null }
                        )
                        else -> SocialScreen(
                            onOpenProfile = { selectedProfile = it },
                            onOpenConversation = { friend ->
                                scope.launch {
                                    repository.getOrCreateConversation(
                                        participantIds = listOf(userProfile.uid, friend.uid),
                                        participantNicknames = mapOf(
                                            userProfile.uid to userProfile.nickname,
                                            friend.uid to friend.nickname
                                        )
                                    ).onSuccess { activeConversation = it }
                                }
                            }
                        )
                    }
                    Screen.Map -> CampusMapScreen(
                        onBack = { currentScreen = Screen.Home },
                        preSelectedLocation = preSelectedMapLocation,
                        onFinishedLoading = {
                            preSelectedMapLocation = null
                        }
                    )
                    Screen.Calendar -> CalendarScreen(
                        onBack = {
                            currentScreen = Screen.Home
                            showAddClassOnCalendar = false
                        },
                        initialShowClassDialog = showAddClassOnCalendar
                    )
                    Screen.Settings -> SettingScreen(
                        onBack = { currentScreen = Screen.Home },
                        onSignOut = onSignOut,
                        isDarkMode = userProfile.isDarkMode,
                        notificationsEnabled = userProfile.notificationsEnabled,
                        onSettingsChange = { dark, notify ->
                            authViewModel.updateSettings(dark, notify)
                        }
                    )
                }
            }
        }
    }
}
