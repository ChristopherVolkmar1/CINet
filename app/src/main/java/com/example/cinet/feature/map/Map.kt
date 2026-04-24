package com.example.cinet.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
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
import com.example.cinet.feature.settings.AppSettings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.google.maps.model.TravelMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


// Main campus map screen. Wires together state, side-effects, permissions,
// user location tracking, and all map overlay composables.
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
    var showBusSheet by remember { mutableStateOf(false) }

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

        Box(modifier = Modifier
            .fillMaxSize()
            .align(Alignment.BottomCenter)) {
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
            routeDurations = durations,
            onShowBusSchedule = { showBusSheet = true }
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

        if (showBusSheet) {
            BusScheduleSheet(onDismiss = { showBusSheet = false })
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
        // This is where the route gets deleted when the map is reloaded, delete the comment to have it be removed again
        //onPolylinePoints(emptyList())
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
    val context = LocalContext.current
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val customIcon = remember(location.category) {
        val resId = when (location.category) {
            "ACADEMIC" -> R.drawable.school
            "TRANSIT" -> R.drawable.bus_stop
            "COMMUTER_PARKING" -> R.drawable.parking
            "DINING" -> R.drawable.dining
            else -> R.drawable.unlisted
        }
        customMarker(
            context = context,
            iconResId = resId,
            backgroundColor = secondaryColor
        )
    }
    val markerState = remember(location.name) { MarkerState(position = location.latLng) }
    Marker(
        state = markerState,
        title = location.name,
        snippet = "Category: ${location.category.lowercase()}",
        icon = customIcon,
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

/** Returns the default-marker hue used for a given campus category OR desired icon*/
private fun markerHueFor(category: String): Float = when (category) {
    "ACADEMIC" -> BitmapDescriptorFactory.HUE_RED
    "COMMUTER_PARKING" -> BitmapDescriptorFactory.HUE_AZURE
    "DINING" -> BitmapDescriptorFactory.HUE_VIOLET
    "TRANSIT" -> BitmapDescriptorFactory.HUE_ROSE
    else -> BitmapDescriptorFactory.HUE_VIOLET
}
fun customMarker(context: Context, iconResId: Int, backgroundColor: Color): BitmapDescriptor? {
    val pinDrawable = ContextCompat.getDrawable(context, R.drawable.pin)
    val iconDrawable = ContextCompat.getDrawable(context, iconResId)
    val size = 105
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    pinDrawable?.let {
        it.setTint(backgroundColor.toArgb())
        it.setBounds(0, 0, size, size)
        it.draw(canvas)
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

private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    return ContextCompat.getDrawable(context, vectorResId)?.run {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

// -------------------- Previews --------------------

/** Design-time preview of the remove-route card on a dark background. */
/*@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DirectionsPopupPreview() {
    CINetTheme {
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
}*/