package com.example.cinet.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import com.example.cinet.feature.map.SearchBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.tasks.await

@Composable
fun ConversationsListScreen(
    onOpenConversation: (Conversation) -> Unit,
    onNewConversation: () -> Unit,
    onOpenFriends: () -> Unit,
    // Maps conversationId -> timestamp when it was last opened this session.
    // A message arriving AFTER that timestamp shows a dot even for opened convos.
    openedConversationTimestamps: Map<String, Long> = emptyMap(),
    // Called once on fresh install / emulator wipe when openedConversationTimestamps
    // is empty but conversations already exist. Seeds all conversation IDs with "now"
    // so existing threads don't incorrectly appear unread after a data clear.
    onSeedTimestamps: (List<String>) -> Unit = {},
) {
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val repository = remember { SocialRepository() }

    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var pendingRequestCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Fresh-install / emulator-wipe guard.
    var hasSeeded by remember { mutableStateOf(false) }
    LaunchedEffect(conversations) {
        if (!hasSeeded && conversations.isNotEmpty() && openedConversationTimestamps.isEmpty()) {
            onSeedTimestamps(conversations.map { it.id })
            hasSeeded = true
        }
    }

    // Real-time listener — updates automatically when messages arrive
    DisposableEffect(currentUid) {
        val listener = FirebaseFirestore.getInstance()
            .collection("conversations")
            .whereArrayContains("participantIds", currentUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    conversations = snapshot.toObjects(Conversation::class.java)
                        .filter { it.active }
                        .sortedByDescending { it.lastUpdated?.time ?: 0L }
                    isLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    // One-shot load for pending request badge and friends for local nicknames
    LaunchedEffect(Unit) {
        repository.getPendingRequests().onSuccess { pendingRequestCount = it.size }
        repository.getFriends().onSuccess { friends = it }
    }

    // Map friends to their local nicknames for fast lookup
    val localNicknames = remember(friends) {
        friends.associate { it.uid to it.localNickname }.filterValues { it != null }
    }

    // Required for searching
    val textFieldState = rememberTextFieldState()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Friends button with pending request badge
                BadgedBox(
                    badge = {
                        if (pendingRequestCount > 0) {
                            Badge { Text(pendingRequestCount.toString()) }
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    IconButton(onClick = onOpenFriends) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Friends",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Compose new conversation
                IconButton(onClick = onNewConversation) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "New conversation",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (conversations.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the pencil icon to start a conversation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = onNewConversation) {
                            Text("New Message")
                        }
                    }
                }
            } else {
                val query = textFieldState.text.toString()
                val filteredConversations = remember(query, conversations, localNicknames) {
                    if (query.isBlank()) conversations
                    else conversations.filter { conversation ->
                        // Match by conversation/group name
                        val nameMatch = if (conversation.isGroup) {
                            conversation.groupName.contains(query, ignoreCase = true)
                        } else {
                            val otherUid = conversation.participantIds.firstOrNull { it != currentUid }
                            val nickname = localNicknames[otherUid] 
                                ?: conversation.participantNicknames.filterKeys { it != currentUid }.values.firstOrNull() 
                                ?: ""
                            nickname.contains(query, ignoreCase = true)
                        }
                        // Also match against the latest message preview
                        val messageMatch = conversation.lastMessage.contains(query, ignoreCase = true)
                        nameMatch || messageMatch
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(8.dp)) {
                    SearchBar(
                        placeholderText = "Search conversations...",
                        textFieldState = textFieldState,
                        searchResults = emptyList(),
                        onSearch = { query ->
                            textFieldState.edit { replace(0, length, query) }
                        }
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredConversations, key = { it.id }) { conversation ->
                        val openedAt = openedConversationTimestamps[conversation.id] ?: 0L
                        val hasUnread = (conversation.lastUpdated?.time ?: 0L) > openedAt &&
                                conversation.lastMessage.isNotBlank() &&
                                conversation.lastSenderId != currentUid
                                
                        val otherUid = if (!conversation.isGroup) conversation.participantIds.firstOrNull { it != currentUid } else null
                        val localNickname = otherUid?.let { localNicknames[it] }

                        ConversationListItem(
                            conversation = conversation,
                            currentUid = currentUid,
                            hasUnread = hasUnread,
                            highlightQuery = query.trim(),
                            localNickname = localNickname,
                            onClick = { onOpenConversation(conversation) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationListItem(
    conversation: Conversation,
    currentUid: String,
    onClick: () -> Unit,
    hasUnread: Boolean = false,
    highlightQuery: String = "",
    localNickname: String? = null,
) {
    val displayName = if (conversation.isGroup) {
        conversation.groupName.ifBlank { "Group Chat" }
    } else {
        localNickname ?: conversation.participantNicknames.entries
            .firstOrNull { it.key != currentUid }?.value ?: "Unknown"
    }

    val otherUid = if (!conversation.isGroup) {
        conversation.participantIds.firstOrNull { it != currentUid } ?: ""
    } else ""

    // For DMs, try to load the other user's photo
    var otherPhotoUrl by remember(otherUid) { mutableStateOf("") }
    LaunchedEffect(otherUid) {
        if (otherUid.isNotBlank()) {
            try {
                val snap = FirebaseFirestore.getInstance()
                    .collection("users").document(otherUid).get().await()
                otherPhotoUrl = snap.getString("photoUrl") ?: ""
            } catch (_: Exception) {}
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unread indicator dot
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(
                    if (hasUnread) MaterialTheme.colorScheme.primary
                    else androidx.compose.ui.graphics.Color.Transparent
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Avatar
        ConversationAvatar(
            isGroup = conversation.isGroup,
            displayName = displayName,
            photoUrl = otherPhotoUrl,
            participantCount = conversation.participantIds.size
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            @Composable
            fun highlighted(text: String): androidx.compose.ui.text.AnnotatedString =
                remember(text, highlightQuery) {
                    buildAnnotatedString {
                        if (highlightQuery.isBlank()) {
                            append(text)
                        } else {
                            val lower = text.lowercase()
                            val q = highlightQuery.lowercase()
                            var cursor = 0
                            while (cursor < text.length) {
                                val hit = lower.indexOf(q, cursor)
                                if (hit == -1) { append(text.substring(cursor)); break }
                                append(text.substring(cursor, hit))
                                withStyle(SpanStyle(
                                    background = Color(0xFFFFEB3B),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                )) { append(text.substring(hit, hit + q.length)) }
                                cursor = hit + q.length
                            }
                        }
                    }
                }

            Text(
                text = highlighted(displayName),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (conversation.lastMessage.isNotBlank()) {
                Text(
                    text = highlighted(conversation.lastMessage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Timestamp
        val timeStr = formatConversationTime(conversation.lastUpdated)
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

@Composable
fun ConversationLocationItem(
    conversation: Conversation,
    currentUid: String,
    onClick: () -> Unit,
    hasUnread: Boolean = false,
    highlightQuery: String = "",
) {
    val displayName = if (conversation.isGroup) {
        conversation.groupName.ifBlank { "Group Chat" }
    } else {
        conversation.participantNicknames.entries
            .firstOrNull { it.key != currentUid }?.value ?: "Unknown"
    }

    val otherUid = if (!conversation.isGroup) {
        conversation.participantIds.firstOrNull { it != currentUid } ?: ""
    } else ""

    // For DMs, try to load the other user's photo
    var otherPhotoUrl by remember(otherUid) { mutableStateOf("") }
    LaunchedEffect(otherUid) {
        if (otherUid.isNotBlank()) {
            try {
                val snap = FirebaseFirestore.getInstance()
                    .collection("users").document(otherUid).get().await()
                otherPhotoUrl = snap.getString("photoUrl") ?: ""
            } catch (_: Exception) {}
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .padding(end = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        ConversationAvatar(
            isGroup = conversation.isGroup,
            displayName = displayName,
            photoUrl = otherPhotoUrl,
            participantCount = conversation.participantIds.size
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ConversationAvatar(
    isGroup: Boolean,
    displayName: String,
    photoUrl: String,
    participantCount: Int,
) {
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val avatarColor = MaterialTheme.colorScheme.secondaryContainer

    if (!isGroup && photoUrl.isNotBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(avatarColor)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun formatConversationTime(date: Date?): String {
    date ?: return ""
    val now = System.currentTimeMillis()
    val diff = now - date.time
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000}m"
        diff < 86_400_000L -> "${diff / 3_600_000}h"
        diff < 172_800_000L -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.US).format(date)
    }
}