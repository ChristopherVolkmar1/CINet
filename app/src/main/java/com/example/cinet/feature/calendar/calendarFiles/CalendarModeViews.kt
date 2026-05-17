package com.example.cinet.feature.calendar.calendarFiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/** Shows the Day, Week, and Month selector as a pill control. */
@Composable
fun CalendarModeTabs(
    selectedMode: CalendarMode,
    onModeSelected: (CalendarMode) -> Unit,
    modifier: Modifier = Modifier,
    mergedWithCalendarGrid: Boolean = false
) {
    val containerColor = if (mergedWithCalendarGrid) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = if (mergedWithCalendarGrid) 0.dp else 5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf(CalendarMode.DAY, CalendarMode.WEEK, CalendarMode.MONTH).forEach { mode ->
                CalendarModeButton(
                    mode = mode,
                    isSelected = selectedMode == mode,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeSelected(mode) }
                )
            }
        }
    }
}

/** Shows the mode selector and active calendar grid as one merged purple section. */
@Composable
fun CalendarModeSection(
    selectedMode: CalendarMode,
    onModeSelected: (CalendarMode) -> Unit,
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    activityCountByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    PurpleSectionCard(contentPadding = 7.dp) {
        CalendarModeTabs(
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
            mergedWithCalendarGrid = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        CalendarModeContentBody(
            mode = selectedMode,
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            activityCountByDate = activityCountByDate,
            onDateSelected = onDateSelected,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )
    }
}

/** Shows one tab inside the segmented calendar selector. */
@Composable
private fun CalendarModeButton(
    mode: CalendarMode,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.displayName(),
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/** Chooses the correct day, week, or month calendar control. */
@Composable
fun CalendarModeContent(
    mode: CalendarMode,
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    activityCountByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    when (mode) {
        CalendarMode.DAY -> DayCalendarView(
            selectedDate = selectedDate,
            itemCount = activityCountByDate[selectedDate] ?: 0,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay
        )

        CalendarMode.WEEK -> WeekCalendarView(
            selectedDate = selectedDate,
            activityCountByDate = activityCountByDate,
            onDateSelected = onDateSelected,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek
        )

        CalendarMode.MONTH -> MonthCalendarView(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            activityCountByDate = activityCountByDate,
            onDateSelected = onDateSelected,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )
    }
}

/** Chooses the active calendar control without adding another outer card. */
@Composable
private fun CalendarModeContentBody(
    mode: CalendarMode,
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    activityCountByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    when (mode) {
        CalendarMode.DAY -> DayCalendarBody(
            selectedDate = selectedDate,
            itemCount = activityCountByDate[selectedDate] ?: 0,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay
        )

        CalendarMode.WEEK -> WeekCalendarBody(
            selectedDate = selectedDate,
            activityCountByDate = activityCountByDate,
            onDateSelected = onDateSelected,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek
        )

        CalendarMode.MONTH -> MonthCalendarBody(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            activityCountByDate = activityCountByDate,
            onDateSelected = onDateSelected,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )
    }
}

/** Shows one selected day with arrows and a compact activity summary. */
@Composable
private fun DayCalendarView(
    selectedDate: LocalDate,
    itemCount: Int,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    PurpleSectionCard(contentPadding = 12.dp) {
        DayCalendarBody(
            selectedDate = selectedDate,
            itemCount = itemCount,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay
        )
    }
}

/** Shows the selected day content without adding an outer card. */
@Composable
private fun DayCalendarBody(
    selectedDate: LocalDate,
    itemCount: Int,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }

    HeaderWithArrows(
        title = selectedDate.format(formatter),
        onPrevious = onPreviousDay,
        onNext = onNextDay
    )

    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedDate.dayOfMonth.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = buildItemCountText(itemCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Shows a purple rounded week strip. */
@Composable
private fun WeekCalendarView(
    selectedDate: LocalDate,
    activityCountByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    PurpleSectionCard(contentPadding = 12.dp) {
        WeekCalendarBody(
            selectedDate = selectedDate,
            activityCountByDate = activityCountByDate,
            onDateSelected = onDateSelected,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek
        )
    }
}

/** Shows the selected week content without adding an outer card. */
@Composable
private fun WeekCalendarBody(
    selectedDate: LocalDate,
    activityCountByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val weekDates = remember(selectedDate, firstDayOfWeek) {
        buildWeekDates(selectedDate, firstDayOfWeek)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NavigationButton(symbol = "‹", onClick = onPreviousWeek)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            weekDates.forEach { date ->
                WeekStripDate(
                    date = date,
                    isSelected = date == selectedDate,
                    itemCount = activityCountByDate[date] ?: 0,
                    onClick = { onDateSelected(date) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        NavigationButton(symbol = "›", onClick = onNextWeek)
    }
}

/** Shows the full month grid in the same bold card style used elsewhere on the page. */
@Composable
private fun MonthCalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    activityCountByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    PurpleSectionCard(contentPadding = 12.dp) {
        MonthCalendarBody(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            activityCountByDate = activityCountByDate,
            onDateSelected = onDateSelected,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )
    }
}

/** Shows the month grid content without adding an outer card. */
@Composable
private fun MonthCalendarBody(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    activityCountByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val monthCells = remember(currentMonth, firstDayOfWeek) {
        buildMonthCells(currentMonth, firstDayOfWeek)
    }

    HeaderWithArrows(
        title = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
        onPrevious = onPreviousMonth,
        onNext = onNextMonth
    )

    Spacer(modifier = Modifier.height(8.dp))
    WeekdayHeader(firstDayOfWeek = firstDayOfWeek)
    Spacer(modifier = Modifier.height(5.dp))
    MonthGrid(
        monthCells = monthCells,
        selectedDate = selectedDate,
        activityCountByDate = activityCountByDate,
        onDateSelected = onDateSelected
    )
}

/** Wraps calendar view pieces in a filled purple rounded card. */
@Composable
private fun PurpleSectionCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/** Shows a centered title with previous and next buttons. */
@Composable
private fun HeaderWithArrows(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NavigationButton(symbol = "‹", onClick = onPrevious)

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )

        NavigationButton(symbol = "›", onClick = onNext)
    }
}

/** Shows one lightweight arrow button. */
@Composable
private fun NavigationButton(
    symbol: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Shows weekday labels for month view. */
@Composable
private fun WeekdayHeader(firstDayOfWeek: DayOfWeek) {
    val orderedDays = remember(firstDayOfWeek) {
        buildOrderedDays(firstDayOfWeek)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        orderedDays.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Shows one date in the week strip with a selected circle and activity dot. */
@Composable
private fun WeekStripDate(
    date: LocalDate,
    isSelected: Boolean,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .size(if (isSelected) 38.dp else 34.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(3.dp))
        ActivityDots(itemCount = itemCount, isSelected = isSelected)
    }
}

/** Shows all date rows needed for month view. */
@Composable
private fun MonthGrid(
    monthCells: List<LocalDate?>,
    selectedDate: LocalDate,
    activityCountByDate: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        monthCells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { date ->
                    if (date == null) {
                        EmptyDateCell(modifier = Modifier.weight(1f))
                    } else {
                        MonthDateCell(
                            modifier = Modifier.weight(1f),
                            date = date,
                            isSelected = date == selectedDate,
                            itemCount = activityCountByDate[date] ?: 0,
                            onClick = { onDateSelected(date) }
                        )
                    }
                }
            }
        }
    }
}

/** Shows one blank placeholder used before or after real month dates. */
@Composable
private fun EmptyDateCell(modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(38.dp))
}

/** Shows one clickable date cell in the month grid. */
@Composable
private fun MonthDateCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    isSelected: Boolean,
    itemCount: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 26.dp else 22.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        ActivityDots(itemCount = itemCount, isSelected = isSelected)
    }
}

