package com.example.cinet.feature.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.maps.model.TravelMode

// Bottom card shown while a route is active. Displays the travel mode icon,
// duration, destination, and ETA with a cancel button to clear the route.
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