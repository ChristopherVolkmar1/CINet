package com.example.cinet.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.cinet.R
import com.example.cinet.core.permissions.PermissionManager
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.MeetupPin
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import com.example.cinet.feature.settings.AppSettings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import com.google.maps.model.TravelMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@Composable
fun CampusMapScreen(
    onBack: () -> Unit,
    viewModel: CampusRegistry = androidx.lifecycle.viewmodel.compose.viewModel(),
    preSelectedLocation: CampusLocation? = null,
    autoRouteToPreSelectedLocation: Boolean = false,
    onFinishedLoading: () -> Unit = {},
    extraLocations: List<CampusLocation> = emptyList(),
    onRemoveExtraLocation: ((CampusLocation) -> Unit)? = null,
    onTopBarStateChanged: (MapTopBarState?) -> Unit = {},
) {
    val context = LocalContext.current
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUid = auth.currentUser?.uid.orEmpty()

    var hasPermission by remember { mutableStateOf(PermissionManager.hasAllPermissions(context)) }
    val mapStyle = rememberCampusMapStyle(context)
    val mapProperties = rememberCampusMapProperties(hasPermission, mapStyle)

    androidx.activity.compose.BackHandler { onBack() }

    val campusRegistry by viewModel.campusRegistry.collectAsState()
    var selectedLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var routeLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var activeFilters by remember { mutableStateOf(setOf<String>()) }
    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var durations by remember { mutableStateOf(RouteDurations()) }
    var activeTravelMode by remember { mutableStateOf(TravelMode.WALKING) }
    var showRemoveRoute by remember { mutableStateOf(false) }
    var eta by remember { mutableStateOf("") }
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }
    var showBusSheet by remember { mutableStateOf(false) }

    var meetupPins by remember { mutableStateOf<List<MeetupPin>>(emptyList()) }
    var pendingMeetupCoordinate by remember { mutableStateOf<LatLng?>(null) }
    var selectedMeetupPin by remember { mutableStateOf<MeetupPin?>(null) }
    var sharePinDialogPin by remember { mutableStateOf<MeetupPin?>(null) }
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var reportMessage by remember { mutableStateOf<String?>(null) }
    var currentUserNickname by remember { mutableStateOf("") }
    var currentUserPhotoUrl by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        var publicPins = emptyList<MeetupPin>()
        var myPins = emptyList<MeetupPin>()

        fun updatePins() {
            meetupPins = (publicPins + myPins)
                .distinctBy { it.id }
                .filterNot { it.isExpired }
        }

        val publicListener = firestore.collection("meetupPins")
            .whereEqualTo("sharedToSocial", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MeetupPins", "Failed to load public meetup pins", error)
                    return@addSnapshotListener
                }

                publicPins = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(MeetupPin::class.java)?.copy(id = document.id)
                } ?: emptyList()

                updatePins()
            }

        val myPinsListener = firestore.collection("meetupPins")
            .whereEqualTo("creatorUid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MeetupPins", "Failed to load my meetup pins", error)
                    return@addSnapshotListener
                }

                myPins = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(MeetupPin::class.java)?.copy(id = document.id)
                } ?: emptyList()

                updatePins()
            }

        onDispose {
            publicListener.remove()
            myPinsListener.remove()
        }
    }

    val visibleMeetupPins = remember(meetupPins) {
        meetupPins.filterNot { it.isExpired }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(34.162, -119.043), 16f)
    }

    val filteredNames = remember(textFieldState.text, campusRegistry) {
        getFilteredLocations(
            fullRegistry = campusRegistry,
            activeFilters = emptySet(),
            searchQuery = textFieldState.text.toString()
        ).map { it.name }
    }

    val markersToDraw = remember(
        activeFilters,
        textFieldState.text,
        campusRegistry,
        selectedLocation,
        routeLocation,
        showRemoveRoute
    ) {
        computeMarkersToDraw(
            registry = campusRegistry,
            activeFilters = activeFilters,
            searchQuery = textFieldState.text.toString()
        )
    }

    val requestRoute: (TravelMode) -> Unit = { mode ->
        activeTravelMode = mode
        requestRouteToDestination(
            destination = selectedLocation?.latLng,
            hasPermission = hasPermission,
            fusedLocationClient = fusedLocationClient,
            context = context,
            mode = mode,
            cameraPositionState = cameraPositionState,
            coroutineScope = coroutineScope,
            onUserLatLng = { userLatLng = it },
            onEta = { eta = it },
            onPolylinePoints = { polylinePoints = it }
        )
    }

    val requestRouteToMeetupPin: (MeetupPin, TravelMode) -> Unit = { pin, mode ->
        val meetupLocation = CampusLocation(
            name = pin.title,
            description = pin.description,
            coordinates = GeoPoint(pin.latitude, pin.longitude),
            category = "SHARED"
        )

        selectedLocation = meetupLocation
        routeLocation = meetupLocation
        activeTravelMode = mode

        requestRouteToDestination(
            destination = pin.latLng,
            hasPermission = hasPermission,
            fusedLocationClient = fusedLocationClient,
            context = context,
            mode = mode,
            cameraPositionState = cameraPositionState,
            coroutineScope = coroutineScope,
            onUserLatLng = { userLatLng = it },
            onEta = { eta = it },
            onPolylinePoints = { polylinePoints = it }
        )

        showRemoveRoute = true
        selectedMeetupPin = null
    }

    val searchState = SearchState(
        textFieldState = textFieldState,
        results = filteredNames,
        onSearch = { query ->
            selectLocationFromSearch(
                query = query,
                registry = campusRegistry,
                cameraPositionState = cameraPositionState,
                coroutineScope = coroutineScope,
                onSelectedLocation = { selectedLocation = it },
                onClearRoute = {
                    polylinePoints = emptyList()
                    showRemoveRoute = false
                    routeLocation = null
                }
            )
            textFieldState.edit { replace(0, length, "") }
        }
    )

    val mapTopBarState = MapTopBarState(
        searchState = searchState,
        categories = campusRegistry.keys.map { it.uppercase() }.toSet() + "TRANSIT" + "SHARED",
        activeFilters = activeFilters,
        onFiltersChanged = { activeFilters = it }
    )

    SideEffect {
        onTopBarStateChanged(mapTopBarState)
    }

    DisposableEffect(Unit) {
        onDispose {
            onTopBarStateChanged(null)
        }
    }

    val repository = remember { SocialRepository() }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getFriends().onSuccess {
            friends = it
        }

        repository.getConversations().onSuccess {
            conversations = it
        }

        if (currentUid.isNotBlank()) {
            firestore.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener { snapshot ->
                    currentUserNickname = snapshot.getString("nickname") ?: ""
                    currentUserPhotoUrl = snapshot.getString("photoUrl") ?: ""
                }
                .addOnFailureListener { error ->
                    Log.e("MapShare", "Failed to load current user profile", error)
                }
        }
    }

    InitializeCampusState(
        hasPermission = hasPermission,
        fusedLocationClient = fusedLocationClient,
        textFieldState = textFieldState,
        onSelectedLocation = { selectedLocation = it },
        onUserLatLng = { userLatLng = it }
    )

    ApplyPreSelectedLocation(
        preSelectedLocation = preSelectedLocation,
        autoRouteToPreSelectedLocation = autoRouteToPreSelectedLocation,
        hasPermission = hasPermission,
        fusedLocationClient = fusedLocationClient,
        context = context,
        cameraPositionState = cameraPositionState,
        coroutineScope = coroutineScope,
        onSelectedLocation = { selectedLocation = it },
        onUserLatLng = { userLatLng = it },
        onEta = { eta = it },
        onPolylinePoints = { polylinePoints = it },
        onRouteVisible = { showRemoveRoute = true },
        onTravelModeSelected = { activeTravelMode = it },
        onFinishedLoading = onFinishedLoading
    )

    ObserveRouteDurations(
        selectedLocation = selectedLocation,
        userLatLng = userLatLng,
        hasPermission = hasPermission,
        fusedLocationClient = fusedLocationClient,
        context = context,
        coroutineScope = coroutineScope,
        onDurationsUpdate = { durations = it },
        onRouteLocationUpdate = { routeLocation = it }
    )

    ObserveUserLocationUpdates(
        hasPermission = hasPermission,
        fusedLocationClient = fusedLocationClient,
        onUserLatLng = { userLatLng = it }
    )

    Box {
        CampusMapLayer(
            mapProperties = mapProperties,
            cameraPositionState = cameraPositionState,
            focusManager = focusManager,
            markers = markersToDraw + extraLocations,
            meetupPins = visibleMeetupPins,
            polylinePoints = polylinePoints,
            coroutineScope = coroutineScope,
            onMarkerSelected = { selectedLocation = it },
            onMeetupPinSelected = { selectedMeetupPin = it },
            onMapLongClick = { pendingMeetupCoordinate = it },
            onRouteVisible = { showRemoveRoute = true },
            mode = activeTravelMode
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
        ) {
            if (showRemoveRoute) {
                RemoveRoute(
                    onDismiss = {
                        polylinePoints = emptyList()
                        showRemoveRoute = false
                        routeLocation = null
                    },
                    routeDurations = durations,
                    location = routeLocation,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    travelMode = activeTravelMode,
                    eta = eta
                )
            }
        }

        MapControls(
            campusRegistry = campusRegistry,
            searchState = searchState,
            onFiltersChanged = { activeFilters = it },
            activeFilters = activeFilters,
            selectedLocation = selectedLocation,
            onDismissPopup = { selectedLocation = null },
            onModeSelected = requestRoute,
            routeDurations = durations,
            onShowBusSchedule = { showBusSheet = true },
            friends = friends,
            onRemoveLocation = onRemoveExtraLocation,
            photoUrl = selectedLocation?.description ?: ""
        )

        userLatLng?.let { user ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 11.dp, bottom = 86.dp)
            ) {
                CenterCamera(location = user, cameraPositionState = cameraPositionState)
            }
        }

        if (showBusSheet) {
            BusScheduleSheet(onDismiss = { showBusSheet = false })
        }

        pendingMeetupCoordinate?.let { coordinate ->
            CreateMeetupPinDialog(
                coordinate = coordinate,
                onDismiss = { pendingMeetupCoordinate = null },
                onCreatePin = { pin ->
                    firestore.collection("meetupPins")
                        .add(pin)
                        .addOnSuccessListener { documentReference ->
                            Log.d("MeetupPins", "Meetup pin saved successfully: ${documentReference.id}")
                            pendingMeetupCoordinate = null
                        }
                        .addOnFailureListener { error ->
                            Log.e("MeetupPins", "Failed to save meetup pin", error)
                            reportMessage = "Failed to save meetup pin: ${error.message}"
                        }
                }
            )
        }

        selectedMeetupPin?.let { pin ->
            MeetupPinDetailsDialog(
                pin = pin,
                onDismiss = {
                    selectedMeetupPin = null
                },
                onDelete = {
                    if (pin.id.isBlank()) {
                        selectedMeetupPin = null
                        reportMessage = "This pin cannot be deleted yet."
                        return@MeetupPinDetailsDialog
                    }

                    firestore.collection("meetupPins")
                        .document(pin.id)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("MeetupPins", "Meetup pin deleted successfully")
                            selectedMeetupPin = null
                        }
                        .addOnFailureListener { error ->
                            Log.e("MeetupPins", "Failed to delete meetup pin", error)
                            reportMessage = "Failed to delete meetup pin: ${error.message}"
                        }
                },
                onShare = {
                    coroutineScope.launch {
                        repository.getConversations()
                            .onSuccess { loadedConversations ->
                                conversations = loadedConversations
                                sharePinDialogPin = pin
                                selectedMeetupPin = null
                            }
                            .onFailure { error ->
                                reportMessage = "Failed to load conversations: ${error.message}"
                            }
                    }
                },

                onDirections = {
                    requestRouteToMeetupPin(pin, TravelMode.WALKING)
                }
            )
        }

        sharePinDialogPin?.let { pin ->
            AlertDialog(
                onDismissRequest = {
                    sharePinDialogPin = null
                },
                title = {
                    Text("Share Pin")
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Choose a conversation")

                        if (conversations.isEmpty()) {
                            Text("No conversations found. Try opening Messages once, then come back.")
                        } else {
                            conversations.forEach { conversation ->
                                val title =
                                    if (conversation.isGroup) {
                                        conversation.groupName.ifBlank { "Group Chat" }
                                    } else {
                                        conversation.participantNicknames.entries
                                            .firstOrNull { it.key != currentUid }
                                            ?.value
                                            ?: conversation.participantNicknames.values.firstOrNull()
                                            ?: "Conversation"
                                    }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val result = repository.sendMessage(
                                                conversationId = conversation.id,
                                                content = "Shared a meetup pin: ${pin.title}",
                                                type = "location_share",
                                                metadata = mapOf(
                                                    "lat" to pin.latitude.toString(),
                                                    "lng" to pin.longitude.toString(),
                                                    "locationName" to pin.title,
                                                    "senderNickname" to currentUserNickname.ifBlank { "Friend" },
                                                    "senderPhotoUrl" to currentUserPhotoUrl
                                                )
                                            )

                                            result
                                                .onSuccess {
                                                    sharePinDialogPin = null
                                                    reportMessage = "Pin shared successfully."
                                                }
                                                .onFailure { error ->
                                                    Log.e("MapShare", "Failed to share pin to chat", error)
                                                    reportMessage = "Failed to share pin: ${error.message}"
                                                }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(title)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            sharePinDialogPin = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        reportMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { reportMessage = null },
                title = { Text("Notice") },
                text = { Text(message) },
                confirmButton = {
                    Button(onClick = { reportMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun rememberCampusMapStyle(context: Context): MapStyleOptions? =
    remember(AppSettings.isDarkMode) {
        if (AppSettings.isDarkMode) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) as MapStyleOptions?
        } else {
            null
        }
    }

@Composable
private fun rememberCampusMapProperties(
    hasPermission: Boolean,
    mapStyle: MapStyleOptions?
): MapProperties =
    remember(hasPermission, mapStyle) {
        MapProperties(
            isMyLocationEnabled = hasPermission,
            mapStyleOptions = mapStyle,
            isBuildingEnabled = true
        )
    }

private fun computeMarkersToDraw(
    registry: Map<String, List<CampusLocation>>,
    activeFilters: Set<String>,
    searchQuery: String
): List<CampusLocation> {
    val allLocations = getFilteredLocations(registry, activeFilters, searchQuery)
    return when {
        activeFilters.isEmpty() -> allLocations
        else -> allLocations.filter { activeFilters.contains(it.category) }
    }
}

@Composable
private fun InitializeCampusState(
    hasPermission: Boolean,
    fusedLocationClient: FusedLocationProviderClient,
    textFieldState: TextFieldState,
    onSelectedLocation: (CampusLocation?) -> Unit,
    onUserLatLng: (LatLng) -> Unit
) {
    LaunchedEffect(Unit) {
        textFieldState.edit { replace(0, length, "") }
        onSelectedLocation(null)

        if (hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    onUserLatLng(
                        if (location != null) LatLng(location.latitude, location.longitude)
                        else LatLng(34.162, -119.043)
                    )
                }
            } catch (_: SecurityException) {
                Log.e("Location", "Permission missing")
            }
        } else {
            onUserLatLng(LatLng(34.162, -119.043))
        }
    }
}

@Composable
private fun ApplyPreSelectedLocation(
    preSelectedLocation: CampusLocation?,
    autoRouteToPreSelectedLocation: Boolean,
    hasPermission: Boolean,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope,
    onSelectedLocation: (CampusLocation) -> Unit,
    onUserLatLng: (LatLng) -> Unit,
    onEta: (String) -> Unit,
    onPolylinePoints: (List<LatLng>) -> Unit,
    onRouteVisible: () -> Unit,
    onTravelModeSelected: (TravelMode) -> Unit,
    onFinishedLoading: () -> Unit
) {
    LaunchedEffect(preSelectedLocation, autoRouteToPreSelectedLocation, hasPermission) {
        if (preSelectedLocation == null || preSelectedLocation.coordinates.latitude == 0.0) {
            return@LaunchedEffect
        }

        onSelectedLocation(preSelectedLocation)

        if (autoRouteToPreSelectedLocation) {
            onTravelModeSelected(TravelMode.WALKING)
            requestRouteToDestination(
                destination = preSelectedLocation.latLng,
                hasPermission = hasPermission,
                fusedLocationClient = fusedLocationClient,
                context = context,
                mode = TravelMode.WALKING,
                cameraPositionState = cameraPositionState,
                coroutineScope = coroutineScope,
                onUserLatLng = onUserLatLng,
                onEta = onEta,
                onPolylinePoints = onPolylinePoints
            )
            onRouteVisible()
        } else {
            cameraPositionState.move(
                update = CameraUpdateFactory.newLatLngZoom(preSelectedLocation.latLng, 18f)
            )
        }

        onFinishedLoading()
    }
}

@Composable
private fun ObserveRouteDurations(
    selectedLocation: CampusLocation?,
    userLatLng: LatLng?,
    hasPermission: Boolean,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    coroutineScope: CoroutineScope,
    onDurationsUpdate: (RouteDurations) -> Unit,
    onRouteLocationUpdate: (CampusLocation?) -> Unit
) {
    LaunchedEffect(selectedLocation, userLatLng) {
        val destination = selectedLocation?.latLng ?: return@LaunchedEffect
        val start = userLatLng ?: return@LaunchedEffect
        if (!hasPermission) return@LaunchedEffect

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                coroutineScope.launch {
                    val drive = async { fetchDirections(start, destination, context, TravelMode.DRIVING) }
                    val walk = async { fetchDirections(start, destination, context, TravelMode.WALKING) }
                    val bike = async { fetchDirections(start, destination, context, TravelMode.BICYCLING) }

                    val driving = drive.await()
                    val walking = walk.await()
                    val biking = bike.await()

                    onDurationsUpdate(
                        RouteDurations(
                            driving = driving.duration,
                            walking = walking.duration,
                            biking = biking.duration
                        )
                    )
                    onRouteLocationUpdate(selectedLocation)
                }
            }
        } catch (_: SecurityException) {
            Log.e("Location", "Permission missing")
        }
    }
}

@Composable
private fun ObserveUserLocationUpdates(
    hasPermission: Boolean,
    fusedLocationClient: FusedLocationProviderClient,
    onUserLatLng: (LatLng) -> Unit
) {
    DisposableEffect(hasPermission) {
        if (!hasPermission) return@DisposableEffect onDispose {}

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let { onUserLatLng(LatLng(it.latitude, it.longitude)) }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                android.os.Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            Log.e("Location", "Permission missing")
        }

        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }
}

private fun requestRouteToDestination(
    destination: LatLng?,
    hasPermission: Boolean,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context,
    mode: TravelMode,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope,
    onUserLatLng: (LatLng) -> Unit,
    onEta: (String) -> Unit,
    onPolylinePoints: (List<LatLng>) -> Unit
) {
    if (destination == null || !hasPermission) return

    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val start = LatLng(it.latitude, it.longitude)
                onUserLatLng(start)
                coroutineScope.launch {
                    val result = fetchDirections(start, destination, context, mode)
                    onEta(result.eta)
                    onPolylinePoints(result.points)
                    animateToRouteBounds(cameraPositionState, start, destination)
                }
            }
        }
    } catch (_: SecurityException) {
        Log.e("Route", "No Permission")
    }
}

