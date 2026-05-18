package com.example.cinet.feature.calendar.calendarFiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.calendar.event.EventItem
import com.example.cinet.feature.calendar.study.StudySession
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.graphics.lerp

/** Identifies which quick-access popup is currently open. */
enum class CalendarQuickAccessType {
    CLASSES,
    STUDY,
    EVENTS
}

/** Shows today's/selected-day content for one quick-access category with a + button for creation. */
@Composable
fun CalendarQuickAccessPopup(
    type: CalendarQuickAccessType,
    selectedDate: LocalDate,
    classes: List<ClassItem>,
    syncedCanvasClassNames: List<String> = emptyList(),
    studySessions: List<StudySession>,
    events: List<EventItem>,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onClassClick: (ClassItem) -> Unit,
    onCanvasClassClick: (String) -> Unit = {},
    onStudySessionClick: (StudySession) -> Unit,
    onEventClick: (EventItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = popupTitle(type),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall
                )

                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add"
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = popupDescription(type),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (type) {
                    CalendarQuickAccessType.CLASSES -> ClassPopupList(
                        classes = classes,
                        syncedCanvasClassNames = syncedCanvasClassNames,
                        onClassClick = onClassClick,
                        onCanvasClassClick = onCanvasClassClick
                    )

                    CalendarQuickAccessType.STUDY -> StudyPopupList(
                        studySessions = studySessions,
                        onStudySessionClick = onStudySessionClick
                    )

                    CalendarQuickAccessType.EVENTS -> EventPopupList(
                        events = events,
                        onEventClick = onEventClick
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

/** Shows the popup title row with the moved + action button. */
@Composable
private fun PopupHeader(
    type: CalendarQuickAccessType,
    onAddClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PopupIcon(type = type)

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = popupTitle(type),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondary
        )

        IconButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

/** Shows the icon bubble for the selected popup type. */
@Composable
private fun PopupIcon(type: CalendarQuickAccessType) {
    //val green = MaterialTheme.colorScheme.onSecondary

    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primary,
            0.80f
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = popupIcon(type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Shows saved class meetings and Canvas-synced classes in the quick-access popup. */
@Composable
private fun ClassPopupList(
    classes: List<ClassItem>,
    syncedCanvasClassNames: List<String>,
    onClassClick: (ClassItem) -> Unit,
    onCanvasClassClick: (String) -> Unit
) {
    val hasSavedClasses = classes.isNotEmpty()
    val hasCanvasClasses = syncedCanvasClassNames.isNotEmpty()

    if (!hasSavedClasses && !hasCanvasClasses) {
        EmptyPopupMessage(text = "No classes scheduled or synced yet.")
        return
    }

    if (hasSavedClasses) {
        PopupSectionLabel(text = "Saved class meetings")

        classes.forEach { classItem ->
            PopupItemCard(
                title = classItem.name,
                primaryDetail = buildTimeRange(classItem.startTime, classItem.endTime),
                secondaryDetail = classItem.meetingDays.joinToString(", "),
                location = classItem.location,
                onClick = { onClassClick(classItem) }
            )
        }
    }

    if (hasSavedClasses && hasCanvasClasses) {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (hasCanvasClasses) {
        PopupSectionLabel(text = "Canvas synced classes")

        syncedCanvasClassNames.forEach { canvasClassName ->
            PopupItemCard(
                title = canvasClassName,
                primaryDetail = "Tap to add meeting time",
                secondaryDetail = "Synced from Canvas",
                location = "",
                onClick = { onCanvasClassClick(canvasClassName) }
            )
        }
    }
}

/** Shows a small label above a group of popup rows. */
@Composable
private fun PopupSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(6.dp))
}

/** Shows study-session rows in the quick-access popup. */
@Composable
private fun StudyPopupList(
    studySessions: List<StudySession>,
    onStudySessionClick: (StudySession) -> Unit
) {
    if (studySessions.isEmpty()) {
        EmptyPopupMessage(text = "No study sessions planned for this day yet.")
        return
    }

    studySessions.forEach { session ->
        PopupItemCard(
            title = session.topic,
            primaryDetail = session.startTime.ifBlank { "Study session" },
            secondaryDetail = session.className,
            location = session.location,
            onClick = { onStudySessionClick(session) }
        )
    }
}

/** Shows custom user events and live campus event rows in the quick-access popup. */
@Composable
private fun EventPopupList(
    events: List<EventItem>,
    onEventClick: (EventItem) -> Unit
) {
    if (events.isEmpty()) {
        EmptyPopupMessage(text = "No custom or campus events for this day yet.")
        return
    }

    events.forEach { event ->
        PopupItemCard(
            title = event.name,
            primaryDetail = if (event.allDay) "All day" else event.time.ifBlank { "Event" },
            secondaryDetail = buildEventPopupDescription(event),
            location = event.location,
            onClick = { onEventClick(event) }
        )
    }
}

/** Builds the secondary text for custom events and campus feed events. */
private fun buildEventPopupDescription(event: EventItem): String {
    val typeLabel = if (event.isCampusEvent) "Live Campus Event" else "Custom Event"
    return if (event.description.isBlank()) {
        typeLabel
    } else {
        "$typeLabel • ${event.description}"
    }
}
/** Shows one standard Material-style item inside a quick-access popup. */
@Composable
private fun PopupItemCard(
    title: String,
    primaryDetail: String,
    secondaryDetail: String,
    location: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (primaryDetail.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))

                DetailRow(
                    icon = Icons.Default.CalendarMonth,
                    text = primaryDetail
                )
            }

            if (secondaryDetail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = secondaryDetail,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                DetailRow(
                    icon = Icons.Default.LocationOn,
                    text = location
                )
            }
        }
    }
}

/** Shows an icon and detail string inside one popup item. */
@Composable
private fun DetailRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Shows the empty state inside one popup. */
@Composable
private fun EmptyPopupMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Returns the popup title for one quick-access type. */
private fun popupTitle(type: CalendarQuickAccessType): String {
    return when (type) {
        CalendarQuickAccessType.CLASSES -> "Classes"
        CalendarQuickAccessType.STUDY -> "Study"
        CalendarQuickAccessType.EVENTS -> "Events"
    }
}

/** Returns the popup description for one quick-access type. */
private fun popupDescription(type: CalendarQuickAccessType): String {
    return when (type) {
        CalendarQuickAccessType.CLASSES -> "Classes scheduled for this day. Tap a class to edit it, or press + to create a class."
        CalendarQuickAccessType.STUDY -> "Study sessions planned for this day. Tap a session to edit it, or press + to create a study session."
        CalendarQuickAccessType.EVENTS -> "Custom events and live campus events for this day. Tap a custom event to edit it, tap a campus event to manage its reminder, or press + to create a custom event."
    }
}

/** Returns the icon for one quick-access type. */
private fun popupIcon(type: CalendarQuickAccessType): ImageVector {
    return when (type) {
        CalendarQuickAccessType.CLASSES -> Icons.Default.School
        CalendarQuickAccessType.STUDY -> Icons.Default.MenuBook
        CalendarQuickAccessType.EVENTS -> Icons.Default.CalendarMonth
    }
}

/** Builds a clean class time range string. */
private fun buildTimeRange(startTime: String, endTime: String): String {
    return when {
        startTime.isBlank() && endTime.isBlank() -> "Class"
        endTime.isBlank() -> startTime
        startTime.isBlank() -> endTime
        else -> "$startTime - $endTime"
    }
}
