package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun EventsSection(
    selectedDate: LocalDate?,
    eventsForSelectedDate: List<EventItem>,
    onEventClick: (EventItem) -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))
    Text("Events", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    if (selectedDate == null) {
        Text("Select a date to view events.")
    } else if (eventsForSelectedDate.isEmpty()) {
        Text("No events for $selectedDate")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            eventsForSelectedDate.forEach { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEventClick(event) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = event.name, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = event.time)
                        if (event.location.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = event.location, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}