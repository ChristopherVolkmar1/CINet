package com.example.cinet.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.model.TravelMode
import kotlinx.coroutines.launch

// Top-of-screen overlay row combining the category filter menu, location
// search bar, and directions popup. Also contains the center-self button.
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
    routeDurations: RouteDurations,
    onShowBusSchedule: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        FilterMenu(
            categories = campusRegistry.keys + "transit",
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
            routeDurations = routeDurations,
            onShowBusSchedule = onShowBusSchedule
        )
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
    "dining" to Icons.Default.Restaurant,
    "transit" to Icons.Default.DirectionsBus
)

/** Title-cases each underscore-separated word (e.g. "commuter_parking" -> "Commuter Parking"). */
fun String.capitalizeWords(): String =
    this.split("_")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
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

