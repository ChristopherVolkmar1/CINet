package com.example.cinet.feature.calendar.calendarFiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/** Shows the calendar page heading in the same bold style as the home greeting. */
@Composable
fun CalendarHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Calendar",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
