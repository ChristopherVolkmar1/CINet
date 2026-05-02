package com.example.cinet.feature.calendar.event

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/** Downloads the live campus ICS feed and converts it into calendar events. */
class CampusEventFeedRepository(
    private val parser: CampusEventFeedParser = CampusEventFeedParser()
) {
    private val feedUrls = listOf(

        "https://25livepub.collegenet.com/calendars/csuci-calendar-of-events.ics"
    )

    /** Tries each known feed URL until one returns a readable event list. */
    suspend fun loadCampusEvents(): List<EventItem> = withContext(Dispatchers.IO) {
        for (feedUrl in feedUrls) {
            val parsedEvents = loadFeedFromUrl(feedUrl)
            if (parsedEvents != null) {
                return@withContext parsedEvents
            }
        }

        Log.e("CampusEventFeed", "All campus event feed URLs failed.")
        emptyList()
    }

    /** Downloads and parses one feed URL, or returns null when that URL fails. */
    private fun loadFeedFromUrl(feedUrl: String): List<EventItem>? {
        return try {
            val icsText = downloadFeed(feedUrl)
            parser.parse(icsText).takeIf { it.isNotEmpty() }
        } catch (error: Exception) {
            Log.e("CampusEventFeed", "Failed to load feed: $feedUrl", error)
            null
        }
    }

    /** Downloads one ICS file as plain text. */
    private fun downloadFeed(feedUrl: String): String {
        val connection = openFeedConnection(feedUrl)

        try {
            throwIfRequestFailed(connection)
            return readResponseBody(connection)
        } finally {
            connection.disconnect()
        }
    }

    /** Opens and configures the HTTP connection used for the feed request. */
    private fun openFeedConnection(feedUrl: String): HttpURLConnection {
        return (URL(feedUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "text/calendar,text/plain,*/*")
            instanceFollowRedirects = true
        }
    }

    /** Throws an error when the feed request returns a non-success HTTP code. */
    private fun throwIfRequestFailed(connection: HttpURLConnection) {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("Feed request failed with HTTP $responseCode")
        }
    }

    /** Reads the full ICS response body into one string. */
    private fun readResponseBody(connection: HttpURLConnection): String {
        return BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
            buildString {
                var line = reader.readLine()
                while (line != null) {
                    append(line).append("\n")
                    line = reader.readLine()
                }
            }
        }
    }
}
