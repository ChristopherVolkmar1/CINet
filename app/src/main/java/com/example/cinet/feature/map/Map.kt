package com.example.cinet

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.ImageButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.ui.theme.CINetTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.firestore.GeoPoint
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.compose.*
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.TravelMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// -------------------- Data classes --------------------

data class CampusLocation(
    val name: String = "",
    val category: String = "",
    val coordinates: GeoPoint = GeoPoint(0.0, 0.0),
    val description: String = ""
) {
    val latLng: LatLng
        get() = LatLng(coordinates.latitude, coordinates.longitude)
}

data class SearchState(
    val textFieldState: TextFieldState,
    val results: List<String>,
    val onSearch: (String) -> Unit
)

data class DirectionsResult(
    val points: List<LatLng>,
    val duration: String,
    val travelMode: TravelMode,
    val eta: String
)

data class RouteDurations(
    var driving: String = "",
    var walking: String = "",
    var biking: String = ""
)

// -------------------- Main screen --------------------

/** Top-level campus map screen: wires together state, side-effects, the map layer, and overlay controls. */
@Composable
fun CampusMapScreen(
    onBack: () -> Unit,
    viewModel: CampusRegistry = androidx.lifecycle.viewmodel.compose.viewModel(),
    preSelectedLocation: CampusLocation? = null,
    onFinishedLoading: () -> Unit = {}
) {
    val context = LocalContext.current
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasPermission by remember { mutableStateOf(PermissionManager.hasAllPermissions(context)) }
    val mapStyle = rememberCampusMapStyle(context)
    val mapProperties = rememberCampusMapProperties(hasPermission, mapStyle)

    androidx.activity.compose.BackHandler { onBack() }

    val campusRegistry by viewModel.campusRegistry.collectAsState()
    var selectedLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var routeLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var activeFilter by remember { mutableStateOf<String?>(null) }
    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var durations by remember { mutableStateOf(RouteDurations()) }
    var activeTravelMode by remember { mutableStateOf(TravelMode.WALKING) }
    var showRemoveRoute by remember { mutableStateOf(false) }
    var eta by remember { mutableStateOf("") }
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(34.162, -119.043), 16f)
    }

    val filteredNames = remember(textFieldState.text, activeFilter, campusRegistry) {
        getFilteredLocations(campusRegistry, activeFilter, textFieldState.text.toString()).map { it.name }
    }

    val markersToDraw = remember(
        activeFilter, textFieldState.text, campusRegistry, selectedLocation, showRemoveRoute
    ) {
        computeMarkersToDraw(
            registry = campusRegistry,
            activeFilter = activeFilter,
            searchQuery = textFieldState.text.toString(),
            selectedLocation = selectedLocation,
            routeLocation = routeLocation,
            showRemoveRoute = showRemoveRoute
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

    InitializeCampusState(
        hasPermission = hasPermission,
        fusedLocationClient = fusedLocationClient,
        textFieldState = textFieldState,
        onPolylinePoints = { polylinePoints = it },
        onSelectedLocation = { selectedLocation = it },
        onUserLatLng = { userLatLng = it }
    )

    ApplyPreSelectedLocation(
        preSelectedLocation = preSelectedLocation,
        cameraPositionState = cameraPositionState,
        onSelectedLocation = { selectedLocation = it },
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
            markers = markersToDraw,
            polylinePoints = polylinePoints,
            coroutineScope = coroutineScope,
            onMarkerSelected = { selectedLocation = it },
            onRouteVisible = { showRemoveRoute = true }
        )

        Box(modifier = Modifier.fillMaxSize().align(Alignment.BottomCenter)) {
            if (showRemoveRoute) {
                RemoveRoute(
                    onDismiss = {
                        polylinePoints = emptyList()
                        showRemoveRoute = false
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
            onFilterChange = { activeFilter = it },
            selectedLocation = selectedLocation,
            onDismissPopup = { selectedLocation = null },
            onModeSelected = requestRoute,
            routeDurations = durations
        )

        userLatLng?.let { user ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 11.dp, bottom = 86.dp)
            ) {
                CenterSelf(user = user, cameraPositionState = cameraPositionState)
            }
        }
    }
}

// -------------------- Derived state helpers --------------------

/** Returns the dark-mode map style options when dark mode is on, otherwise null (default style). */
@Composable
private fun rememberCampusMapStyle(context: Context): MapStyleOptions? =
    remember(AppSettings.isDarkMode) {
        if (AppSettings.isDarkMode) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) as MapStyleOptions?
        } else {
            null
        }
    }

