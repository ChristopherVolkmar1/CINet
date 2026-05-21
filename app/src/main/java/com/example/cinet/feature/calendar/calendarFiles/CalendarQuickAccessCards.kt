package com.example.cinet.feature.calendar.calendarFiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Shows the circular calendar action buttons at the bottom of the card. */
@Composable
fun CalendarQuickAccessCards(
    onClassesClick: () -> Unit,
    onStudyClick: () -> Unit,
    onEventsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        CalendarQuickAccessCircle(
            title = "Classes",
            icon = Icons.Default.School,
            onClick = onClassesClick
        )

        CalendarQuickAccessCircle(
            title = "Study",
            icon = Icons.Default.MenuBook,
            onClick = onStudyClick
        )

        CalendarQuickAccessCircle(
            title = "Events",
            icon = Icons.Default.CalendarMonth,
            onClick = onEventsClick
        )
    }
}

/** Draws one circular quick access button for the old calendar card layout. */
@Composable
private fun CalendarQuickAccessCircle(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(58.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Shows icon-only calendar action buttons in the persistent top bar. */
@Composable
fun CalendarTopBarActions(state: CalendarTopBarState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        CalendarTopBarIcon(
            title = "Classes",
            icon = Icons.Default.School,
            onClick = state.onClassesClick
        )

        CalendarTopBarIcon(
            title = "Study",
            icon = Icons.Default.MenuBook,
            onClick = state.onStudyClick
        )

        CalendarTopBarIcon(
            title = "Events",
            icon = Icons.Default.CalendarMonth,
            onClick = state.onEventsClick
        )
    }
}

/** Draws one large white icon button for the calendar top bar. */
@Composable
private fun CalendarTopBarIcon(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}