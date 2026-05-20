package com.example.cinet.feature.messages.canvas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.remote.canvas.CanvasConversation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.cinet.feature.messages.canvas.viewmodel.CanvasMessagingViewModel

/**
 * Inbox list of Canvas conversations. Renders one row per conversation with
 * participant names, subject, last-message preview, and a relative timestamp.
 * Tapping a row opens the thread view.
 *
 * No unread indicator: CINet does not flip Canvas's read state when the user
 * opens a thread, so an unread dot would persist forever and confuse the user.
 * The CanvasConversation model still exposes `isUnread` if a future version
 * decides to manage read state via Canvas's PUT /conversations/:id endpoint.
 */
@Composable
fun CanvasInboxScreen(
    onOpenConversation: (CanvasConversation) -> Unit,
    viewModel: CanvasMessagingViewModel = viewModel()
) {
    val state by viewModel.inboxState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInbox()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.conversations.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.conversations.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your Canvas inbox is empty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.conversations, key = { it.id }) { conversation ->
                            CanvasConversationRow(
                                conversation = conversation,
                                onClick = { onOpenConversation(conversation) }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasConversationRow(
    conversation: CanvasConversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.participantNames,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (conversation.subject.isNotBlank()) {
                Text(
                    text = conversation.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (conversation.lastMessagePreview.isNotBlank()) {
                Text(
                    text = conversation.lastMessagePreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val timeStr = formatInboxTimestamp(conversation.lastMessageAtIso)
        if (timeStr.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatInboxTimestamp(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(iso)
        val zone = ZoneId.systemDefault()
        val localDate = instant.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(localDate, today)

        when {
            daysAgo == 0L -> DateTimeFormatter.ofPattern("h:mm a", Locale.US)
                .withZone(zone).format(instant)
            daysAgo in 1..6 -> DateTimeFormatter.ofPattern("EEE", Locale.US)
                .withZone(zone).format(instant)
            else -> DateTimeFormatter.ofPattern("MMM d", Locale.US)
                .withZone(zone).format(instant)
        }
    } catch (ex: Exception) {
        ""
    }
}