/** Builds the GoogleMap MapProperties, reacting to permission and style changes. */
@Composable
private fun rememberCampusMapProperties(
    hasPermission: Boolean,
    mapStyle: MapStyleOptions?
): MapProperties =
    remember(hasPermission, mapStyle) {
        MapProperties(
            isMyLocationEnabled = hasPermission,
            mapStyleOptions = mapStyle
        )
    }

/** Decides which campus-location markers should be drawn given filter, search, selection, and route state. */
private fun computeMarkersToDraw(
    registry: Map<String, List<CampusLocation>>,
    activeFilter: String?,
    searchQuery: String,
    selectedLocation: CampusLocation?,
    routeLocation: CampusLocation?,
    showRemoveRoute: Boolean
): List<CampusLocation> {
    val filtered = getFilteredLocations(registry, activeFilter, searchQuery)
    return when {
        showRemoveRoute -> filtered.filter { it.name == routeLocation?.name }
        selectedLocation != null -> filtered.filter { it.name == selectedLocation.name }
        else -> filtered
    }
}

// -------------------- Side-effects --------------------

/** Resets search/polyline/selection once on first composition and seeds the user location from last-known or default. */
@Composable
private fun InitializeCampusState(
    hasPermission: Boolean,
    fusedLocationClient: FusedLocationProviderClient,
    textFieldState: TextFieldState,
    onPolylinePoints: (List<LatLng>) -> Unit,
    onSelectedLocation: (CampusLocation?) -> Unit,
    onUserLatLng: (LatLng) -> Unit
) {
    LaunchedEffect(Unit) {
        textFieldState.edit { replace(0, length, "") }
        onPolylinePoints(emptyList())
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

/** Centers the map on an initially-supplied location and notifies the caller when done. */
@Composable
private fun ApplyPreSelectedLocation(
    preSelectedLocation: CampusLocation?,
    cameraPositionState: CameraPositionState,
    onSelectedLocation: (CampusLocation) -> Unit,
    onFinishedLoading: () -> Unit
) {
    LaunchedEffect(preSelectedLocation) {
        if (preSelectedLocation == null || preSelectedLocation.coordinates.latitude == 0.0) return@LaunchedEffect
        onSelectedLocation(preSelectedLocation)
        cameraPositionState.move(
            update = CameraUpdateFactory.newLatLngZoom(preSelectedLocation.latLng, 18f)
        )
        onFinishedLoading()
    }
}

/** Recomputes driving/walking/biking durations whenever the selected destination or user position changes. */
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
            fusedLocationClient.lastLocation.addOnSuccessListener { _ ->
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

/** Subscribes to continuous user-location updates while permission is granted; unsubscribes on dispose. */
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
            2000L // update every 2 seconds
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

// -------------------- Action handlers (non-composable) --------------------

/** Fetches a route from the user's current location to a destination and publishes the polyline, ETA, and camera bounds. */
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

/** Animates the camera so both the start and destination are visible within the route bounds. */
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

/** Looks up the campus location matching the search query and, if found, selects it and animates the camera to it. */
private fun selectLocationFromSearch(
    query: String,
    registry: Map<String, List<CampusLocation>>,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope,
    onSelectedLocation: (CampusLocation) -> Unit,
    onClearRoute: () -> Unit
) {
    val target = registry.values.flatten().find { it.name.equals(query, ignoreCase = true) } ?: return
    onSelectedLocation(target)
    onClearRoute()
    coroutineScope.launch {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(target.latLng, 18f),
            durationMs = 1000
        )
    }
}

// -------------------- Map rendering --------------------

/** Renders the GoogleMap layer together with category-colored markers and the active route polyline. */
@Composable
private fun CampusMapLayer(
    mapProperties: MapProperties,
    cameraPositionState: CameraPositionState,
    focusManager: FocusManager,
    markers: List<CampusLocation>,
    polylinePoints: List<LatLng>,
    coroutineScope: CoroutineScope,
    onMarkerSelected: (CampusLocation) -> Unit,
    onRouteVisible: () -> Unit
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        properties = mapProperties,
        cameraPositionState = cameraPositionState,
        onMapClick = { focusManager.clearFocus() },
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false,
            zoomControlsEnabled = true
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
        if (polylinePoints.isNotEmpty()) {
            Polyline(
                points = polylinePoints,
                color = Color(0xFF4285F4),
                width = 12f,
                jointType = JointType.ROUND
            )
            onRouteVisible()
        }
    }
}

/** Renders a single campus-location marker that zooms to and selects the location when tapped. */
@Composable
private fun CampusMarker(
    location: CampusLocation,
    cameraPositionState: CameraPositionState,
    coroutineScope: CoroutineScope,
    onSelected: (CampusLocation) -> Unit
) {
    val markerState = remember(location.name) { MarkerState(position = location.latLng) }
    Marker(
        state = markerState,
        title = location.name,
        snippet = "Category: ${location.category.lowercase()}",
        icon = BitmapDescriptorFactory.defaultMarker(markerHueFor(location.category)),
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

/** Returns the default-marker hue used for a given campus category. */
private fun markerHueFor(category: String): Float = when (category) {
    "ACADEMIC" -> BitmapDescriptorFactory.HUE_RED
    "COMMUTER_PARKING" -> BitmapDescriptorFactory.HUE_AZURE
    "DINING" -> BitmapDescriptorFactory.HUE_VIOLET
    else -> BitmapDescriptorFactory.HUE_VIOLET
}

// -------------------- Overlay: center-self button --------------------

/** Floating button that smoothly re-centers the camera on the user's current position. */
@Composable
fun CenterSelf(
    user: LatLng,
    cameraPositionState: CameraPositionState
) {
    val coroutineScope = rememberCoroutineScope()
    Box {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFFEEEEEE).copy(alpha = 0.9f),
            shadowElevation = 0.dp,
            modifier = Modifier.size(40.dp)
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(user, 18f),
                            durationMs = 1000
                        )
                    }
                }
            ) {
                Icon(Icons.Default.FilterCenterFocus, contentDescription = "Center Self", tint = Color.Gray)
            }
        }
    }
}

