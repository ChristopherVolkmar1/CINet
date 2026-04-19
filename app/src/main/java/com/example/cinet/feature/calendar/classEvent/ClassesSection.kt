package com.example.cinet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.calendar.classEvent.ClassItem
import java.time.LocalDate

@Composable
fun ClassesSection(
    selectedDate: LocalDate?,
    classesForSelectedDate: List<ClassItem>,
    onClassClick: (ClassItem) -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))

    Text("Classes", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    if (selectedDate == null) {
        // This state depends on CalendarScreen not selecting a date yet.
        Text("Select a date to view classes for that day.")
    } else if (classesForSelectedDate.isEmpty()) {
        // classesForSelectedDate is pre-filtered by the ViewModel,
        // so empty here means "no classes match this date", not "no classes exist".
        Text("No classes scheduled for $selectedDate")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            classesForSelectedDate.forEach { classItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Click behavior is delegated upward (likely opens edit dialog).
                        .clickable { onClassClick(classItem) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = classItem.name,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Assumes startTime/endTime are already formatted strings
                        // (no formatting logic is done here).
                        Text(text = "${classItem.startTime} - ${classItem.endTime} | ${classItem.location}")

                        Spacer(modifier = Modifier.height(4.dp))

                        //Text(text = classItem.location)
                        //Spacer(modifier = Modifier.height(4.dp))

                        // meetingDays must already be stored in display-ready format
                        // (e.g., "Mon", "Tue"), since this just joins them.
                        Text(text = classItem.meetingDays.joinToString(", "))
                    }
                }
            }
        }
    }
}