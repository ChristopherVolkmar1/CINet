package com.example.cinet.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.cinet.data.model.*
import com.example.cinet.data.remote.SocialRepository
import java.util.Calendar
import java.util.Locale
import com.example.cinet.feature.auth.*
import com.example.cinet.feature.map.*
import com.example.cinet.feature.home.*
import com.example.cinet.feature.social.*
import com.example.cinet.feature.profile.*
import com.example.cinet.feature.calendar.calendarFiles.*
import com.example.cinet.feature.settings.*
import kotlinx.coroutines.launch

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
    onSaveProfile: (String, String, String) -> Unit,
    initialMapLocationName: String? = null,
    onInitialMapLocationConsumed: () -> Unit = {}
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
            onSignOut = onSignOut,
            initialMapLocationName = initialMapLocationName,
            onInitialMapLocationConsumed = onInitialMapLocationConsumed
        )
    }
}

@Composable
private fun MainScaffold(
    userProfile: UserProfile,
    onSignOut: () -> Unit,
    initialMapLocationName: String? = null,
    onInitialMapLocationConsumed: () -> Unit = {},
    viewModel: CampusRegistry = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val calendarViewModel: CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val campusRegistry by viewModel.campusRegistry.collectAsState()
    var preSelectedMapLocation by remember { mutableStateOf<CampusLocation?>(null) }

    val socialScope = rememberCoroutineScope()
    val socialRepository = remember { SocialRepository() }

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
        calendarViewModel.scheduleItems
            .filter { it.date == dateStr }
            .forEach { list.add(it.assignmentName to "Due: ${it.dueTime} (${it.className})") }
        list
    }

    var currentScreen by remember { mutableStateOf(Screen.Home) }

    LaunchedEffect(initialMapLocationName, campusRegistry) {
        val locationName = initialMapLocationName ?: return@LaunchedEffect

        val location = campusRegistry["academic"]?.find { it.name == locationName }
        if (location != null) {
            preSelectedMapLocation = location
            currentScreen = Screen.Map
            onInitialMapLocationConsumed()
        }
    }

    var showAddClassOnCalendar by remember { mutableStateOf(false) }
    var showProfileEdit by remember { mutableStateOf(false) }

    // Social sub-navigation stack
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showNewConversation by remember { mutableStateOf(false) }
    var showSocialScreen by remember { mutableStateOf(false) } // friends / search / requests

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("cinet_prefs", android.content.Context.MODE_PRIVATE) }
    val campusReminderPrefs = remember {
        context.getSharedPreferences("campus_event_reminders", android.content.Context.MODE_PRIVATE)
    }
    var campusReminderRefreshKey by remember { mutableStateOf(0) }

    DisposableEffect(campusReminderPrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key?.startsWith("reminder_") == true) {
                campusReminderRefreshKey++
            }
        }

        campusReminderPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            campusReminderPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

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

    var upcomingEventsItems by remember { mutableStateOf(loadItems("event_items")) }

    val homeUpcomingEventsItems = remember(
        upcomingEventsItems,
        calendarViewModel.campusEventItems,
        campusReminderRefreshKey
    ) {
        buildHomeUpcomingEventItems(
            context = context,
            manualItems = upcomingEventsItems,
            campusEvents = calendarViewModel.campusEventItems
        )
    }

    val socialBackStackActive = currentScreen == Screen.Social &&
            (activeConversation != null || selectedProfile != null ||
                    showNewConversation || showSocialScreen)

    BackHandler(enabled = currentScreen != Screen.Home || socialBackStackActive || showProfileEdit) {
        when {
            activeConversation != null -> activeConversation = null
            showNewConversation -> showNewConversation = false
            selectedProfile != null -> selectedProfile = null
            showSocialScreen -> showSocialScreen = false
            else -> currentScreen = Screen.Home
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            if (screen != Screen.Social) {
                                // Clear social sub-navigation when leaving the tab
                                activeConversation = null
                                selectedProfile = null
                                showNewConversation = false
                                showSocialScreen = false
                            }
                            if (screen != Screen.Calendar) {
                                showAddClassOnCalendar = false
                                if (screen != Screen.Settings)
                                    showProfileEdit = false
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    nickname = userProfile.nickname,
                    scheduleItems = calendarScheduleItems,
                    manualUpcomingEventsItems = upcomingEventsItems,
                    displayUpcomingEventsItems = homeUpcomingEventsItems,
                    onUpdateSchedule = { },
                    onUpdateEvents = {
                        upcomingEventsItems = it
                        saveItems("event_items", it)
                    },
                    onMapClick = { currentScreen = Screen.Map },
                    onSettingsClick = { currentScreen = Screen.Settings },
                    onCalendarClick = { currentScreen = Screen.Calendar },
                    onAddClassClick = {
                        showAddClassOnCalendar = true
                        currentScreen = Screen.Calendar
                    },
                    onNavigateToLocation = { locationName ->
                        val location = campusRegistry["academic"]?.find { it.name == locationName }
                        preSelectedMapLocation = location
                        currentScreen = Screen.Map
                    }
                )

                Screen.Social -> when {
                    // Deepest layer first
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
                    showNewConversation -> NewConversationScreen(
                        currentUserProfile = userProfile,
                        onBack = { showNewConversation = false },
                        onOpenConversation = {
                            showNewConversation = false
                            activeConversation = it
                        }
                    )
                    showSocialScreen -> SocialScreen(
                        onOpenProfile = { selectedProfile = it },
                        onOpenConversation = { friend ->
                            socialScope.launch {
                                socialRepository.getOrCreateConversation(
                                    participantIds = listOf(userProfile.uid, friend.uid),
                                    participantNicknames = mapOf(
                                        userProfile.uid to userProfile.nickname,
                                        friend.uid to friend.nickname
                                    )
                                ).onSuccess {
                                    showSocialScreen = false
                                    activeConversation = it
                                }
                            }
                        }
                    )
                    else -> ConversationsListScreen(
                        onOpenConversation = { activeConversation = it },
                        onNewConversation = { showNewConversation = true },
                        onOpenFriends = { showSocialScreen = true }
                    )
                }

                Screen.Map -> CampusMapScreen(
                    onBack = { currentScreen = Screen.Home },
                    preSelectedLocation = preSelectedMapLocation,
                    onFinishedLoading = { preSelectedMapLocation = null }
                )

                Screen.Calendar -> CalendarScreen(
                    onBack = {
                        currentScreen = Screen.Home
                        showAddClassOnCalendar = false
                    },
                    initialShowClassDialog = showAddClassOnCalendar
                )

                Screen.Settings -> if (showProfileEdit) {
                    ProfileEditScreen(
                        onBack = { showProfileEdit = false }
                    )
                } else {
                    SettingScreen(
                        onBack = { currentScreen = Screen.Home },
                        onSignOut = onSignOut,
                        onEditProfile = { showProfileEdit = true },
                        userProfile = userProfile
                    )
                }
            }
        }
    }
}