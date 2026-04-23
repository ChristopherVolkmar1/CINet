package com.example.cinet.feature.calendar.event

import androidx.core.text.HtmlCompat

/** Cleans raw ICS text so campus events display as normal readable text. */
object CampusEventTextFormatter {

    /** Converts escaped ICS text and inline HTML into clean display text. */
    fun sanitize(value: String): String {
        val unescapedText = unescapeIcsText(value)
        val linkReadyText = preserveAnchorLinks(unescapedText)
        val htmlReadyText = replaceLineBreaksForHtml(linkReadyText)
        val plainText = convertHtmlToPlainText(htmlReadyText)
        return normalizeWhitespace(plainText)
    }

    /** Converts common ICS escape sequences into regular characters. */
    private fun unescapeIcsText(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
            .trim()
    }


    /** Converts HTML anchor tags into plain text that keeps the original URL visible. */
    private fun preserveAnchorLinks(value: String): String {
        return ANCHOR_TAG_REGEX.replace(value) { match ->
            val url = match.groupValues.getOrNull(1).orEmpty().trim()
            val label = match.groupValues.getOrNull(2).orEmpty().trim()

            when {
                url.isBlank() -> label
                label.isBlank() -> url
                label.equals(url, ignoreCase = true) -> url
                else -> "$label ($url)"
            }
        }
    }

    /** Replaces plain new lines with HTML line breaks before HTML decoding. */
    private fun replaceLineBreaksForHtml(value: String): String {
        return value.replace("\n", "<br>")
    }

    /** Decodes any HTML entities and strips HTML tags from the text. */
    private fun convertHtmlToPlainText(value: String): String {
        return HtmlCompat
            .fromHtml(value, HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toString()
    }

    /** Removes extra spacing so the dialog and event cards stay easy to read. */
    private fun normalizeWhitespace(value: String): String {
        return value
            .replace('\u00A0', ' ')
            .replace(Regex("[ \t]*\n[ \t]*"), "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}


private val ANCHOR_TAG_REGEX = Regex("""<a\b[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
