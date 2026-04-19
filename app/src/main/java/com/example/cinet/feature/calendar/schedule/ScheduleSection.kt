package com.example.cinet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.calendar.schedule.ScheduleItem
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
        // Depends on parent screen not having a selected date yet.
        Text("Select a date to view scheduled items.")
    } else if (itemsForSelectedDate.isEmpty()) {
        // itemsForSelectedDate is already filtered before reaching this composable,
        // so empty here means no items match the selected date.
        Text("No items scheduled for $selectedDate")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsForSelectedDate.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Click handling is delegated upward, typically to open edit/view behavior.
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

                        // Assumes dueTime is already stored in display-ready format.
                        Text(text = "Due: ${item.dueTime}")
                    }
                }
            }
        }
    }
}