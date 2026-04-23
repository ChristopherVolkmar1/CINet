package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/** Renders event description text and makes any detected links tappable. */
@Composable
fun CampusEventLinkText(text: String) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedText = remember(text, linkColor) { buildLinkedText(text, linkColor) }

    ClickableText(
        text = annotatedText,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(tag = URL_TAG, start = offset, end = offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(normalizeUrl(it.item)) }
        }
    )
}

/** Converts plain description text into an annotated string with clickable URL spans. */
private fun buildLinkedText(text: String, linkColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0

        URL_REGEX.findAll(text).forEach { match ->
            appendPlainSegment(text, currentIndex, match.range.first)
            appendUrlSegment(cleanUrl(match.value), linkColor)
            currentIndex = match.range.last + 1
        }

        appendPlainSegment(text, currentIndex, text.length)
    }
}

/** Appends one non-link text segment to the annotated string builder. */
private fun AnnotatedString.Builder.appendPlainSegment(
    text: String,
    startIndex: Int,
    endIndex: Int
) {
    if (startIndex >= endIndex) {
        return
    }

    append(text.substring(startIndex, endIndex))
}

/** Appends one clickable URL segment to the annotated string builder. */
private fun AnnotatedString.Builder.appendUrlSegment(url: String, linkColor: Color) {
    if (url.isBlank()) {
        return
    }

    pushStringAnnotation(tag = URL_TAG, annotation = url)
    pushStyle(
        SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Medium
        )
    )
    append(url)
    pop()
    pop()
}

/** Removes punctuation that is commonly attached to the end of copied links. */
private fun cleanUrl(rawUrl: String): String {
    return rawUrl.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
}

/** Adds a scheme when the detected link starts with www instead of http or https. */
private fun normalizeUrl(url: String): String {
    return if (url.startsWith("www.", ignoreCase = true)) {
        "https://$url"
    } else {
        url
    }
}

private const val URL_TAG = "campus_event_url"
private val URL_REGEX = Regex("""((https?://|www\.)[^\s]+)""")
