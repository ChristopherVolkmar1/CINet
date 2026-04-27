package com.example.cinet.feature.map

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.cinet.ui.theme.CINetTheme
import com.google.maps.model.TravelMode
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

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
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 30.dp)
        ) {
            Spacer(modifier = Modifier.width(2.dp))
            LocationHeader(location, onShowBusSchedule)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Spacer(modifier = Modifier.height(24.dp))

            Row {
                TravelModeButton(
                    icon = Icons.Default.DirectionsCar,
                    duration = routeDurations.driving,
                    onClick = {
                        onModeSelected(TravelMode.DRIVING)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                TravelModeButton(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    duration = routeDurations.walking,
                    onClick = {
                        onModeSelected(TravelMode.WALKING)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                TravelModeButton(
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    duration = routeDurations.biking,
                    onClick = {
                        onModeSelected(TravelMode.BICYCLING)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Spacer(modifier = Modifier.height(16.dp))

            QuickInfo(location)

            Spacer(modifier = Modifier.height(16.dp))
            UpcomingClass()
        }
    }
}

/** Title row showing a school icon next to the selected location's name. */
private fun categoryIcon(category: String): ImageVector = when (category) {
    "ACADEMIC" -> Icons.Default.School
    "COMMUTER_PARKING" -> Icons.Default.LocalParking
    "DINING" -> Icons.Default.Dining
    "TRANSIT" -> Icons.Default.DirectionsTransit
    else -> Icons.Default.School
}

fun isOpen(hours: DayHours?): Boolean {
    if (hours == null || hours.isClosed) return false
    val current = LocalTime.now()
    val currentTime = (current.hour * 100) + current.minute
    return currentTime >= hours.open && currentTime < hours.close
}

fun getStatusForLocation(weeklyHours: WeeklyHours?): String {
    if (weeklyHours == null) return "UNKNOWN"
    val dayOfWeek = LocalDate.now().dayOfWeek.name.lowercase()

    val todayHours = when (dayOfWeek) {
        "monday" -> weeklyHours.monday
        "tuesday" -> weeklyHours.tuesday
        "wednesday" -> weeklyHours.wednesday
        "thursday" -> weeklyHours.thursday
        "friday" -> weeklyHours.friday
        "saturday" -> weeklyHours.saturday
        "sunday" -> weeklyHours.sunday
        else -> null
    }

    return if (isOpen(todayHours)) {
        "Open Now"
    } else {
        "Closed"
    }
}
@Composable
private fun StatusBox(status: String) {
    val backgroundColor = when (status) {
        "OPEN" -> MaterialTheme.colorScheme.secondaryContainer
        "CLOSED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(36.dp),
        color = backgroundColor,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .widthIn(min = 50.dp)
            .height(32.dp)
            .offset(y = (-24).dp)
    ) {
        Box (
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = status.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondary,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
    }
}
@Composable
private fun LocationHeader(location: CampusLocation, onShowBusSchedule: () -> Unit = {}) {
    val status = getStatusForLocation(location.hours)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            categoryIcon(location.category),
            contentDescription = null,
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(
                text = location.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondary
            )
            if (location.category == "TRANSIT") {
                TextButton(onClick = onShowBusSchedule) {
                    Text("View Bus Schedule", color = MaterialTheme.colorScheme.onSecondary)
                }
            } else {
                if (location.description.isNotBlank()) {
                    Text(
                        text = location.category.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        StatusBox(status)
    }
}

/** travel type directions popup: icon + clickable label + trailing duration. */
@Composable
private fun TravelModeButton(
    icon: ImageVector,
    duration: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer, // Background
            contentColor = MaterialTheme.colorScheme.onSecondary    // Text/Icon
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 1.dp),
        modifier = modifier.height(48.dp).width(110.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(25.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = duration, style = MaterialTheme.typography.bodyLarge, maxLines = 1, softWrap = false
        )
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

/** Other Information*/

fun convertTime(time: Int): String {
    val hours = time / 100
    val minutes = time % 100
    val amPm = if (hours >= 12) "PM" else "AM"
    val standardHour = when {
        hours == 0 -> 12
        hours > 12 -> hours - 12
        else -> hours
    }
    return String.format(Locale.US, "%d:%02d%s", standardHour, minutes, amPm)
}
@Composable
fun QuickInfo(location: CampusLocation) {
    val displayHours = remember(location.hours) {
        val dayOfWeek = LocalDate.now().dayOfWeek.name.lowercase()
        val today = when (dayOfWeek) {
            "monday" -> location.hours?.monday
            "tuesday" -> location.hours?.tuesday
            "wednesday" -> location.hours?.wednesday
            "thursday" -> location.hours?.thursday
            "friday" -> location.hours?.friday
            "saturday" -> location.hours?.saturday
            "sunday" -> location.hours?.sunday
            else -> null
        }
        when {
            today == null -> "HOURS UNKNOWN"
            today.isClosed || today.open == 0 -> "CLOSED"
            else -> {
                val open = convertTime(today.open)
                val close = convertTime(today.close)
                "$open - $close"
            }
        }
    }
    Column(modifier = Modifier.padding(2.dp)) {
        Text(
            "Quick Info",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Hours of Operation: $displayHours",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = location.description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun UpcomingClass(
    className: String = "UPCOMING CLASS"
) {
    Surface(
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = className.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Preview() {
    CINetTheme(darkTheme = true) {
        DirectionsPopup(
            location = CampusLocation("Aliso Hall", "ACADEMIC", description = "description from firebase"),
            onDismiss = {},
            onModeSelected = {},
            routeDurations = RouteDurations(
                driving = "15 mins",
                walking = "3.5 hrs",
                biking = "33 mins"
            )
        )
        //BusScheduleSheet(onDismiss = {})
    }
}