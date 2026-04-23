package com.example.cinet.feature.calendar.calendarFiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate,
    scheduleItems: List<ScheduleItem>,
    markedDates: Set<String>,
    onDateSelected: (Int) -> Unit,
    onSameDateClicked: () -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val days = mutableListOf<Int?>()
    repeat(startDayOfWeek) { days.add(null) }

    for (day in 1..daysInMonth) {
        days.add(day)
    }

    while (days.size % 7 != 0) {
        days.add(null)
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(it)
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Column {
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val cellDate = currentMonth.atDay(day)
                            val isToday = cellDate == today
                            val isSelected = selectedDate == cellDate
                            val dateString = "%04d-%02d-%02d".format(
                                cellDate.year,
                                cellDate.monthValue,
                                cellDate.dayOfMonth
                            )
                            val hasItems = scheduleItems.any { it.date == dateString } || markedDates.contains(dateString)

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        CircleShape
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.tertiary,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        if (selectedDate == cellDate) {
                                            onSameDateClicked()
                                        } else {
                                            onDateSelected(day)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (hasItems) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(
                                                    if (isToday) Color.White else MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