private suspend fun animateToRouteBounds(
    cameraPositionState: CameraPositionState,
    start: LatLng,
    destination: LatLng
) {
    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
        .include(start)
        .include(destination)
        .build()

    cameraPositionState.animate(
        update = CameraUpdateFactory.newLatLngBounds(bounds, 100),
        durationMs = 1000
    )
}

private fun selectLocationFromSearch(
    query: String,
    registry: Map<String, List<CampusLocation>>,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope,
    onSelectedLocation: (CampusLocation) -> Unit,
    onClearRoute: () -> Unit
) {
    val target = registry.values.flatten()
        .find { it.name.equals(query, ignoreCase = true) }
        ?: return

    onSelectedLocation(target)
    onClearRoute()

    coroutineScope.launch {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(target.latLng, 18f),
            durationMs = 1000
        )
    }
}

@Composable
private fun CampusMapLayer(
    mapProperties: MapProperties,
    cameraPositionState: CameraPositionState,
    focusManager: FocusManager,
    markers: List<CampusLocation>,
    meetupPins: List<MeetupPin>,
    polylinePoints: List<LatLng>,
    coroutineScope: CoroutineScope,
    onMarkerSelected: (CampusLocation) -> Unit,
    onMeetupPinSelected: (MeetupPin) -> Unit,
    onMapLongClick: (LatLng) -> Unit,
    onRouteVisible: () -> Unit,
    mode: TravelMode
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        properties = mapProperties,
        cameraPositionState = cameraPositionState,
        onMapClick = { focusManager.clearFocus() },
        onMapLongClick = {
            focusManager.clearFocus()
            onMapLongClick(it)
        },
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = true,
            tiltGesturesEnabled = true
        )
    ) {
        markers.forEach { location ->
            CampusMarker(
                location = location,
                cameraPositionState = cameraPositionState,
                coroutineScope = coroutineScope,
                onSelected = onMarkerSelected
            )
        }

        meetupPins.forEach { pin ->
            MeetupPinMarker(
                pin = pin,
                cameraPositionState = cameraPositionState,
                coroutineScope = coroutineScope,
                onSelected = onMeetupPinSelected
            )
        }

        if (polylinePoints.isNotEmpty()) {
            if (mode == TravelMode.DRIVING || mode == TravelMode.BICYCLING) {
                Polyline(
                    points = polylinePoints,
                    color = Color(0xFF4285F4),
                    width = 12f,
                    jointType = JointType.ROUND
                )
            } else if (mode == TravelMode.WALKING) {
                Polyline(
                    points = polylinePoints,
                    color = Color(0xFF4285F4),
                    width = 12f,
                    jointType = JointType.ROUND,
                    pattern = listOf(Dot(), Gap(20f))
                )
            }

            onRouteVisible()
        }
    }
}

