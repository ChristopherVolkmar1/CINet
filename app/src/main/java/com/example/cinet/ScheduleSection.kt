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
fun ScheduleSection(
    selectedDate: LocalDate?,
    itemsForSelectedDate: List<ScheduleItem>,
    onItemClick: (ScheduleItem) -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))

    Text("Schedule", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    if (selectedDate == null) {
        Text("Select a date to view scheduled items.")
    } else if (itemsForSelectedDate.isEmpty()) {
        Text("No items scheduled for $selectedDate")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsForSelectedDate.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = item.className,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item.assignmentName)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Due: ${item.dueTime}")
                    }
                }
            }
        }
    }
}