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
    var activeFilters by remember { mutableStateOf(setOf<String>()) }
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

    val repository = remember { SocialRepository() }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getFriends().onSuccess {
            friends = it
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
            markers = markersToDraw,
            polylinePoints = polylinePoints,
            coroutineScope = coroutineScope,
            onMarkerSelected = { selectedLocation = it },
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
            friends = friends
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
            mapStyleOptions = mapStyle
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
    polylinePoints: List<LatLng>,
    coroutineScope: CoroutineScope,
    onMarkerSelected: (CampusLocation) -> Unit,
    onRouteVisible: () -> Unit,
    mode: TravelMode
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