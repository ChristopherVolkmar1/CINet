package com.example.cinet

import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils.replace
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
<<<<<<< settings-page
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
=======
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.firestore.FirebaseFirestore
>>>>>>> develop
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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

data class CampusLocation(
    val name: String = "",
    val category: String = "",
    val coordinates: GeoPoint = GeoPoint(0.0, 0.0)
) {
    val latLng: LatLng
        get() = LatLng(coordinates.latitude, coordinates.longitude)
}

data class SearchState(
    val textFieldState: TextFieldState,
    val results: List<String>,
    val onSearch: (String) -> Unit
)
@Composable
fun CampusMapScreen(
<<<<<<< settings-page
    /* Settings stuff -Zack 
     * gets if night mode is on or off
     */
    isNightMode: Boolean = false,
    modifier: Modifier = Modifier,
=======
>>>>>>> develop
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val textFieldState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

<<<<<<< settings-page
    val csuciBounds = LatLngBounds(
        LatLng(34.155, -119.055),
        LatLng(34.168, -119.035)
    )

    val permissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    val mapProperties by remember(permissionState.status.isGranted, isNightMode) {
        mutableStateOf(
            MapProperties(
                latLngBoundsForCameraTarget = csuciBounds,
                minZoomPreference = 14f,
                maxZoomPreference = 20f,
                isMyLocationEnabled = permissionState.status.isGranted,
                /* Settings stuff -Zack
                 * makes the map dark if u picked night mode
                 */
                mapStyleOptions = if (isNightMode) {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
                } else {
                    null
                }
=======
    var hasPermission by remember { mutableStateOf(PermissionManager.hasAllPermissions(context)) }
    val mapStyle: MapStyleOptions? = remember(AppSettings.isDarkMap) {
        if (AppSettings.isDarkMap) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) as MapStyleOptions?
        } else {
            null
        }
    }
    val mapProperties by remember(hasPermission) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasPermission,
                mapStyleOptions = mapStyle
>>>>>>> develop
            )
        )
    }
    androidx.activity.compose.BackHandler {
        onBack()
    }

    var campusRegistry by remember { mutableStateOf<Map<String, List<CampusLocation>>>(emptyMap()) }
    var selectedLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var activeFilter by remember { mutableStateOf<String?>(null) }
    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(34.162, -119.043), 16f)
    }

    val filteredNames = remember(textFieldState.text, activeFilter, campusRegistry) {
        getFilteredLocations(campusRegistry, activeFilter, textFieldState.text.toString())
            .map { it.name }
    }

    val markersToDraw = remember(activeFilter, textFieldState.text, campusRegistry, selectedLocation) {
        val filtered = getFilteredLocations(campusRegistry, activeFilter, textFieldState.text.toString())
        val current = selectedLocation
        if (current != null) {
            filtered.filter { it.name == current.name }
        } else {
            filtered
        }
    }

    val focusManager = LocalFocusManager.current
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }
    val requestRoute = { mode: TravelMode ->
        val destination = selectedLocation?.latLng
        if (destination != null && hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        userLatLng = LatLng(it.latitude, it.longitude)
                        coroutineScope.launch {
                            val path = fetchDirections(LatLng(it.latitude, it.longitude), destination, context, mode)
                            polylinePoints = path
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 18f),
                                durationMs = 1000
                            )
                        }
                    }
                }
            } catch (e: SecurityException) { Log.e("Route", "No Permission") }
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
            } catch (e: SecurityException) {
                Log.e("Location", "Permission missing")
            }
        } else {
            userLatLng = LatLng(34.162, -119.043)
        }

        try {
            val collections = listOf("academic", "dining", "commuter_parking")
            val results = coroutineScope {
                collections.map { collectionName ->
                    async(Dispatchers.IO) {
                        val snapshot = db.collection(collectionName).get().await()
                        val list = snapshot.toObjects(CampusLocation::class.java)
                        Log.d("Firestore", "Fetched ${list.size} items from $collectionName")
                        collectionName to list
                    }
                }.awaitAll()
            }
            campusRegistry = results.toMap()
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching data: ${e.message}")
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
        } catch (e: SecurityException) {
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
            }
        }

        MapControls(
            campusRegistry = campusRegistry,
            searchState = searchState,
            onFilterChange = { activeFilter = it },
            selectedLocation = selectedLocation,
            onDismissPopup = { selectedLocation = null },
            onModeSelected = requestRoute
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
            color = Color(0xFFEEEEEE).copy(alpha = 0.8f),
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
            color = Color(0xFF1C1B1F),
            shadowElevation = 4.dp,
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(
                onClick = { filterExpanded = true }
            ) { Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.Gray) }
        }
        DropdownMenu(
            expanded = filterExpanded,
            onDismissRequest = { filterExpanded = false},
            offset = DpOffset(x = 0.dp, y = 12.dp)
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
        color = Color(0xFF1C1B1F),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
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
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
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
                HorizontalDivider(color = Color.DarkGray)
                uniqueResults.take(5).forEach { result ->
                    ListItem(
                        headlineContent = { Text(result, color = Color.White) },
                        colors = ListItemDefaults.colors(
                            containerColor = Color(0xFF1C1B1F)
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
    onModeSelected: (TravelMode) -> Unit
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
            onModeSelected = onModeSelected
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
): List<LatLng> {
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

            result.routes.getOrNull(0)?.overviewPolyline?.decodePath()?.map {
                LatLng(it.lat, it.lng)
            } ?: listOf(start, end)
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(start, end)
        }
    }
}
<<<<<<< settings-page
=======


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DirectionsPopup(
    location: CampusLocation?,
    onDismiss: () -> Unit,
    onModeSelected: (TravelMode) -> Unit
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
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp)
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = location.category.capitalizeWords(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFFD0BCFF))
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onModeSelected(TravelMode.DRIVING)
                            onDismiss()
                        }
                    ) {
                        Text("Driving", color = Color.White)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = Color(0xFFD0BCFF))
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onModeSelected(TravelMode.WALKING)
                            onDismiss()
                        }
                    ){
                        Text("Walking", color = Color.White)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, tint = Color(0xFFD0BCFF))
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onModeSelected(TravelMode.BICYCLING)
                            onDismiss()
                        }
                    ){
                        Text("Biking", color = Color.White)
                    }
                }
            }
        }
    }
}
>>>>>>> develop
