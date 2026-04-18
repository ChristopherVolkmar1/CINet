package com.example.cinet

import android.R.attr.onClick
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cinet.com.example.cinet.data.model.CampusRegistry
import com.example.cinet.ui.theme.CINetTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.async

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
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasPermission by remember { mutableStateOf(PermissionManager.hasAllPermissions(context)) }
    // Map style now follows the global isDarkMode setting
    val mapStyle: MapStyleOptions? = remember(AppSettings.isDarkMode) {
        if (AppSettings.isDarkMode) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) as MapStyleOptions?
        } else {
            null
        }
    }
    val mapProperties by remember(hasPermission, mapStyle) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasPermission,
                mapStyleOptions = mapStyle
            )
        )
    }
    androidx.activity.compose.BackHandler {
        onBack()
    }

    val campusRegistry by viewModel.campusRegistry.collectAsState()
    var selectedLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var routeLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var activeFilter by remember { mutableStateOf<String?>(null) }

    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var durations by remember { mutableStateOf(RouteDurations())}
    var activeTravelMode by remember { mutableStateOf(TravelMode.WALKING) }
    var showRemoveRoute by remember { mutableStateOf(false) }
    var eta by remember { mutableStateOf("") }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(34.162, -119.043), 16f)
    }

    val filteredNames = remember(textFieldState.text, activeFilter, campusRegistry) {
        getFilteredLocations(campusRegistry, activeFilter, textFieldState.text.toString())
            .map { it.name }
    }

    val markersToDraw = remember(activeFilter, textFieldState.text, campusRegistry, selectedLocation, showRemoveRoute) {
        val filtered = getFilteredLocations(campusRegistry, activeFilter, textFieldState.text.toString())
        val current = selectedLocation
        if (showRemoveRoute) {
            filtered.filter { it.name == routeLocation?.name }
        } else if (current != null) {
            filtered.filter { it.name == current.name }
        } else {
            filtered
        }
    }

    val focusManager = LocalFocusManager.current
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }
    val requestRoute = { mode: TravelMode ->
        activeTravelMode = mode
        val destination = selectedLocation?.latLng
        if (destination != null && hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        userLatLng = LatLng(it.latitude, it.longitude)
                        coroutineScope.launch {
                            val result = fetchDirections(LatLng(it.latitude, it.longitude), destination, context, mode)
                            eta = result.eta
                            polylinePoints = result.points

                            val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                                .include(LatLng(it.latitude, it.longitude))
                                .include(destination)
                                .build()
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngBounds(bounds, 100),
                                durationMs = 1000
                            )
                        }
                    }
                }
            } catch (_: SecurityException) { Log.e("Route", "No Permission") }
        }
    }
    val searchState = SearchState(
        textFieldState = textFieldState,
        results = filteredNames,
        onSearch = { query ->
            val target = campusRegistry.values.flatten().find {
                it.name.equals(query, ignoreCase = true)
            }
            target?.let { location ->
                selectedLocation = location
                polylinePoints = emptyList()
                showRemoveRoute = false
                routeLocation = null
                coroutineScope.launch {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(location.latLng, 18f),
                        durationMs = 1000
                    )
                }
            }
            textFieldState.edit { replace(0, length, "") }
        }
    )

    LaunchedEffect(Unit) {
        textFieldState.edit { replace(0, length, "") }
        polylinePoints = emptyList()
        selectedLocation = null

        if (hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    userLatLng = if (location != null) {
                        LatLng(location.latitude, location.longitude)
                    } else {
                        LatLng(34.162, -119.043)
                    }
                }
            } catch (_: SecurityException) {
                Log.e("Location", "Permission missing")
            }
        } else {
            userLatLng = LatLng(34.162, -119.043)
        }
    }
    LaunchedEffect(preSelectedLocation) {
        if (preSelectedLocation == null ||  preSelectedLocation.coordinates.latitude == 0.0) return@LaunchedEffect
        selectedLocation = preSelectedLocation
        cameraPositionState.move(
            update = CameraUpdateFactory.newLatLngZoom(preSelectedLocation.latLng, 18f)
        )
        onFinishedLoading()
    }
    LaunchedEffect(selectedLocation, userLatLng) {
        val destination = selectedLocation?.latLng ?: return@LaunchedEffect
        val start = userLatLng ?: return@LaunchedEffect
        if (hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    coroutineScope.launch {
                        val drive = async { fetchDirections(
                            start,
                            destination,
                            context,
                            TravelMode.DRIVING
                        ) }
                        val walk = async {fetchDirections(
                            start,
                            destination,
                            context,
                            TravelMode.WALKING
                        ) }
                        val bike = async { fetchDirections(
                            start,
                            destination,
                            context,
                            TravelMode.BICYCLING
                        ) }

                        val driving = drive.await()
                        val walking = walk.await()
                        val biking = bike.await()

                        durations = durations.copy(
                            driving = driving.duration,
                            walking = walking.duration,
                            biking = biking.duration
                        )
                        routeLocation = selectedLocation
                    }

                }
            } catch (_: SecurityException) {
                Log.e("Location", "Permission missing")
            }
        }
    }
    DisposableEffect(hasPermission) {
        if (!hasPermission) return@DisposableEffect onDispose {}

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            2000L // update every 2 seconds
        ).build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let {
                    userLatLng = LatLng(it.latitude, it.longitude)
                }
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

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    Box {
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
            markersToDraw.forEach { location ->
                val markerState = remember(location.name) {
                    MarkerState(position = location.latLng)
                }
                Marker(
                    state = markerState,
                    title = location.name,
                    snippet = "Category: ${location.category.lowercase()}",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when (location.category) {
                            "ACADEMIC" -> BitmapDescriptorFactory.HUE_RED
                            "COMMUTER_PARKING" -> BitmapDescriptorFactory.HUE_AZURE
                            "DINING" -> BitmapDescriptorFactory.HUE_VIOLET
                            else -> BitmapDescriptorFactory.HUE_VIOLET
                        }
                    ),
                    onClick = {
                        selectedLocation = location
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
            if (polylinePoints.isNotEmpty()) {
                Polyline(
                    points = polylinePoints,
                    color = Color(0xFF4285F4),
                    width = 12f,
                    jointType = JointType.ROUND
                )
                showRemoveRoute = true
            }
        }

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
                CenterSelf(
                    user = user,
                    cameraPositionState = cameraPositionState
                )
            }
        }
    }
}

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
            ) { Icon(Icons.Default.FilterCenterFocus, contentDescription = "Center Self", tint = Color.Gray) }
        }
    }
}