@Composable
private fun CampusMarker(
    location: CampusLocation,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope,
    onSelected: (CampusLocation) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    val customIcon = remember(location.category, primaryColor) {
        try {
            customMarker(
                context = context,
                iconResId = when (location.category) {
                    "ACADEMIC" -> R.drawable.school
                    "TRANSIT" -> R.drawable.bus_stop
                    "COMMUTER_PARKING" -> R.drawable.parking
                    "DINING" -> R.drawable.dining
                    "SHARED" -> R.drawable.person
                    else -> R.drawable.unlisted
                },
                backgroundColor = primaryColor
            )
        } catch (_: Exception) {
            null
        }
    }

    val markerState = remember(location.name) {
        MarkerState(position = location.latLng)
    }

    Marker(
        state = markerState,
        title = location.name,
        snippet = "Category: ${location.category.lowercase()}",
        icon = customIcon ?: BitmapDescriptorFactory.defaultMarker(),
        onClick = {
            onSelected(location)
            coroutineScope.launch {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(location.latLng, 18f),
                    durationMs = 1000
                )
            }
            true
        }
    )
}

@Composable
private fun CreateMeetupPinDialog(
    coordinate: LatLng,
    onDismiss: () -> Unit,
    onCreatePin: (MeetupPin) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isTemporary by remember { mutableStateOf(true) }
    var durationHoursText by remember { mutableStateOf("2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Meetup Pin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = isTemporary,
                        onCheckedChange = { isTemporary = it }
                    )
                    Text(if (isTemporary) "Temporary pin" else "Permanent pin")
                }

                if (isTemporary) {
                    TextField(
                        value = durationHoursText,
                        onValueChange = { durationHoursText = it.filter { char -> char.isDigit() } },
                        label = { Text("Expires in hours") },
                        singleLine = true
                    )
                }

                Text(
                    text = "Location: %.5f, %.5f".format(coordinate.latitude, coordinate.longitude),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val durationHours = durationHoursText.toLongOrNull()?.coerceAtLeast(1L) ?: 2L
                    val now = System.currentTimeMillis()

                    onCreatePin(
                        MeetupPin(
                            title = title.trim(),
                            description = description.trim(),
                            latitude = coordinate.latitude,
                            longitude = coordinate.longitude,
                            isTemporary = isTemporary,
                            sharedToSocial = false,
                            creatorUid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                            creatorName = "Bryan",
                            createdAt = Timestamp.now(),
                            expiresAt = if (isTemporary) {
                                Timestamp(
                                    java.util.Date(
                                        now + durationHours * 60L * 60L * 1000L
                                    )
                                )
                            } else {
                                null
                            }
                        )
                    )
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MeetupPinDetailsDialog(
    pin: MeetupPin,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onDirections: () -> Unit
) {
    val expirationText = pin.expiresAt?.let {
        val minutesLeft =
            ((it.toDate().time - System.currentTimeMillis()) / 60000L)
                .coerceAtLeast(0L)

        "Expires in about $minutesLeft minutes"
    } ?: "Permanent pin"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pin.title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pin.description.isNotBlank()) {
                    Text(pin.description)
                }

                Text(
                    expirationText,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = if (pin.sharedToSocial) {
                        "Shared to Social"
                    } else {
                        "Not shared to Social"
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Location: %.5f, %.5f".format(pin.latitude, pin.longitude),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDirections,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get Directions")
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share to Chat")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Pin")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MeetupPinMarker(
    pin: MeetupPin,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope,
    onSelected: (MeetupPin) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val customIcon = remember(primaryColor) {
        try {
            customMarker(
                context = context,
                iconResId = R.drawable.person,
                backgroundColor = primaryColor
            )
        } catch (_: Exception) {
            null
        }
    }

    val markerState = remember(pin.id) {
        MarkerState(position = pin.latLng)
    }

    Marker(
        state = markerState,
        title = pin.title,
        snippet = if (pin.isTemporary) "Temporary meetup pin" else "Permanent meetup pin",
        icon = customIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
        onClick = {
            onSelected(pin)
            coroutineScope.launch {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(pin.latLng, 18f),
                    durationMs = 1000
                )
            }
            true
        }
    )
}

fun customMarker(
    context: Context,
    iconResId: Int,
    backgroundColor: Color
): BitmapDescriptor? {
    val pinDrawable = ContextCompat.getDrawable(context, R.drawable.pin)
    val iconDrawable = ContextCompat.getDrawable(context, iconResId)
    val size = 105
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    pinDrawable?.let { drawable ->
        val wrapped = androidx.core.graphics.drawable.DrawableCompat
            .wrap(drawable)
            .mutate()

        androidx.core.graphics.drawable.DrawableCompat.setTint(
            wrapped,
            backgroundColor.toArgb()
        )

        wrapped.setBounds(0, 0, size, size)
        wrapped.draw(canvas)
    }

    iconDrawable?.let {
        it.setTint(android.graphics.Color.WHITE)
        val iconSize = (size * 0.5).toInt()
        val left = (size - iconSize) / 2
        val top = size / 10

        it.setBounds(left, top, left + iconSize, top + iconSize)
        it.draw(canvas)
    }

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}