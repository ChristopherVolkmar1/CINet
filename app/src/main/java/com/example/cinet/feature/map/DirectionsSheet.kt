package com.example.cinet.feature.map

import android.R
import android.R.attr.onClick
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.cinet.core.designsystem.ListItem
import com.example.cinet.ui.theme.CINetTheme
import com.google.maps.model.TravelMode

// Bottom sheet that displays a selected campus location and lets the user
// pick a travel mode (driving, walking, biking) to generate a route.
// -------------------- Data classes --------------------
data class RouteDurations(
    var driving: String = "",
    var walking: String = "",
    var biking: String = ""
)

// -------------------- Directions popup --------------------

/** Bottom sheet that shows the selected location and lets the user pick a travel mode to route in. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DirectionsPopup(
    location: CampusLocation?,
    onDismiss: () -> Unit,
    onModeSelected: (TravelMode) -> Unit,
    routeDurations: RouteDurations,
    onShowBusSchedule: () -> Unit = {}
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

            if (location.category == "TRANSIT") {
                TextButton(onClick = onShowBusSchedule) {
                    Text("View Bus Schedule", color = Color(0xFFD0BCFF))
                }
            } else {
                if (location.description.isNotBlank()) {
                    Text(
                        text = location.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
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

/** Bus Information*/
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BusScheduleSheet(onDismiss: () -> Unit) {
    val arrival = listOf<String>("7:25am", "8:45am", "10:05am", "11:25am", "1:00pm", "2:20pm", "3:40pm", "5:00pm")
    val departure = listOf<String>("7:50am", "9:10am", "10:30am", "11:50am", "1:25pm", "2:45pm", "4:05pm", "5:30pm")
    var selectedTab by remember { mutableIntStateOf(0) }
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(Alignment.CenterVertically),
        onDismissRequest = onDismiss,
        title = {
            Text("Route 99 - CSUCI",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center)
                },
        text = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f),horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Arriving", style = MaterialTheme.typography.labelLarge)
                    HorizontalDivider()
                    arrival.forEach { time ->
                        Text(text = time, modifier = Modifier.padding(vertical = 8.dp))
                        HorizontalDivider()
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Departing", style = MaterialTheme.typography.labelLarge)
                    HorizontalDivider()
                    departure.forEach { time ->
                        Text(text = time, modifier = Modifier.padding(vertical = 8.dp))
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Preview() {
    CINetTheme {
        /*DirectionsPopup(
            location = CampusLocation("CSUCI Transit Stop", "TRANSIT"),
            onDismiss = {},
            onModeSelected = {},
            routeDurations = RouteDurations(
                driving = "1 mins",
                walking = "2 mins",
                biking = "3 mins"
            )
        )*/
        BusScheduleSheet(onDismiss = {})
    }
}