/** Draws a small activity dot under dates that have at least one item. */
@Composable
private fun ActivityDots(
    itemCount: Int,
    isSelected: Boolean
) {
    val dotColor = if (isSelected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f)
    }

    Box(
        modifier = Modifier.height(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (itemCount > 0) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/** Builds the seven dates that belong to the selected week. */
private fun buildWeekDates(
    selectedDate: LocalDate,
    firstDayOfWeek: DayOfWeek
): List<LocalDate> {
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    return (0..6).map { weekStart.plusDays(it.toLong()) }
}

/** Builds the visible month cells, including empty cells for grid alignment. */
private fun buildMonthCells(
    currentMonth: YearMonth,
    firstDayOfWeek: DayOfWeek
): List<LocalDate?> {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysBeforeMonth = calculateLeadingEmptyCells(firstDayOfMonth, firstDayOfWeek)
    val cells = mutableListOf<LocalDate?>()

    repeat(daysBeforeMonth) { cells.add(null) }
    for (day in 1..currentMonth.lengthOfMonth()) {
        cells.add(currentMonth.atDay(day))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }

    return cells
}

/** Counts how many empty cells are needed before the first day of the month. */
private fun calculateLeadingEmptyCells(
    firstDayOfMonth: LocalDate,
    firstDayOfWeek: DayOfWeek
): Int {
    val firstDayValue = firstDayOfWeek.value
    val monthStartValue = firstDayOfMonth.dayOfWeek.value
    return (monthStartValue - firstDayValue + 7) % 7
}

/** Builds the ordered list of weekdays starting with the user's locale first day. */
private fun buildOrderedDays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> {
    return (0..6).map { firstDayOfWeek.plus(it.toLong()) }
}

/** Converts one mode enum into display text. */
private fun CalendarMode.displayName(): String {
    return name.lowercase().replaceFirstChar { it.uppercase() }
}

/** Builds the summary line for day view. */
private fun buildItemCountText(itemCount: Int): String {
    return when (itemCount) {
        0 -> "No saved items for this day yet."
        1 -> "1 saved item for this day."
        else -> "$itemCount saved items for this day."
    }
}