// -------------------- Filter menu --------------------

/** Round filter button with a dropdown to pick "all" or a specific campus-location category. */
@Composable
fun FilterMenu(
    categories: Set<String>,
    onFilterChange: (String?) -> Unit
) {
    val categoryIcons = categoryIconMap()
    var filterExpanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.size(56.dp)
        ) {
            IconButton(onClick = { filterExpanded = true }) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        DropdownMenu(
            expanded = filterExpanded,
            onDismissRequest = { filterExpanded = false },
            offset = DpOffset(x = 0.dp, y = 12.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DropdownMenuItem(
                text = { Text("All Locations") },
                leadingIcon = { Icon(Icons.Default.Place, "All locations") },
                onClick = {
                    onFilterChange(null)
                    filterExpanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.replace("_", " ").capitalizeWords()) },
                    onClick = {
                        onFilterChange(category)
                        filterExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = categoryIcons[category] ?: Icons.Default.Place,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

/** Returns the icon used to represent each known campus category in the filter menu. */
private fun categoryIconMap(): Map<String, ImageVector> = mapOf(
    "academic" to Icons.Default.School,
    "commuter_parking" to Icons.Default.DirectionsCar,
    "dining" to Icons.Default.Restaurant
)

/** Title-cases each underscore-separated word (e.g. "commuter_parking" -> "Commuter Parking"). */
fun String.capitalizeWords(): String =
    this.split("_")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }

// -------------------- Search bar --------------------

/** Rounded search bar with an inline dropdown of matching location names. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchLocationBar(
    textFieldState: TextFieldState,
    searchResults: List<String>,
    onSearch: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val uniqueResults = remember(searchResults) { dedupeSearchResults(searchResults) }
    val showDropdown = isFocused && textFieldState.text.isNotEmpty() && uniqueResults.isNotEmpty()

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(bottom = 8.dp)
    ) {
        Column {
            SearchInputField(
                textFieldState = textFieldState,
                onFocusChanged = { isFocused = it },
                onSubmit = {
                    onSearch(textFieldState.text.toString())
                    isFocused = false
                    focusManager.clearFocus()
                }
            )

            if (showDropdown) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                uniqueResults.take(5).forEach { result ->
                    SearchSuggestionItem(
                        label = result,
                        onClick = {
                            onSearch(result)
                            isFocused = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}

/** Trims, blank-filters, and case-insensitively dedupes a list of search result names. */
private fun dedupeSearchResults(results: List<String>): List<String> =
    results
        .map { it.trim() }
        .distinctBy { it.lowercase() }
        .filter { it.isNotBlank() }

/** Transparent single-line TextField used as the search input. */
@Composable
private fun SearchInputField(
    textFieldState: TextFieldState,
    onFocusChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    TextField(
        value = textFieldState.text.toString(),
        onValueChange = { textFieldState.edit { replace(0, length, it) } },
        placeholder = { Text("Search", color = Color.Gray) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChanged(it.isFocused) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() })
    )
}

/** One tappable search-suggestion row. */
@Composable
private fun SearchSuggestionItem(
    label: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
}

// -------------------- Top controls row --------------------

/** Top-of-screen row that combines the filter menu, the search bar, and the directions popup. */
@Composable
fun MapControls(
    campusRegistry: Map<String, List<CampusLocation>>,
    searchState: SearchState,
    onFilterChange: (String?) -> Unit,
    selectedLocation: CampusLocation?,
    onDismissPopup: () -> Unit,
    onModeSelected: (TravelMode) -> Unit,
    routeDurations: RouteDurations
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        FilterMenu(
            categories = campusRegistry.keys,
            onFilterChange = onFilterChange
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(Modifier.weight(1f)) {
            SearchLocationBar(
                textFieldState = searchState.textFieldState,
                searchResults = searchState.results,
                onSearch = searchState.onSearch
            )
        }

        DirectionsPopup(
            location = selectedLocation,
            onDismiss = onDismissPopup,
            onModeSelected = onModeSelected,
            routeDurations = routeDurations
        )
    }
}

// -------------------- Location filtering --------------------

/** Returns every location, alphabetically sorted, that matches the given category filter and search query. */
fun getFilteredLocations(
    fullRegistry: Map<String, List<CampusLocation>>,
    selectedCategory: String?, // null means "all"
    searchQuery: String
): List<CampusLocation> {
    val allLocations = fullRegistry.values.flatten()
    return allLocations
        .filter { matchesFilter(it, selectedCategory, searchQuery) }
        .sortedBy { it.name }
}

/** Returns true when a location matches both the category filter and the search query. */
private fun matchesFilter(
    location: CampusLocation,
    selectedCategory: String?,
    searchQuery: String
): Boolean {
    val matchesCategory = selectedCategory == null || location.category.equals(selectedCategory, ignoreCase = true)
    val matchesSearch = searchQuery.isEmpty() || location.name.contains(searchQuery, ignoreCase = true)
    return matchesCategory && matchesSearch
}

// -------------------- Directions API --------------------

/** Fetches a directions result for the given travel mode; returns a fallback straight-line result on error. */
suspend fun fetchDirections(
    start: LatLng,
    end: LatLng,
    context: Context,
    mode: TravelMode
): DirectionsResult = withContext(Dispatchers.IO) {
    try {
        val apiKey = readGoogleMapsApiKey(context)
        val geoContext = GeoApiContext.Builder().apiKey(apiKey).build()
        val route = requestDirectionsRoute(geoContext, start, end, mode)
        buildDirectionsResult(route, start, end, mode)
    } catch (e: Exception) {
        e.printStackTrace()
        DirectionsResult(listOf(start, end), "", TravelMode.WALKING, "")
    }
}

/** Reads the Google Maps API key from the app's AndroidManifest meta-data. */
private fun readGoogleMapsApiKey(context: Context): String? {
    val ai = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA
    )
    return ai.metaData.getString("com.google.android.geo.API_KEY")
}

/** Makes a blocking Directions API request for a single origin/destination pair in the given mode. */
private fun requestDirectionsRoute(
    geoContext: GeoApiContext,
    start: LatLng,
    end: LatLng,
    mode: TravelMode
): DirectionsRoute? {
    val result = DirectionsApi.newRequest(geoContext)
        .mode(mode)
        .origin(com.google.maps.model.LatLng(start.latitude, start.longitude))
        .destination(com.google.maps.model.LatLng(end.latitude, end.longitude))
        .await()
    return result.routes.getOrNull(0)
}

/** Converts a raw Directions API route into the app's DirectionsResult (polyline, duration, ETA). */
private fun buildDirectionsResult(
    route: DirectionsRoute?,
    start: LatLng,
    end: LatLng,
    mode: TravelMode
): DirectionsResult {
    val leg = route?.legs?.getOrNull(0)
    val duration = leg?.duration?.humanReadable ?: ""
    val points = route?.overviewPolyline?.decodePath()?.map { LatLng(it.lat, it.lng) }
        ?: listOf(start, end)
    val etaMillis = System.currentTimeMillis() + (leg?.duration?.inSeconds ?: 0) * 1000
    val eta = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        .format(java.util.Date(etaMillis))
    return DirectionsResult(points, duration, mode, eta)
}

// -------------------- Directions popup --------------------

/** Bottom sheet that shows the selected location and lets the user pick a travel mode to route in. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DirectionsPopup(
    location: CampusLocation?,
    onDismiss: () -> Unit,
    onModeSelected: (TravelMode) -> Unit,
    routeDurations: RouteDurations
) {
    val sheetState = rememberModalBottomSheetState()
    if (location == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color(0xFF1C1B1F),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 30.dp)
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            LocationHeader(name = location.name)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = location.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))

            TravelModeRow(
                icon = Icons.Default.DirectionsCar,
                label = "Driving",
                duration = routeDurations.driving,
                onClick = {
                    onModeSelected(TravelMode.DRIVING)
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            TravelModeRow(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = "Walking",
                duration = routeDurations.walking,
                onClick = {
                    onModeSelected(TravelMode.WALKING)
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            TravelModeRow(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                label = "Biking",
                duration = routeDurations.biking,
                onClick = {
                    onModeSelected(TravelMode.BICYCLING)
                    onDismiss()
                }
            )
        }
    }
}

/** Title row showing a school icon next to the selected location's name. */
@Composable
private fun LocationHeader(name: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.School,
            contentDescription = null,
            modifier = Modifier.padding(8.dp).size(25.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )
    }
}

