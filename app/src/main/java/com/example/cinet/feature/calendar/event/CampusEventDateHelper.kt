package com.example.cinet.feature.calendar.event

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Parses campus event dates and builds the display values used by the calendar UI. */
class CampusEventDateHelper(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE
    private val localDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val localDateTimeMinuteFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")
    private val utcDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
    private val utcDateTimeMinuteFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'")
    private val outputDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val outputTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    /** Converts one raw ICS field into epoch milliseconds. */
    fun parseDateMillis(field: IcsField?): Long? {
        field ?: return null

        return when {
            field.isDateOnly -> parseDateOnlyMillis(field.value)
            field.value.endsWith("Z") -> parseUtcDateTimeMillis(field.value)
            else -> parseLocalDateTimeMillis(field.value, field.timeZone)
        }
    }

    /** Creates the fallback end time when an event does not include DTEND. */
    fun buildFallbackEndMillis(startField: IcsField, startMillis: Long): Long {
        if (!startField.isDateOnly) {
            return startMillis
        }

        return Instant.ofEpochMilli(startMillis)
            .atZone(zoneId)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    /** Converts epoch milliseconds into a zoned date time using the parser's zone. */
    fun toZonedDateTime(epochMillis: Long): ZonedDateTime {
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId)
    }

    /** Resolves the last day an event should appear on the calendar. */
    fun resolveLastCoveredDate(
        startField: IcsField,
        endField: IcsField?,
        rawEndDate: LocalDate
    ): LocalDate {
        return if (startField.isDateOnly && endField != null) rawEndDate.minusDays(1) else rawEndDate
    }

    /** Builds the time text shown on a campus event card. */
    fun buildTimeLabel(startTime: LocalTime, endTime: LocalTime, allDay: Boolean): String {
        if (allDay) return "All day"
        if (startTime == endTime) return outputTimeFormatter.format(startTime)
        return "${outputTimeFormatter.format(startTime)} - ${outputTimeFormatter.format(endTime)}"
    }

    /** Builds the ISO date string stored by the existing calendar UI. */
    fun formatDate(date: LocalDate): String {
        return date.format(outputDateFormatter)
    }

    /** Creates an inclusive list of dates for multi-day campus events. */
    fun generateDateSequence(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val safeEndDate = if (endDate.isBefore(startDate)) startDate else endDate
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate

        while (!currentDate.isAfter(safeEndDate)) {
            dates.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        return dates
    }

    /** Parses a date-only ICS value. */
    private fun parseDateOnlyMillis(value: String): Long {
        val date = LocalDate.parse(value, dateFormatter)
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    /** Parses a UTC ICS date-time value. */
    private fun parseUtcDateTimeMillis(value: String): Long? {
        val localDateTime = parseUtcLocalDateTime(value) ?: return null
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    /** Parses a local or TZID-based ICS date-time value. */
    private fun parseLocalDateTimeMillis(value: String, timeZone: String?): Long? {
        val localDateTime = parseLocalDateTime(value) ?: return null
        val parsedZone = resolveZoneId(timeZone)
        return localDateTime.atZone(parsedZone).toInstant().toEpochMilli()
    }

    /** Parses a UTC local date time before it is converted to an instant. */
    private fun parseUtcLocalDateTime(value: String): LocalDateTime? {
        return try {
            when (value.length) {
                16 -> LocalDateTime.parse(value, utcDateTimeFormatter)
                14 -> LocalDateTime.parse(value, utcDateTimeMinuteFormatter)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Parses a non-UTC local date time before time zone conversion. */
    private fun parseLocalDateTime(value: String): LocalDateTime? {
        return try {
            when (value.length) {
                15 -> LocalDateTime.parse(value, localDateTimeFormatter)
                13 -> LocalDateTime.parse(value, localDateTimeMinuteFormatter)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Resolves the ICS TZID into a valid Java ZoneId with a safe fallback. */
    private fun resolveZoneId(timeZone: String?): ZoneId {
        return try {
            if (timeZone.isNullOrBlank()) zoneId else ZoneId.of(timeZone)
        } catch (_: Exception) {
            zoneId
        }
    }
}
