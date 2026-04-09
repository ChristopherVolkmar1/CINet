package com.example.cinet

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
        Text("Select a date to view classes for that day.")
    } else if (classesForSelectedDate.isEmpty()) {
        Text("No classes scheduled for $selectedDate")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            classesForSelectedDate.forEach { classItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClassClick(classItem) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = classItem.name,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${classItem.startTime} - ${classItem.endTime}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = classItem.meetingDays.joinToString(", "))
                    }
                }
            }
        }
    }
}