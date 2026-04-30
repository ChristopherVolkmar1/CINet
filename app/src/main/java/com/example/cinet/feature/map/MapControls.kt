package com.example.cinet.feature.map

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cinet.ui.theme.CINetTheme
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
    onFiltersChanged: (Set<String>) -> Unit,
    activeFilters: Set<String>,
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
            categories = campusRegistry.keys.map { it.uppercase() }.toSet() + "TRANSIT",
            activeFilters = activeFilters,
            onFiltersChanged = onFiltersChanged
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterMenu(
    categories: Set<String>,
    activeFilters: Set<String>,
    onFiltersChanged: (Set<String>) -> Unit
) {
    val categoryIcons = categoryIconMap()
    var showDialog by remember { mutableStateOf(false) }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.size(56.dp)
    ) {
        IconButton(onClick = { showDialog = true }) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = "Filter",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showDialog) {
        BasicAlertDialog(
            onDismissRequest = {showDialog = false}
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.secondary,
                tonalElevation = 6.dp,
            ) {
                Column(
                    Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Filter Categories",
                            modifier = Modifier
                                .padding(16.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        ElevatedButton(
                            onClick = { onFiltersChanged(emptySet()) },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            )
                        ) {
                            Text(
                                text = "Clear",
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                    Spacer(modifier = Modifier.padding(start = 2.dp))
                    categories.forEach { category ->
                        val isSelected = activeFilters.contains(category)
                        Row (
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = categoryIcons[category.uppercase().trim()] ?: Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.padding(start = 6.dp))
                            Text(
                                category.lowercase()
                                    .split("_")
                                    .joinToString(" ") { word ->
                                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                    },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val newFilters = if (checked) {
                                        activeFilters + category
                                    } else {
                                        activeFilters - category
                                    }
                                    onFiltersChanged(newFilters)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}



/** Returns the icon used to represent each known campus category in the filter menu. */
private fun categoryIconMap(): Map<String, ImageVector> = mapOf(
    "ACADEMIC" to Icons.Default.School,
    "COMMUTER_PARKING" to Icons.Default.LocalParking,
    "DINING" to Icons.Default.Restaurant,
    "TRANSIT" to Icons.Default.DirectionsBus
)

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

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewFilter() {
    CINetTheme(darkTheme = true) {
        val sampleCategories = setOf("ACADEMIC", "DINING", "TRANSIT", "COMMUTER_PARKING")
        var activeFilters by remember { mutableStateOf(setOf("ACADEMIC")) }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            FilterMenu(
                categories = sampleCategories,
                activeFilters = activeFilters,
                onFiltersChanged = { activeFilters = it }
            )
        }
    }
}