/** Single row in the directions popup: icon + clickable label + trailing duration. */
@Composable
private fun TravelModeRow(
    icon: ImageVector,
    label: String,
    duration: String,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFD0BCFF),
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onClick) {
            Text(label, color = Color.White, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = duration, style = MaterialTheme.typography.bodyLarge)
    }
}

// -------------------- Remove-route card --------------------

/** Bottom card summarizing the active route with a cancel button to clear it. */
@Composable
fun RemoveRoute(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    routeDurations: RouteDurations,
    location: CampusLocation? = null,
    travelMode: TravelMode,
    eta: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier.wrapContentWidth(),
    ) {
        Row(
            modifier = Modifier.padding(
                start = 8.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RouteCancelButton(onDismiss = onDismiss)
            Spacer(Modifier.width(8.dp))
            RouteSummary(
                travelMode = travelMode,
                durationText = durationFor(travelMode, routeDurations),
                locationName = location?.name,
                eta = eta
            )
        }
    }
}

/** Round "cancel route" button displayed on the left of the remove-route card. */
@Composable
private fun RouteCancelButton(onDismiss: () -> Unit) {
    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = Icons.Outlined.Cancel,
            contentDescription = "Cancel",
            modifier = Modifier.size(32.dp),
            tint = Color.Gray
        )
    }
}

