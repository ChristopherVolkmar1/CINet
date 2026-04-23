package com.example.cinet.feature.calendar.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.calendar.event.EventItem
import java.time.LocalDate

/** Shows the schedule section for the selected day, including assignments and reminder-enabled campus events. */
@Composable
fun ScheduleSection(
    selectedDate: LocalDate?,
    itemsForSelectedDate: List<ScheduleItem>,
    reminderEventsForSelectedDate: List<EventItem>,
    onItemClick: (ScheduleItem) -> Unit,
    onReminderClick: (EventItem) -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))
    Text("Schedule", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    when {
        selectedDate == null -> Text("Select a date to view scheduled items.")
        hasNoScheduleContent(itemsForSelectedDate, reminderEventsForSelectedDate) -> {
            Text("No items scheduled for $selectedDate")
        }
        else -> ScheduleContent(
            itemsForSelectedDate = itemsForSelectedDate,
            reminderEventsForSelectedDate = reminderEventsForSelectedDate,
            onItemClick = onItemClick,
            onReminderClick = onReminderClick
        )
    }
}

/** Returns true when the selected day has neither assignment items nor reminder items. */
private fun hasNoScheduleContent(
    itemsForSelectedDate: List<ScheduleItem>,
    reminderEventsForSelectedDate: List<EventItem>
): Boolean {
    return itemsForSelectedDate.isEmpty() && reminderEventsForSelectedDate.isEmpty()
}

/** Shows both assignment cards and reminder cards for the selected day. */
@Composable
private fun ScheduleContent(
    itemsForSelectedDate: List<ScheduleItem>,
    reminderEventsForSelectedDate: List<EventItem>,
    onItemClick: (ScheduleItem) -> Unit,
    onReminderClick: (EventItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AssignmentScheduleList(itemsForSelectedDate, onItemClick)
        CampusReminderScheduleList(reminderEventsForSelectedDate, onReminderClick)
    }
}

/** Shows each assignment card already stored in the schedule section. */
@Composable
private fun AssignmentScheduleList(
    itemsForSelectedDate: List<ScheduleItem>,
    onItemClick: (ScheduleItem) -> Unit
) {
    itemsForSelectedDate.forEach { item ->
        AssignmentScheduleCard(item = item, onItemClick = onItemClick)
    }
}

/** Shows one assignment card and delegates click handling upward. */
@Composable
private fun AssignmentScheduleCard(
    item: ScheduleItem,
    onItemClick: (ScheduleItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(item) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.className,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.assignmentName)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Due: ${item.dueTime}")
        }
    }
}

/** Shows each reminder-enabled campus event in the schedule section. */
@Composable
private fun CampusReminderScheduleList(
    reminderEventsForSelectedDate: List<EventItem>,
    onReminderClick: (EventItem) -> Unit
) {
    reminderEventsForSelectedDate.forEach { event ->
        CampusReminderScheduleCard(event = event, onReminderClick = onReminderClick)
    }
}

/** Shows one reminder card for a campus event that has notifications turned on. */
@Composable
private fun CampusReminderScheduleCard(
    event: EventItem,
    onReminderClick: (EventItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReminderClick(event) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = event.name,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = campusEventReminderScheduleText(event),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Event: ${event.time}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
