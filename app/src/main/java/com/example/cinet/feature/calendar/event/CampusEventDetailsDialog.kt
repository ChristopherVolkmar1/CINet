package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shows details for one live campus event and lets the user toggle reminders. */
@Composable
fun CampusEventDetailsDialog(
    event: EventItem,
    isReminderEnabled: Boolean,
    onReminderToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.name) },
        text = {
            CampusEventDetailsContent(
                event = event,
                isReminderEnabled = isReminderEnabled,
                onReminderToggle = onReminderToggle
            )
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/** Displays the scrollable body content inside the campus event dialog. */
@Composable
private fun CampusEventDetailsContent(
    event: EventItem,
    isReminderEnabled: Boolean,
    onReminderToggle: (Boolean) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        CampusEventTypeLabel()
        Spacer(modifier = Modifier.height(12.dp))
        EventDetailLine(label = "Date", value = event.date)
        EventDetailLine(label = "Time", value = event.time)
        EventDetailLine(label = "Location", value = event.location.ifBlank { "TBA" })
        EventDescription(event.description)
        Spacer(modifier = Modifier.height(16.dp))
        CampusEventReminderRow(
            event = event,
            isReminderEnabled = isReminderEnabled,
            onReminderToggle = onReminderToggle
        )
    }
}

/** Shows the small campus-event label at the top of the dialog. */
@Composable
private fun CampusEventTypeLabel() {
    Text(
        text = "Campus event",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

/** Shows the optional event description block when the feed includes one. */
@Composable
private fun EventDescription(description: String) {
    if (description.isBlank()) {
        return
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text("Description", style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(4.dp))
    CampusEventLinkText(description)
}

/** Shows the reminder row with the helper text and toggle switch. */
@Composable
private fun CampusEventReminderRow(
    event: EventItem,
    isReminderEnabled: Boolean,
    onReminderToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Event reminder", style = MaterialTheme.typography.bodyLarge)
            ReminderDescription(event)
        }

        Switch(
            checked = isReminderEnabled,
            onCheckedChange = onReminderToggle
        )
    }
}

/** Shows the reminder timing text for timed and all-day campus events. */
@Composable
private fun ReminderDescription(event: EventItem) {
    Text(
        text = reminderDescriptionText(event),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Builds the reminder timing description shown below the reminder title. */
private fun reminderDescriptionText(event: EventItem): String {
    return if (event.allDay) {
        "Get a reminder at 9:00 AM on the event day"
    } else {
        "Get a reminder 30 minutes before the event"
    }
}

/** Shows one labeled detail line inside the campus event dialog. */
@Composable
private fun EventDetailLine(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value)
    }
}
