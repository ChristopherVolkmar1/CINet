package com.example.cinet.feature.calendar.calendarFiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.calendar.event.EventItem
import com.example.cinet.feature.calendar.study.StudySession
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Shows the selected day's classes, study sessions, custom events, and toggled campus events. */
@Composable
fun CalendarDailyAgendaCard(
    selectedDate: LocalDate,
    classes: List<ClassItem>,
    studySessions: List<StudySession>,
    events: List<EventItem>,
    reminderCampusEvents: List<EventItem>,
    onTodayClick: () -> Unit,
    onClassClick: (ClassItem) -> Unit,
    onStudySessionClick: (StudySession) -> Unit,
    onEventClick: (EventItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val agendaItems = remember(
        selectedDate,
        classes,
        studySessions,
        events,
        reminderCampusEvents,
        onClassClick,
        onStudySessionClick,
        onEventClick
    ) {
        buildAgendaItems(
            classes = classes,
            studySessions = studySessions,
            events = events,
            reminderCampusEvents = reminderCampusEvents,
            onClassClick = onClassClick,
            onStudySessionClick = onStudySessionClick,
            onEventClick = onEventClick
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AgendaDateHeader(
            selectedDate = selectedDate,
            onTodayClick = onTodayClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.10f)
            )
        ) {
            if (agendaItems.isEmpty()) {
                EmptyAgendaMessage(selectedDate = selectedDate)
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    agendaItems.forEachIndexed { index, item ->
                        AgendaTimelineRow(
                            item = item,
                            isFirst = index == 0,
                            isLast = index == agendaItems.lastIndex
                        )

                        if (index != agendaItems.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 86.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Shows the date title and the Today shortcut above the agenda card. */
@Composable
private fun AgendaDateHeader(
    selectedDate: LocalDate,
    onTodayClick: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }
    val green = MaterialTheme.colorScheme.secondaryContainer

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(green.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = green,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = selectedDate.format(formatter),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedButton(
            onClick = onTodayClick,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, green.copy(alpha = 0.30f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = green,
                containerColor = Color.Transparent
            )
        ) {
            Text(
                text = "Today",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Shows a helpful empty state when the selected day has no agenda items yet. */
@Composable
private fun EmptyAgendaMessage(selectedDate: LocalDate) {
    val isToday = selectedDate == LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isToday) "No saved calendar items for today yet" else "No saved calendar items for this day yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Use Classes, Study, or Events below to add and view today's items.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Draws one timeline row for one class, study session, event, or reminder-enabled campus event. */
@Composable
private fun AgendaTimelineRow(
    item: AgendaItem,
    isFirst: Boolean,
    isLast: Boolean
) {
    val green = MaterialTheme.colorScheme.secondaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.timeText,
            modifier = Modifier.width(66.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Column(
            modifier = Modifier.width(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (isFirst) 12.dp else 22.dp)
                    .background(if (isFirst) Color.Transparent else green.copy(alpha = 0.55f))
            )

            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(green)
            )

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (isLast) 12.dp else 22.dp)
                    .background(if (isLast) Color.Transparent else green.copy(alpha = 0.55f))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (item.location.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = green.copy(alpha = 0.80f),
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = item.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Converts selected-day content into one sorted agenda list. */
private fun buildAgendaItems(
    classes: List<ClassItem>,
    studySessions: List<StudySession>,
    events: List<EventItem>,
    reminderCampusEvents: List<EventItem>,
    onClassClick: (ClassItem) -> Unit,
    onStudySessionClick: (StudySession) -> Unit,
    onEventClick: (EventItem) -> Unit
): List<AgendaItem> {
    val classItems = classes.map { classItem ->
        val timeText = buildTimeRange(classItem.startTime, classItem.endTime, "Class")
        AgendaItem(
            timeText = timeText,
            title = classItem.name,
            subtitle = "Class",
            location = classItem.location,
            sortValue = parseTimeToMinutes(timeText),
            onClick = { onClassClick(classItem) }
        )
    }

    val studyItems = studySessions.map { session ->
        AgendaItem(
            timeText = session.startTime.ifBlank { "Study" },
            title = session.topic,
            subtitle = if (session.className.isBlank()) "Study Session" else "Study • ${session.className}",
            location = session.location,
            sortValue = parseTimeToMinutes(session.startTime),
            onClick = { onStudySessionClick(session) }
        )
    }

    val customEventItems = events.map { event ->
        AgendaItem(
            timeText = if (event.allDay) "All day" else event.time.ifBlank { "Event" },
            title = event.name,
            subtitle = "Custom Event",
            location = event.location,
            sortValue = if (event.allDay) Int.MIN_VALUE else parseTimeToMinutes(event.time),
            onClick = { onEventClick(event) }
        )
    }

    val campusReminderItems = reminderCampusEvents.map { event ->
        AgendaItem(
            timeText = if (event.allDay) "All day" else event.time.ifBlank { "Campus" },
            title = event.name,
            subtitle = "Campus Event • Reminder on",
            location = event.location,
            sortValue = if (event.allDay) Int.MIN_VALUE else parseTimeToMinutes(event.time),
            onClick = { onEventClick(event) }
        )
    }

    return (classItems + studyItems + customEventItems + campusReminderItems)
        .sortedWith(compareBy<AgendaItem> { it.sortValue }.thenBy { it.title.lowercase() })
}

/** Builds a class time range while keeping blank times readable. */
private fun buildTimeRange(
    startTime: String,
    endTime: String,
    fallback: String
): String {
    return when {
        startTime.isBlank() && endTime.isBlank() -> fallback
        endTime.isBlank() -> startTime
        startTime.isBlank() -> endTime
        else -> "$startTime - $endTime"
    }
}

/** Converts common app time text into minutes since midnight for sorting. */
private fun parseTimeToMinutes(time: String): Int {
    if (time.equals("All day", ignoreCase = true)) return Int.MIN_VALUE

    val normalizedTime = time.substringBefore("-").trim()
    val pieces = normalizedTime.split(" ", ":")
    if (pieces.size < 3) return Int.MAX_VALUE

    var hour = pieces[0].toIntOrNull() ?: return Int.MAX_VALUE
    val minute = pieces[1].toIntOrNull() ?: return Int.MAX_VALUE
    val period = pieces[2].uppercase()

    if (period == "PM" && hour != 12) hour += 12
    if (period == "AM" && hour == 12) hour = 0

    return hour * 60 + minute
}

/** Stores one unified row used by the daily agenda card. */
private data class AgendaItem(
    val timeText: String,
    val title: String,
    val subtitle: String,
    val location: String,
    val sortValue: Int,
    val onClick: () -> Unit
)
