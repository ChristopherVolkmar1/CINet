package com.example.cinet

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository class to handle fetching and parsing news from the CI View RSS feed.
 */
class NewsRepository {
    private val client = OkHttpClient()
    // The official RSS feed URL for The CI View newspaper
    private val rssUrl = "https://civiewnews.com/feed/"

    /**
     * Fetches the latest news articles from the website.
     * Runs on a background thread (Dispatchers.IO) to keep the UI smooth.
     */
    suspend fun fetchLatestNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(rssUrl).build()
            val response = client.newCall(request).execute()
            val inputStream = response.body?.byteStream() ?: return@withContext emptyList()
            // Convert the raw XML response into a list of NewsArticle objects
            parseRss(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Parses the XML data from the RSS feed.
     */
    private fun parseRss(inputStream: InputStream): List<NewsArticle> {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)
        val articles = mutableListOf<NewsArticle>()
        var eventType = parser.eventType
        var currentTag: String? = null

        var title = ""
        var link = ""
        var pubDate = ""
        var description = ""

        // Loop through the XML tags
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    // Reset variables when we start a new 'item' (article)
                    if (currentTag == "item") {
                        title = ""
                        link = ""
                        pubDate = ""
                        description = ""
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    if (text.isNotEmpty()) {
                        // Fill in article details based on the current tag
                        when (currentTag) {
                            "title" -> title = text
                            "link" -> link = text
                            "pubDate" -> pubDate = formatDate(text)
                            "description" -> description = cleanDescription(text)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    // When we reach the end of an 'item', add the article to our list
                    if (parser.name == "item") {
                        articles.add(NewsArticle(title, pubDate, description, link))
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }
        return articles
    }

    /**
     * Formats the raw RSS date string into something more readable (e.g., "Oct 24, 2023").
     */
    private fun formatDate(rawDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val date = inputFormat.parse(rawDate)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            rawDate // Return the original if parsing fails
        }
    }

    /**
     * Strips HTML tags from the article description and shortens it for a preview.
     */
    private fun cleanDescription(html: String): String {
        // Simple regex to remove <tags> like <b> or <p>
        return html.replace(Regex("<[^>]*>"), "").trim().take(150) + "..."
    }
}