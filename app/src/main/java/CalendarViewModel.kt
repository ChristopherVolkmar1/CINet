package com.example.cinet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel : ViewModel() {

    var currentMonth by mutableStateOf(YearMonth.now())
        private set

    var selectedDate by mutableStateOf<LocalDate?>(null)
        private set

    var classItems by mutableStateOf<List<ClassItem>>(emptyList())
        private set

    var scheduleItems by mutableStateOf<List<ScheduleItem>>(emptyList())
        private set

    fun nextMonth() {
        currentMonth = currentMonth.plusMonths(1)
    }

    fun previousMonth() {
        currentMonth = currentMonth.minusMonths(1)
    }

    fun selectDate(day: Int) {
        selectedDate = currentMonth.atDay(day)
    }

    fun addClass(
        name: String,
        meetingDays: List<String>,
        startTime: String,
        endTime: String
    ) {
        val orderedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val sortedMeetingDays = meetingDays.sortedBy { orderedDays.indexOf(it) }

        val newClass = ClassItem(
            name = name,
            meetingDays = sortedMeetingDays,
            startTime = startTime,
            endTime = endTime
        )
        classItems = sortClassesByTime(classItems + newClass)
    }

    fun updateClass(
        classId: Long,
        name: String,
        meetingDays: List<String>,
        startTime: String,
        endTime: String
    ) {
        val orderedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val sortedMeetingDays = meetingDays.sortedBy { orderedDays.indexOf(it) }

        classItems = sortClassesByTime(
            classItems.map { item ->
                if (item.id == classId) {
                    item.copy(
                        name = name,
                        meetingDays = sortedMeetingDays,
                        startTime = startTime,
                        endTime = endTime
                    )
                } else item
            }
        )

        scheduleItems = scheduleItems.map { item ->
            if (item.classId == classId) {
                item.copy(className = name)
            } else item
        }
    }

    fun deleteClass(classId: Long) {
        classItems = classItems.filterNot { it.id == classId }
        scheduleItems = scheduleItems.filterNot { it.classId == classId }
    }

    fun addScheduleItem(
        classItem: ClassItem,
        assignmentName: String,
        dueTime: String
    ) {
        val date = selectedDate ?: return

        val newItem = ScheduleItem(
            date = formatDate(date),
            classId = classItem.id,
            className = classItem.name,
            assignmentName = assignmentName,
            dueTime = dueTime
        )

        scheduleItems = scheduleItems + newItem
    }

    fun updateScheduleItem(
        itemId: Long,
        classItem: ClassItem,
        assignmentName: String,
        dueTime: String
    ) {
        scheduleItems = scheduleItems.map { item ->
            if (item.id == itemId) {
                item.copy(
                    classId = classItem.id,
                    className = classItem.name,
                    assignmentName = assignmentName,
                    dueTime = dueTime
                )
            } else item
        }
    }

    fun deleteScheduleItem(itemId: Long) {
        scheduleItems = scheduleItems.filterNot { it.id == itemId }
    }

    fun getItemsForSelectedDate(): List<ScheduleItem> {
        val date = selectedDate ?: return emptyList()
        val formattedDate = formatDate(date)

        return scheduleItems
            .filter { it.date == formattedDate }
            .sortedBy { parseTimeToSortableValue(it.dueTime) }
    }
    fun getClassesGroupedByDay(): Map<String, List<ClassItem>> {
        val orderedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        return orderedDays.associateWith { day ->
            classItems
                .filter { it.meetingDays.contains(day) }
                .sortedBy { parseTimeToSortableValue(it.startTime) }
        }.filterValues { it.isNotEmpty() }
    }

    fun getClassesForSelectedDate(): List<ClassItem> {
        val date = selectedDate ?: return emptyList()

        val dayName = when (date.dayOfWeek.value) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> ""
        }

        return classItems
            .filter { it.meetingDays.contains(dayName) }
            .sortedBy { parseTimeToSortableValue(it.startTime) }
    }
    private fun formatDate(date: LocalDate): String {
        return "%04d-%02d-%02d".format(
            date.year,
            date.monthValue,
            date.dayOfMonth
        )
    }
    private fun parseTimeToSortableValue(time: String): Int {
        val parts = time.split(" ", ":")
        if (parts.size < 3) return Int.MAX_VALUE

        var hour = parts[0].toIntOrNull() ?: return Int.MAX_VALUE
        val minute = parts[1].toIntOrNull() ?: return Int.MAX_VALUE
        val amPm = parts[2]

        if (amPm == "PM" && hour != 12) hour += 12
        if (amPm == "AM" && hour == 12) hour = 0

        return hour * 60 + minute
    }

    private fun sortClassesByTime(classes: List<ClassItem>): List<ClassItem> {
        return classes.sortedBy { parseTimeToSortableValue(it.startTime) }
    }
}