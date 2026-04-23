package com.example.cinet.feature.calendar.event

import java.time.LocalDate
import java.time.ZoneId

/** Parses raw ICS text into calendar events that match the app's existing event model. */
class CampusEventFeedParser(
    zoneId: ZoneId = ZoneId.systemDefault(),
    private val dateHelper: CampusEventDateHelper = CampusEventDateHelper(zoneId)
) {

    /** Converts one ICS document into a sorted list of display events. */
    fun parse(icsText: String): List<EventItem> {
        val unfoldedText = unfoldLines(icsText)
        val eventBlocks = extractEventBlocks(unfoldedText)

        return eventBlocks
            .flatMap(::parseEventBlock)
            .sortedWith(compareBy<EventItem> { it.date }
                .thenBy { it.startEpochMillis ?: Long.MAX_VALUE }
                .thenBy { it.name })
    }

    /** Joins folded ICS lines so each property can be read as one line. */
    private fun unfoldLines(icsText: String): String {
        return icsText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("\n[ \t]"), "")
    }

    /** Extracts each VEVENT block from the ICS document. */
    private fun extractEventBlocks(icsText: String): List<String> {
        return icsText
            .split("BEGIN:VEVENT")
            .drop(1)
            .mapNotNull(::extractEventBlockBody)
    }

    /** Keeps only the inner lines of one VEVENT block. */
    private fun extractEventBlockBody(block: String): String? {
        return block
            .substringBefore("END:VEVENT")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    /** Parses one VEVENT block and expands multi-day events across their covered dates. */
    private fun parseEventBlock(block: String): List<EventItem> {
        val lines = block.lines().filter { it.isNotBlank() }
        val metadata = readEventMetadata(lines) ?: return emptyList()
        val timing = readEventTiming(lines) ?: return emptyList()
        val displayDates = buildDisplayDates(timing)
        val timeLabel = buildTimeLabel(timing)

        return displayDates.map { date ->
            buildEventItem(
                metadata = metadata,
                timing = timing,
                displayDate = date,
                timeLabel = timeLabel
            )
        }
    }

    /** Reads the text fields needed to display one campus event. */
    private fun readEventMetadata(lines: List<String>): CampusEventMetadata? {
        val uid = readField(lines, "UID")?.value ?: return null
        val title = sanitizeFieldValue(lines, "SUMMARY", "Campus Event")
        val description = sanitizeFieldValue(lines, "DESCRIPTION")
        val location = sanitizeFieldValue(lines, "LOCATION")

        return CampusEventMetadata(
            uid = uid,
            title = title,
            description = description,
            location = location
        )
    }

    /** Reads and converts the start and end timing for one campus event. */
    private fun readEventTiming(lines: List<String>): CampusEventTiming? {
        val startField = readField(lines, "DTSTART") ?: return null
        val endField = readField(lines, "DTEND")
        val startMillis = dateHelper.parseDateMillis(startField) ?: return null
        val endMillis = dateHelper.parseDateMillis(endField)
            ?: dateHelper.buildFallbackEndMillis(startField, startMillis)

        return CampusEventTiming(
            startField = startField,
            endField = endField,
            startMillis = startMillis,
            endMillis = endMillis,
            allDay = startField.isDateOnly
        )
    }

    /** Builds the list of dates on which the event should appear in the calendar. */
    private fun buildDisplayDates(timing: CampusEventTiming): List<LocalDate> {
        val startDate = dateHelper.toZonedDateTime(timing.startMillis).toLocalDate()
        val rawEndDate = dateHelper.toZonedDateTime(timing.endMillis).toLocalDate()
        val lastCoveredDate = dateHelper.resolveLastCoveredDate(
            startField = timing.startField,
            endField = timing.endField,
            rawEndDate = rawEndDate
        )

        return dateHelper.generateDateSequence(startDate, lastCoveredDate)
    }

    /** Builds the time label shown in the event list and details dialog. */
    private fun buildTimeLabel(timing: CampusEventTiming): String {
        val startTime = dateHelper.toZonedDateTime(timing.startMillis).toLocalTime()
        val endTime = dateHelper.toZonedDateTime(timing.endMillis).toLocalTime()
        return dateHelper.buildTimeLabel(startTime, endTime, timing.allDay)
    }

    /** Creates one EventItem instance for a specific display date. */
    private fun buildEventItem(
        metadata: CampusEventMetadata,
        timing: CampusEventTiming,
        displayDate: LocalDate,
        timeLabel: String
    ): EventItem {
        val formattedDate = dateHelper.formatDate(displayDate)

        return EventItem(
            id = buildEventId(metadata.uid, formattedDate),
            date = formattedDate,
            name = metadata.title,
            time = timeLabel,
            location = metadata.location,
            description = metadata.description,
            source = EventSource.CAMPUS,
            startEpochMillis = timing.startMillis,
            endEpochMillis = timing.endMillis,
            allDay = timing.allDay
        )
    }

    /** Builds the stable ID used for one campus event occurrence. */
    private fun buildEventId(uid: String, formattedDate: String): String {
        return "campus_${uid}_${formattedDate}"
    }

    /** Reads and sanitizes one text field from the ICS lines. */
    private fun sanitizeFieldValue(
        lines: List<String>,
        key: String,
        defaultValue: String = ""
    ): String {
        val rawValue = readField(lines, key)?.value ?: defaultValue
        return CampusEventTextFormatter.sanitize(rawValue)
    }

    /** Reads one ICS property and keeps any useful metadata attached to it. */
    private fun readField(lines: List<String>, key: String): IcsField? {
        val rawLine = lines.firstOrNull { it.startsWith("$key:") || it.startsWith("$key;") } ?: return null
        val header = rawLine.substringBefore(':')
        val value = rawLine.substringAfter(':', missingDelimiterValue = "")
        val timeZone = Regex("TZID=([^;:]+)").find(header)?.groupValues?.get(1)
        val isDateOnly = header.contains("VALUE=DATE") || value.length == 8

        return IcsField(
            value = value.trim(),
            timeZone = timeZone,
            isDateOnly = isDateOnly
        )
    }
}

/** Stores one parsed ICS field together with the metadata needed to parse dates. */
data class IcsField(
    val value: String,
    val timeZone: String?,
    val isDateOnly: Boolean
)

/** Stores the text content used to display one campus event. */
private data class CampusEventMetadata(
    val uid: String,
    val title: String,
    val description: String,
    val location: String
)

/** Stores the timing values used to place one campus event on the calendar. */
private data class CampusEventTiming(
    val startField: IcsField,
    val endField: IcsField?,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean
)