/** Two-line route summary: icon + duration on top, destination and ETA on the bottom. */
@Composable
private fun RouteSummary(
    travelMode: TravelMode,
    durationText: String,
    locationName: String?,
    eta: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = travelModeIcon(travelMode),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.Gray
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = durationText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.Gray
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = buildSummaryLine(locationName, eta),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

/** Joins the destination name and ETA into a single bullet-separated line, skipping missing pieces. */
private fun buildSummaryLine(locationName: String?, eta: String): String =
    listOfNotNull(
        locationName?.takeIf { it.isNotBlank() },
        "ETA $eta".takeIf { eta.isNotBlank() }
    ).joinToString("  •  ")

/** Returns the duration string that matches the currently-selected travel mode. */
private fun durationFor(mode: TravelMode, durations: RouteDurations): String = when (mode) {
    TravelMode.DRIVING -> durations.driving
    TravelMode.BICYCLING -> durations.biking
    TravelMode.WALKING -> durations.walking
    else -> durations.walking
}

/** Returns the appropriate icon for a given Google Maps TravelMode. */
private fun travelModeIcon(mode: TravelMode): ImageVector = when (mode) {
    TravelMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    TravelMode.DRIVING -> Icons.Default.DirectionsCar
    TravelMode.BICYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
    else -> Icons.Default.TravelExplore
}

// -------------------- Previews --------------------

/** Design-time preview of the remove-route card on a dark background. */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DirectionsPopupPreview() {
    CINetTheme {
        /*DirectionsPopup(
            location = CampusLocation("Aliso Hall", "ACADEMIC"),
            onDismiss = {},
            onModeSelected = {},
            routeDurations = RouteDurations(
                driving = "1 mins",
                walking = "2 mins",
                biking = "3 mins"
            )
        )*/
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1C1B1F)),
            contentAlignment = Alignment.BottomCenter
        ) {
            RemoveRoute(
                onDismiss = {},
                routeDurations = RouteDurations(
                    driving = "1 mins",
                    walking = "2 mins",
                    biking = "3 mins"
                ),
                location = CampusLocation("Aliso Hall", "ACADEMIC"),
                travelMode = TravelMode.DRIVING,
                eta = "12:00AM"
            )
        }
    }
}