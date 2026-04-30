package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Shows the event section for the currently selected calendar date. */
@Composable
fun EventsSection(
    selectedDate: LocalDate?,
    eventsForSelectedDate: List<EventItem>,
    onEventClick: (EventItem) -> Unit,
    title: String = "Events",
    emptyMessage: String = "No events for $selectedDate"
) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    when {
        selectedDate == null -> Text("Select a date to view events.")
        eventsForSelectedDate.isEmpty() -> Text(emptyMessage)
        else -> EventList(eventsForSelectedDate, onEventClick)
    }
}

/** Shows the list of event cards for the selected date. */
@Composable
private fun EventList(
    events: List<EventItem>,
    onEventClick: (EventItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.forEach { event ->
            EventCard(
                event = event,
                onClick = { onEventClick(event) }
            )
        }
    }
}

/** Shows one event card in the daily event list. */
@Composable
private fun EventCard(
    event: EventItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            EventCardHeader(event)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = event.time)
            EventLocation(event.location)
        }
    }
}

/** Shows the title row and optional campus badge for one event card. */
@Composable
private fun EventCardHeader(event: EventItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EventTitle(
            title = event.name,
            modifier = Modifier.weight(1f)
        )

        if (event.isCampusEvent) {
            Spacer(modifier = Modifier.width(8.dp))
            CampusBadge()
        }
    }
}

/** Shows the event title and lets it wrap cleanly across multiple lines. */
@Composable
private fun EventTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

/** Shows the campus source badge aligned cleanly within the event card header. */
@Composable
private fun CampusBadge() {
    Box(contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "Campus",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** Shows the optional event location text. */
@Composable
private fun EventLocation(location: String) {
    if (location.isBlank()) {
        return
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = location,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
