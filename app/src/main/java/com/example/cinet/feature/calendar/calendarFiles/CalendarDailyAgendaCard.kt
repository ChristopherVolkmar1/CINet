package com.example.cinet.feature.calendar.calendarFiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            itemCount = agendaItems.size,
            onTodayClick = onTodayClick
        )

        Spacer(modifier = Modifier.height(7.dp))

        if (agendaItems.isEmpty()) {
            EmptyAgendaMessage(selectedDate = selectedDate)
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                agendaItems.forEach { item ->
                    AgendaEventCard(item = item)
                }
            }
        }
    }
}

/** Shows the selected schedule heading and the total number of items. */
@Composable
private fun AgendaDateHeader(
    selectedDate: LocalDate,
    itemCount: Int,
    onTodayClick: () -> Unit
) {
    val selectedFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val isToday = selectedDate == LocalDate.now()
    val title = if (isToday) "Today’s Schedule" else "Schedule • ${selectedDate.format(selectedFormatter)}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = buildEventCountLabel(itemCount),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            modifier = Modifier.clickable(onClick = onTodayClick),
            maxLines = 1
        )
    }
}

/** Shows a helpful empty state when the selected day has no agenda items yet. */
@Composable
private fun EmptyAgendaMessage(selectedDate: LocalDate) {
    val isToday = selectedDate == LocalDate.now()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isToday) "No saved calendar items for today yet" else "No saved calendar items for this day yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Saved classes, study sessions, and reminder events will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f)
            )
        }
    }
}

/** Draws one individual event card using the same bold purple card style as the home news cards. */
@Composable
private fun AgendaEventCard(item: AgendaItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable { item.onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.42f))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.timeText,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.90f),
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = item.location.ifBlank { item.subtitle },
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(19.dp)
            )
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

/** Builds a readable event-count label. */
private fun buildEventCountLabel(itemCount: Int): String {
    return if (itemCount == 1) "1 Event" else "$itemCount Events"
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