@Composable
fun FilterMenu(
    categories: Set<String>,
    onFilterChange: (String?) -> Unit
) {
    val categoryIcons = mapOf(
        "academic" to Icons.Default.School,
        "commuter_parking" to Icons.Default.DirectionsCar,
        "dining" to Icons.Default.Restaurant,
    )
    var filterExpanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.size(56.dp)
        ) {
            IconButton(
                onClick = { filterExpanded = true }
            ) { Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.onSurface) }
        }
        DropdownMenu(
            expanded = filterExpanded,
            onDismissRequest = { filterExpanded = false},
            offset = DpOffset(x = 0.dp, y = 12.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DropdownMenuItem(
                text = { Text("All Locations") },
                leadingIcon = {Icon(Icons.Default.Place, "All locations")},
                onClick = { onFilterChange(null); filterExpanded = false }
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
fun String.capitalizeWords(): String =
    this.split("_")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchLocationBar(
    textFieldState: TextFieldState,
    searchResults: List<String>,
    onSearch: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val uniqueResults = remember(searchResults) {
        searchResults
            .map { it.trim() }
            .distinctBy { it.lowercase() }
            .filter { it.isNotBlank() }
    }

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
                    .onFocusChanged { isFocused = it.isFocused },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(textFieldState.text.toString())
                        isFocused = false
                        focusManager.clearFocus()
                    }
                )
            )

            if (showDropdown) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                uniqueResults.take(5).forEach { result ->
                    ListItem(
                        headlineContent = { Text(result, color = MaterialTheme.colorScheme.onSurface) },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
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

fun getFilteredLocations(
    fullRegistry: Map<String, List<CampusLocation>>,
    selectedCategory: String?, // null means "all"
    searchQuery: String
): List<CampusLocation> {
    val allLocations = fullRegistry.values.flatten()
    return allLocations.filter { location ->
        val matchesCategory = selectedCategory == null || location.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isEmpty() || location.name.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }.sortedBy { it.name }
}

suspend fun fetchDirections(
    start: LatLng,
    end: LatLng,
    context: Context,
    mode: TravelMode
): DirectionsResult {
    return withContext(Dispatchers.IO) {
        try {
            val ai = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val bundle = ai.metaData
            val apiKey = bundle.getString("com.google.android.geo.API_KEY")

            val geoContext = GeoApiContext.Builder()
                .apiKey(apiKey)
                .build()

            val result = DirectionsApi.newRequest(geoContext)
                .mode(mode)
                .origin(com.google.maps.model.LatLng(start.latitude, start.longitude))
                .destination(com.google.maps.model.LatLng(end.latitude, end.longitude))
                .await()

            val route = result.routes.getOrNull(0)
            val duration = route?.legs?.getOrNull(0)?.duration?.humanReadable ?: ""
            val points = route?.overviewPolyline?.decodePath()?.map {
                LatLng(it.lat, it.lng)
            } ?: listOf(start, end)

            val durationSeconds = route?.legs?.getOrNull(0)?.duration?.inSeconds ?: 0
            val etaMillis = System.currentTimeMillis() + durationSeconds * 1000
            val eta = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(etaMillis))

            DirectionsResult(points, duration, mode, eta)
        } catch (e: Exception) {
            e.printStackTrace()
            DirectionsResult(listOf(start, end), "", TravelMode.WALKING, "")
        }
    }
}
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DirectionsPopup(
    location: CampusLocation?,
    onDismiss: () -> Unit,
    onModeSelected: (TravelMode) -> Unit,
    routeDurations: RouteDurations
) {
    val sheetState = rememberModalBottomSheetState()

    // Only show if location is not null
    if (location != null) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(25.dp)
                    )
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = location.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onModeSelected(TravelMode.DRIVING)
                            onDismiss()
                        }
                    ) {
                        Text("Driving", color = Color.White, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = routeDurations.driving,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onModeSelected(TravelMode.WALKING)
                            onDismiss()
                        }
                    ){
                        Text("Walking", color = Color.White, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = routeDurations.walking,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onModeSelected(TravelMode.BICYCLING)
                            onDismiss()
                        }
                    ){
                        Text("Biking", color = Color.White, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = routeDurations.biking,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

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
        modifier = modifier.fillMaxWidth(0.70f),
    ) {
        Box (modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            // Delete route button
            IconButton(
                onClick = { onDismiss() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = "Cancel",
                    modifier = Modifier
                        .size(60.dp),
                    tint = Color.Gray
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(

                        when (travelMode) {
                            TravelMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
                            TravelMode.DRIVING -> Icons.Default.DirectionsCar
                            TravelMode.BICYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
                            else -> Icons.Default.TravelExplore
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(25.dp),
                        tint = Color.Gray
                    )

                    val displayDuration = when (travelMode) {
                        TravelMode.WALKING -> routeDurations.walking
                        TravelMode.DRIVING -> routeDurations.driving
                        TravelMode.BICYCLING -> routeDurations.biking
                        else -> routeDurations.walking
                    }

                    Text(
                        text = displayDuration,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Gray
                    )
                }
                Text(
                    text = "${location?.name} | $eta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
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
}