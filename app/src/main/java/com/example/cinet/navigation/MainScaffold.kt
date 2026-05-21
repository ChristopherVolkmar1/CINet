package com.example.cinet.navigation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import com.example.cinet.feature.calendar.calendarFiles.CalendarViewModel
import com.example.cinet.feature.home.buildHomeUpcomingEventItems
import com.example.cinet.feature.home.news.NewsArticle
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.settings.AppSettings
import com.example.cinet.feature.auth.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import com.example.cinet.feature.home.HomeUpcomingEventItem
import com.example.cinet.feature.social.NewConversationTopBarState
import com.example.cinet.feature.social.ConversationTopBarState
import com.example.cinet.data.remote.canvas.CanvasConversation
import com.example.cinet.data.remote.canvas.CanvasDisplaySettings
import com.example.cinet.data.remote.canvas.CanvasMessagingSettings
import com.example.cinet.data.remote.canvas.CanvasTokenStore

@Composable
internal fun MainScaffold(
    userProfile: UserProfile,
    onSignOut: () -> Unit,
    initialConversationId: String? = null,
    onConversationOpened: () -> Unit = {},
    initialMapLocationName: String? = null,
    onMapLocationOpened: () -> Unit = {},
) {
    val authViewModel: AuthViewModel = viewModel()
    val calendarViewModel: CalendarViewModel = viewModel()
    val campusRegistryViewModel: CampusRegistry = viewModel()
    val campusRegistry by campusRegistryViewModel.campusRegistry.collectAsState()
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("cinet_prefs", Context.MODE_PRIVATE)
    }
    val canvasTokenStore = remember(context) { CanvasTokenStore(context) }
    val socialScope = rememberCoroutineScope()
    val socialRepository = remember { SocialRepository() }
    var pendingRequestCount by remember { mutableStateOf(0) }

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var backStack by remember { mutableStateOf(listOf(Screen.Home)) }

    var showAddClassOnCalendar by remember { mutableStateOf(false) }
    var showCanvasScreen by remember { mutableStateOf(false) }
    var showCanvasInbox by remember { mutableStateOf(false) }
    var selectedCanvasConversation by remember { mutableStateOf<CanvasConversation?>(null) }
    var showProfileEdit by remember { mutableStateOf(false) }
    var profileOpenedFromHome by remember { mutableStateOf(false) }

    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showNewConversation by remember { mutableStateOf(false) }
    var showSocialScreen by remember { mutableStateOf(false) }
    var newConversationTopBarState by remember { mutableStateOf<NewConversationTopBarState?>(null) }
    var conversationTopBarState by remember { mutableStateOf<ConversationTopBarState?>(null) }
    // Restored: top-bar state pushed up from the Map and Calendar screens.
    var mapTopBarState by remember { mutableStateOf<com.example.cinet.feature.map.MapTopBarState?>(null) }
    var calendarTopBarState by remember { mutableStateOf<com.example.cinet.feature.calendar.calendarFiles.CalendarTopBarState?>(null) }

    var showCIView by remember { mutableStateOf(false) }
    var selectedNewsArticle by remember { mutableStateOf<NewsArticle?>(null) }
    var showClubs by remember { mutableStateOf(false) }
    var selectedClub by remember { mutableStateOf<com.example.cinet.feature.clubs.ClubItem?>(null) }

    var preSelectedMapLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var autoRouteToPreSelectedMapLocation by remember { mutableStateOf(false) }
    var sharedLocations by remember { mutableStateOf<List<CampusLocation>>(emptyList()) }

    var openedConversationTimestamps by remember {
        mutableStateOf(
            sharedPrefs.all
                .filter { it.key.startsWith("opened_conv_") }
                .mapKeys { it.key.removePrefix("opened_conv_") }
                .mapValues { (it.value as? Long) ?: 0L }
        )
    }

    var manualUpcomingEventsItems by remember {
        mutableStateOf(sharedPrefs.loadPairItems("event_items"))
    }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(userProfile) {
        AppSettings.isDarkMode = userProfile.isDarkMode
        AppSettings.notificationsEnabled = userProfile.notificationsEnabled
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    fun navigateToScreen(screen: Screen) {
        if (currentScreen != screen) {
            backStack = backStack + screen
            currentScreen = screen
        }
    }

    fun popBackStack() {
        if (backStack.size > 1) {
            val newStack = backStack.dropLast(1)
            backStack = newStack
            currentScreen = newStack.last()
        }
    }

    LaunchedEffect(currentScreen, activeConversation, selectedProfile, showNewConversation, showSocialScreen) {
        val isMessagesList = currentScreen == Screen.Social &&
                activeConversation == null &&
                selectedProfile == null &&
                !showNewConversation &&
                !showSocialScreen

        if (isMessagesList) {
            socialRepository.getPendingRequests().onSuccess { requests ->
                pendingRequestCount = requests.size
            }
        }
    }

    fun openConversation(conversation: Conversation) {
        val now = System.currentTimeMillis()
        openedConversationTimestamps = openedConversationTimestamps + (conversation.id to now)
        sharedPrefs.edit().putLong("opened_conv_${conversation.id}", now).apply()
        conversationTopBarState = null   // will be re-pushed by ConversationScreen on open
        activeConversation = conversation
    }

    fun seedConversationTimestamps(ids: List<String>) {
        val now = System.currentTimeMillis()
        val seeded = ids.associateWith { now }
        openedConversationTimestamps = openedConversationTimestamps + seeded

        val editor = sharedPrefs.edit()
        ids.forEach { id -> editor.putLong("opened_conv_$id", now) }
        editor.apply()
    }

    fun navigateToLocation(locationName: String, shouldAutoRoute: Boolean = false) {
        preSelectedMapLocation = campusRegistry.values.flatten()
            .find { it.name.equals(locationName, ignoreCase = true) }
        autoRouteToPreSelectedMapLocation = shouldAutoRoute
        currentScreen = Screen.Map
    }

    fun clearOverlays() {
        showCIView = false
        selectedNewsArticle = null
        showClubs = false
        selectedClub = null
        showCanvasInbox = false
        selectedCanvasConversation = null
    }

    fun resolveTopBarTitle(): String {
        return when {
            selectedCanvasConversation != null -> selectedCanvasConversation?.subject?.takeIf { it.isNotBlank() } ?: "Canvas"
            showCanvasInbox -> "Canvas Inbox"
            selectedNewsArticle != null -> selectedNewsArticle?.title ?: "CI View"
            showCIView -> "CI View"
            selectedClub != null -> selectedClub?.title ?: "Campus Clubs"
            showClubs -> "Campus Clubs"

            currentScreen == Screen.Home -> "Home"

            currentScreen == Screen.Social -> when {
                conversationTopBarState != null -> conversationTopBarState?.title ?: ""
                showNewConversation -> newConversationTopBarState?.title ?: "New Message"
                selectedProfile != null -> if (selectedProfile?.uid == userProfile.uid) "Profile" else selectedProfile?.nickname ?: "Profile"
                showSocialScreen -> "Friends"
                else -> "Messages"
            }

            currentScreen == Screen.Settings -> when {
                showCanvasScreen -> "Canvas Sync"
                showProfileEdit -> "Edit Profile"
                selectedProfile != null -> if (selectedProfile?.uid == userProfile.uid) "Profile" else selectedProfile?.nickname ?: "Profile"
                else -> "Settings"
            }

            else -> currentScreen.label
        }
    }

    fun shouldShowTopBarBack(): Boolean {
        return when {
            showCanvasInbox -> true
            selectedNewsArticle != null || showCIView -> true
            selectedClub != null || showClubs -> true
            currentScreen == Screen.Social && activeConversation != null -> true
            currentScreen == Screen.Social && selectedProfile != null -> true
            currentScreen == Screen.Social && showSocialScreen -> true
            currentScreen == Screen.Settings && showCanvasScreen -> true
            currentScreen == Screen.Settings && showProfileEdit -> true
            currentScreen == Screen.Settings && selectedProfile != null -> true
            currentScreen == Screen.Social && showNewConversation -> true
            else -> false
        }
    }

    fun handleTopBarBack() {
        when {
            selectedCanvasConversation != null -> selectedCanvasConversation = null
            showCanvasInbox -> showCanvasInbox = false
            selectedNewsArticle?.title == "Study Rooms" -> {
                selectedNewsArticle = null
                showCIView = false
            }
            selectedNewsArticle != null -> {
                selectedNewsArticle = null
                showCIView = true
            }
            showCIView -> showCIView = false
            selectedClub != null -> selectedClub = null
            showClubs -> showClubs = false
            currentScreen == Screen.Social && selectedProfile != null -> selectedProfile = null
            currentScreen == Screen.Social && showSocialScreen -> {
                showSocialScreen = false
            }
            currentScreen == Screen.Social && activeConversation != null -> {
                activeConversation = null
                conversationTopBarState = null
            }
            currentScreen == Screen.Social && showNewConversation -> {
                newConversationTopBarState?.onBackClick?.invoke() ?: run {
                    showNewConversation = false
                    newConversationTopBarState = null
                }
            }
            currentScreen == Screen.Settings && showCanvasScreen -> showCanvasScreen = false
            currentScreen == Screen.Settings && showProfileEdit -> showProfileEdit = false
            currentScreen == Screen.Settings && selectedProfile != null -> {
                selectedProfile = null
                if (profileOpenedFromHome) {
                    profileOpenedFromHome = false
                    currentScreen = Screen.Home
                }
            }
        }
    }

    fun handleBottomScreenSelected(screen: Screen) {
        navigateToScreen(screen)
        clearOverlays()
        profileOpenedFromHome = false
        newConversationTopBarState = null

        if (screen == Screen.Social) {
            activeConversation = null
            selectedProfile = null
            showNewConversation = false
            showSocialScreen = false
            conversationTopBarState = null
        }

        if (screen == Screen.Settings) {
            selectedProfile = null
            showProfileEdit = false
        }

        if (screen != Screen.Calendar) {
            showAddClassOnCalendar = false
            if (screen != Screen.Settings) showProfileEdit = false
        }

        if (screen != Screen.Map) {
            preSelectedMapLocation = null
            autoRouteToPreSelectedMapLocation = false
        }
    }

    val handleClearProfile = {
        selectedProfile = null
        // Only pop the back stack when there is no active conversation.
        // If a profile was opened from inside a conversation, clearing it
        // must return to the conversation — not navigate away from the screen.
        if (profileOpenedFromHome && activeConversation == null) {
            profileOpenedFromHome = false
            popBackStack()
        } else {
            profileOpenedFromHome = false
        }
    }

    LaunchedEffect(initialConversationId) {
        val id = initialConversationId ?: return@LaunchedEffect
        try {
            val snap = FirebaseFirestore.getInstance()
                .collection("conversations")
                .document(id)
                .get()
                .await()
            val conversation = snap.toObject(Conversation::class.java)

            if (conversation != null) {
                navigateToScreen(Screen.Social)
                openConversation(conversation)
                onConversationOpened()
            }
        } catch (e: Exception) {
            Log.e("NavigationHandler", "Failed to open conversation from notification: ${e.message}")
        }
    }

    LaunchedEffect(initialMapLocationName, campusRegistry) {
        val locationName = initialMapLocationName ?: return@LaunchedEffect
        if (campusRegistry.isEmpty()) return@LaunchedEffect

        val location = campusRegistry.values.flatten()
            .find { it.name.equals(locationName, ignoreCase = true) }

        if (location != null) {
            preSelectedMapLocation = location
            autoRouteToPreSelectedMapLocation = true
            navigateToScreen(Screen.Map)
            onMapLocationOpened()
        } else {
            Log.e("NavigationHandler", "Failed to find map location from notification: $locationName")
        }
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
        calendarViewModel.scheduleItems
            .filter { it.date == dateStr }
            .forEach { list.add(it.assignmentName to "Due: ${it.dueTime} (${it.className})") }
        list
    }

    val showCanvasItemsInCalendar = CanvasDisplaySettings.showCanvasInCalendar

    val displayUpcomingEventsItems: List<HomeUpcomingEventItem> = remember(
        manualUpcomingEventsItems,
        calendarViewModel.campusEventItems,
        calendarViewModel.userEventItems,
        calendarViewModel.classItems,
        calendarViewModel.scheduleItems,
        showCanvasItemsInCalendar,
        currentTimeMillis
    ) {
        buildHomeUpcomingEventItems(
            context = context,
            manualItems = manualUpcomingEventsItems,
            campusEvents = calendarViewModel.campusEventItems,
            userEvents = calendarViewModel.userEventItems,
            classItems = calendarViewModel.classItems,
            scheduleItems = calendarViewModel.scheduleItems,
            showCanvasItems = showCanvasItemsInCalendar,
            currentTimeMillis = currentTimeMillis
        )
    }

    val isShowingNews = showCIView || selectedNewsArticle != null
    val isShowingClubs = showClubs || selectedClub != null
    val isShowingCanvasInbox = showCanvasInbox
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val hideBottomBarForConversationTyping =
        currentScreen == Screen.Social && activeConversation != null && isKeyboardOpen

    val showCanvasMessagesAction =
        currentScreen == Screen.Social &&
                CanvasMessagingSettings.showCanvasMessaging &&
                canvasTokenStore.hasToken()

    val uiState = NavigationUiState(
        topBarState = NavigationTopBarState(
            title = resolveTopBarTitle(),
            showBackButton = shouldShowTopBarBack(),
            showSocialActions = currentScreen == Screen.Social &&
                    activeConversation == null &&
                    selectedProfile == null &&
                    !showNewConversation &&
                    !showSocialScreen,
            showCanvasMessagesAction = showCanvasMessagesAction,
            showSettingsActions = currentScreen == Screen.Settings &&
                    !showCanvasScreen &&
                    !showProfileEdit &&
                    selectedProfile == null,
            pendingRequestCount = pendingRequestCount,
            isHomeScreen = currentScreen == Screen.Home &&
                    selectedNewsArticle == null &&
                    !showCIView &&
                    selectedClub == null &&
                    !showClubs,
            nickname = userProfile.nickname,
            newConversationTopBarState = if (currentScreen == Screen.Social && showNewConversation) {
                newConversationTopBarState
            } else {
                null
            },
            conversationTopBarState = if (currentScreen == Screen.Social && activeConversation != null) {
                conversationTopBarState
            } else {
                null
            },
            // Restored: pass map and calendar top-bar states through to AppTopBar.
            mapTopBarState = if (currentScreen == Screen.Map) mapTopBarState else null,
            calendarTopBarState = if (currentScreen == Screen.Calendar) calendarTopBarState else null,
        ),
        currentScreen = currentScreen,
        userProfile = userProfile,
        calendarScheduleItems = calendarScheduleItems,
        manualUpcomingEventsItems = manualUpcomingEventsItems,
        displayUpcomingEventsItems = displayUpcomingEventsItems,
        activeConversation = activeConversation,
        selectedProfile = selectedProfile,
        showNewConversation = showNewConversation,
        showSocialScreen = showSocialScreen,
        showAddClassOnCalendar = showAddClassOnCalendar,
        showCanvasScreen = showCanvasScreen,
        showProfileEdit = showProfileEdit,
        openedConversationTimestamps = openedConversationTimestamps,
        showCIView = showCIView,
        selectedNewsArticle = selectedNewsArticle,
        showClubs = showClubs,
        selectedClub = selectedClub,
        preSelectedMapLocation = preSelectedMapLocation,
        autoRouteToPreSelectedMapLocation = autoRouteToPreSelectedMapLocation,
        sharedLocations = sharedLocations,
        isShowingNews = isShowingNews,
        isShowingClubs = isShowingClubs,
        hideBottomBarForConversationTyping = hideBottomBarForConversationTyping,
        showCanvasInbox = showCanvasInbox,
        selectedCanvasConversation = selectedCanvasConversation,
        isShowingCanvasInbox = isShowingCanvasInbox,
    )

    val routeCallbacks = NavigationRouteCallbacks(
        onUpdateManualEvents = {
            manualUpcomingEventsItems = it
            sharedPrefs.savePairItems("event_items", it)
        },
        onGoToScreen = { navigateToScreen(it) },
        onShowAddClass = {
            showAddClassOnCalendar = true
            navigateToScreen(Screen.Calendar)
        },
        onShowClubs = { showClubs = true },
        onShowCIView = { showCIView = true },
        onSelectNewsArticle = { selectedNewsArticle = it },
        onOpenHomeProfile = {
            profileOpenedFromHome = true
            selectedProfile = userProfile
            navigateToScreen(Screen.Settings)
        },
        onNavigateToLocation = { navigateToLocation(it) },
        onClearActiveConversation = { activeConversation = null },
        onResetSocialStack = {
            activeConversation = null
            selectedProfile = null
            showNewConversation = false
            showSocialScreen = false
            conversationTopBarState = null
        },
        onNavigateToCoordinates = { lat, lng, nickname, photoUrl ->
            val sharedLocation = CampusLocation(
                name = "$nickname's Location",
                category = "SHARED",
                description = photoUrl,
                coordinates = GeoPoint(lat, lng)
            )
            val alreadyExists = sharedLocations.any {
                it.coordinates.latitude == lat && it.coordinates.longitude == lng
            }
            if (!alreadyExists) sharedLocations = sharedLocations + sharedLocation
            preSelectedMapLocation = sharedLocation
            autoRouteToPreSelectedMapLocation = false
            navigateToScreen(Screen.Map)
        },
        onClearSelectedProfile = { selectedProfile = null },
        onShowProfileEdit = { showProfileEdit = true },
        onHideNewConversation = { showNewConversation = false },
        onOpenConversationFromNew = {
            showNewConversation = false
            newConversationTopBarState = null
            activeConversation = it
        },
        onOpenProfile = { selectedProfile = it },
        onOpenConversationWithFriend = { friend ->
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
        },
        onOpenConversationFromList = ::openConversation,
        onShowNewConversation = {
            newConversationTopBarState = null
            showNewConversation = true
        },
        onShowSocialScreen = { showSocialScreen = true },
        onSeedTimestamps = ::seedConversationTimestamps,
        onCalendarBack = {
            popBackStack()
            showAddClassOnCalendar = false
        },
        onHideCanvas = { showCanvasScreen = false },
        onCanvasSyncComplete = { calendarViewModel.refreshAllSavedCalendarItems() },
        onHideProfileEdit = { showProfileEdit = false },
        onProfileSaved = { authViewModel.silentReloadProfile() },
        onSettingsConversationOpened = {
            activeConversation = it
            navigateToScreen(Screen.Social)
        },
        onSettingsSelectedProfileBack = handleClearProfile,
        onSignOut = onSignOut,
        onSettingsChange = { dark, notify, theme ->
            authViewModel.updateSettings(dark, notify, theme)
        },
        onReadReceiptsChange = { authViewModel.updateReadReceiptsEnabled(it) },
        onViewProfile = {
            profileOpenedFromHome = false
            selectedProfile = userProfile
        },
        onOpenCanvas = { showCanvasScreen = true },
        onNewConversationTopBarChange = { newConversationTopBarState = it },
        onConversationTopBarChange = { conversationTopBarState = it },
        onOpenChatFromHome = { otherUser ->
            socialScope.launch {
                socialRepository.getOrCreateConversation(
                    participantIds = listOf(userProfile.uid, otherUser.uid),
                    participantNicknames = mapOf(
                        userProfile.uid to userProfile.nickname,
                        otherUser.uid to otherUser.nickname
                    )
                ).onSuccess {
                    activeConversation = it
                }
            }
        },
        onShowCanvasInbox = {
            activeConversation = null
            selectedProfile = null
            showNewConversation = false
            showSocialScreen = false
            newConversationTopBarState = null
            conversationTopBarState = null
            showCanvasInbox = true
        },
        onOpenCanvasConversation = { selectedCanvasConversation = it },
    )

    NavigationBackHandler(
        currentScreen = currentScreen,
        backStackSize = backStack.size,
        activeConversation = activeConversation,
        selectedProfile = selectedProfile,
        showNewConversation = showNewConversation,
        showSocialScreen = showSocialScreen,
        showProfileEdit = showProfileEdit,
        showCanvasScreen = showCanvasScreen,
        showAddClassOnCalendar = showAddClassOnCalendar,
        showCIView = showCIView,
        selectedNewsArticle = selectedNewsArticle,
        showClubs = showClubs,
        selectedClub = selectedClub,
        showCanvasInbox = showCanvasInbox,
        selectedCanvasConversation = selectedCanvasConversation,
        onHideCanvas = { showCanvasScreen = false },
        onHideAddClass = { showAddClassOnCalendar = false },
        onClearSelectedNewsArticle = { selectedNewsArticle = null },
        onShowCIView = { showCIView = true },
        onHideCIView = { showCIView = false },
        onClearSelectedClub = { selectedClub = null },
        onHideClubs = { showClubs = false },
        onClearActiveConversation = { activeConversation = null },
        onHideNewConversation = { showNewConversation = false },
        onClearSelectedProfile = handleClearProfile,
        onHideSocialScreen = { showSocialScreen = false },
        onHideProfileEdit = { showProfileEdit = false },
        onCloseCanvasConversation = { selectedCanvasConversation = null },
        onHideCanvasInbox = { showCanvasInbox = false },
        onGoBack = ::popBackStack
    )

    NavigationScaffoldContent(
        uiState = uiState,
        routeCallbacks = routeCallbacks,
        onBottomScreenSelected = ::handleBottomScreenSelected,
        onTopBarBack = ::handleTopBarBack,
        onTopBarFriendsClick = {
            activeConversation = null
            selectedProfile = null
            showNewConversation = false
            showSocialScreen = true
        },
        onTopBarNewMessageClick = {
            activeConversation = null
            selectedProfile = null
            showSocialScreen = false
            newConversationTopBarState = null
            showNewConversation = true
        },
        onTopBarCanvasMessagesClick = routeCallbacks.onShowCanvasInbox,
        onMapBack = ::popBackStack,
        onMapFinishedLoading = {
            preSelectedMapLocation = null
            autoRouteToPreSelectedMapLocation = false
        },
        onRemoveExtraLocation = { sharedLocations = sharedLocations - it },
        onArticleClick = { selectedNewsArticle = it },
        onNewsBack = {
            if (selectedNewsArticle?.title == "Study Rooms" || selectedNewsArticle?.title == "Dining") {
                selectedNewsArticle = null
                showCIView = false
            } else if (selectedNewsArticle != null) {
                selectedNewsArticle = null
                showCIView = true
            } else {
                showCIView = false
            }
        },
        onClubClick = { selectedClub = it },
        onClubsBack = {
            if (selectedClub != null) selectedClub = null else showClubs = false
        },
        onCanvasConversationClick = { selectedCanvasConversation = it },
        onSettingsCanvasClick = routeCallbacks.onOpenCanvas,
        onSettingsSignOutClick = routeCallbacks.onSignOut,
    )
}