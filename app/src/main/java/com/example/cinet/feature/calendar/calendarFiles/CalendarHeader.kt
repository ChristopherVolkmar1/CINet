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

/** Shows the calendar back button and page title. */
@Composable
fun CalendarHeader(
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Calendar",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
