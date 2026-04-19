package com.example.cinet.feature.calendar.study


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.calendar.study.StudySession
import java.time.LocalDate

@Composable
fun StudySessionsSection(
    selectedDate: LocalDate?,
    studySessionsForSelectedDate: List<StudySession>,
    onSessionClick: (StudySession) -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))
    Text("Study Sessions", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    if (selectedDate == null) {
        Text("Select a date to view study sessions.")
    } else if (studySessionsForSelectedDate.isEmpty()) {
        Text("No study sessions for $selectedDate")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            studySessionsForSelectedDate.forEach { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSessionClick(session) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = session.className, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = session.topic)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = session.startTime)
                        if (session.location.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = session.location, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}