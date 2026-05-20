package com.example.cinet.feature.messages.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.remote.canvas.CanvasMessage
import com.example.cinet.feature.messages.canvas.viewmodel.CanvasMessagingViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Thread view for one Canvas conversation. Top to bottom:
 *   - Header with subject + participant list
 *   - Scrollable list of messages (oldest at top, newest at bottom)
 *   - Reply box pinned to the bottom with a send button
 *
 * Tap "back" via the existing top bar / system back to return to the inbox —
 * this screen doesn't render its own back chrome since the navigation
 * scaffold owns it.
 */
@Composable
fun CanvasConversationScreen(
    viewModel: CanvasMessagingViewModel = viewModel()
) {
    val state by viewModel.threadState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to the latest message whenever the list grows.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Subject + participants header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = state.subject.ifBlank { "(no subject)" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.participantNames.isNotBlank()) {
                        Text(
                            text = state.participantNames,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Message list — takes remaining space above the reply box.
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
            ) {
                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.messages.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No messages in this conversation.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.messages, key = { it.id }) { message ->
                                val isMine = state.currentUserId != null &&
                                        message.authorId == state.currentUserId
                                MessageBubble(message = message, isMine = isMine)
                            }
                        }
                    }
                }
            }

            // Reply error banner — shown above the input when the last send failed.
            state.sendError?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Reply input row — pinned at bottom.
            ReplyBox(
                value = state.replyDraft,
                onValueChange = viewModel::onReplyDraftChange,
                onSend = viewModel::sendReply,
                isSending = state.isSending
            )
        }
    }
}

/**
 * One message bubble. Right-aligned/primary-colored when sent by the
 * current user, left-aligned/surface-colored otherwise — matches the
 * convention used in the existing chat UI.
 */
@Composable
private fun MessageBubble(message: CanvasMessage, isMine: Boolean) {
    val bubbleColor = if (isMine)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            // Author name + timestamp header for incoming messages only —
            // for "mine", the alignment alone signals authorship.
            if (!isMine) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.authorName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val stamp = formatBubbleTimestamp(message.createdAtIso)
                    if (stamp.isNotBlank()) {
                        Text(
                            text = " · $stamp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = message.body,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    // Attachment placeholder — names only, no download.
                    if (message.attachmentNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        message.attachmentNames.forEach { name ->
                            Text(
                                text = "📎 $name",
                                color = textColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                        Text(
                            text = "(open in Canvas to download)",
                            color = textColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            // Trailing timestamp for "mine" — below the bubble so the bubble
            // shape stays clean.
            if (isMine) {
                val stamp = formatBubbleTimestamp(message.createdAtIso)
                if (stamp.isNotBlank()) {
                    Text(
                        text = stamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Simple reply input pinned to the bottom of the screen. Intentionally much
 * simpler than the friend-to-friend [MessageBox] — Canvas messages are
 * text-only in this v1 (no attachments, no invite shortcuts).
 */
@Composable
private fun ReplyBox(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Reply") },
                modifier = Modifier.weight(1f),
                enabled = !isSending,
                maxLines = 5,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (value.isBlank() || isSending)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (value.isBlank())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun formatBubbleTimestamp(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(iso)
        DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } catch (ex: Exception) {
        ""
    }
}
