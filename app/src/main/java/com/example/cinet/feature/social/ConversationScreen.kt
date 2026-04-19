package com.example.cinet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.Message
import com.example.cinet.data.remote.SocialRepository
import com.example.cinet.feature.calendar.calendarFiles.CalendarFirestoreRepository
import com.example.cinet.feature.calendar.event.EventItem
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import com.example.cinet.feature.calendar.study.StudySession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ConversationScreen(
    conversation: Conversation,
    onBack: () -> Unit,
) {
    val repository = remember { SocialRepository() }
    val calendarRepository = remember { CalendarFirestoreRepository() }
    val scope = rememberCoroutineScope()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var showStudyInviteDialog by remember { mutableStateOf(false) }
    var showEventInviteDialog by remember { mutableStateOf(false) }
    var myScheduleItems by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }
    var myStudySessions by remember { mutableStateOf<List<StudySession>>(emptyList()) }
    var myEvents by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var otherUserPhotoUrl by remember { mutableStateOf("") } // header avatar photo
    var currentUserPhotoUrl by remember { mutableStateOf("") } // current user's avatar

    // Load both participants' photos on open
    LaunchedEffect(conversation.id) {
        val otherUid = conversation.participantIds.firstOrNull { it != currentUid } ?: return@LaunchedEffect

        val otherSnapshot = FirebaseFirestore.getInstance()
            .collection("users").document(otherUid).get().await()
        otherUserPhotoUrl = otherSnapshot.getString("photoUrl") ?: ""

        // Also load current user's photo for sent message avatars
        val currentSnapshot = FirebaseFirestore.getInstance()
            .collection("users").document(currentUid).get().await()
        currentUserPhotoUrl = currentSnapshot.getString("photoUrl") ?: ""
    }

    DisposableEffect(conversation.id) {
        val listener = FirebaseFirestore.getInstance()
            .collection("conversations")
            .document(conversation.id)
            .collection("messages")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.toObjects(Message::class.java)
                        .sortedBy { it.createdAt }
                }
            }
        onDispose { listener.remove() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val conversationTitle = if (conversation.isGroup) {
        conversation.groupName
    } else {
        conversation.participantNicknames.entries
            .firstOrNull { it.key != currentUid }?.value ?: "Conversation"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header — shows avatar and conversation title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack) { Text("Back") }
                Spacer(modifier = Modifier.width(12.dp))

                // Avatar — shows Google photo if available, otherwise initials
                val headerPhoto = otherUserPhotoUrl.takeIf { it.isNotBlank() }
                if (headerPhoto != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(headerPhoto)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversationTitle.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))
                Text(text = conversationTitle, style = MaterialTheme.typography.titleLarge)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Study invite — loads assignments and study sessions only
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repository.getMyScheduleItems().onSuccess { myScheduleItems = it }
                            repository.getMyStudySessions().onSuccess { myStudySessions = it }
                            showStudyInviteDialog = true
                        }
                    }
                ) {
                    Text("Study Invite", style = MaterialTheme.typography.labelSmall)
                }
                // Event invite — loads existing events for picker
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repository.getMyEvents().onSuccess { myEvents = it }
                            showEventInviteDialog = true
                        }
                    }
                ) {
                    Text("Event Invite", style = MaterialTheme.typography.labelSmall)
                }
            }

            HorizontalDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val alreadyResponded = message.metadata["response"] != null
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == currentUid,
                        currentUserPhotoUrl = currentUserPhotoUrl,
                        onAccept = if (!alreadyResponded && message.senderId != currentUid &&
                            (message.type == "study_invite" || message.type == "event_invite")) {
                            {
                                scope.launch {
                                    if (message.type == "study_invite") {
                                        val className = message.metadata["className"] ?: ""
                                        val topic = message.metadata["topic"] ?: ""
                                        val date = message.metadata["date"] ?: ""
                                        val time = message.metadata["time"] ?: ""
                                        val location = message.metadata["location"] ?: ""
                                        android.util.Log.d("CalendarSave", "Saving study session: $className $topic $date $time")
                                        if (date.isNotBlank()) {
                                            android.util.Log.d("CalendarSave", "metadata: ${message.metadata}")
                                            android.util.Log.d("CalendarSave", "date: ${message.metadata["date"]}")
                                            calendarRepository.addStudySession(date, className, topic, time, location)
                                            android.util.Log.d("CalendarSave", "Study session saved successfully")
                                        } else {
                                            android.util.Log.e("CalendarSave", "Date is blank — metadata: ${message.metadata}")
                                        }
                                    } else {
                                        val name = message.metadata["name"] ?: ""
                                        val date = message.metadata["date"] ?: ""
                                        val time = message.metadata["time"] ?: ""
                                        val location = message.metadata["location"] ?: ""
                                        if (date.isNotBlank()) {
                                            calendarRepository.addEvent(date, name, time, location)
                                        }
                                    }
                                    repository.respondToInvite(conversation.id, message.id, "accepted")
                                    repository.sendMessage(conversation.id, "Accepted your invite!", "text")
                                }
                            }
                        } else null,
                        onDecline = if (!alreadyResponded && message.senderId != currentUid &&
                            (message.type == "study_invite" || message.type == "event_invite")) {
                            {
                                scope.launch {
                                    repository.respondToInvite(conversation.id, message.id, "declined")
                                    repository.sendMessage(conversation.id, "Declined your invite.", "text")
                                }
                            }
                        } else null
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Message") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val content = messageInput.trim()
                        if (content.isNotBlank()) {
                            scope.launch {
                                repository.sendMessage(conversation.id, content)
                                messageInput = ""
                            }
                        }
                    }
                ) { Text("Send") }
            }
        }
    }

    // Study invite sender dialog — shows assignments and study sessions
    if (showStudyInviteDialog) {
        StudyInviteDialog(
            existingItems = myScheduleItems,
            existingStudySessions = myStudySessions,
            onDismiss = { showStudyInviteDialog = false },
            onSendExisting = { item ->
                scope.launch {
                    val content = "Study invite: ${item.className} — ${item.assignmentName} on ${item.date} at ${item.dueTime}"
                    repository.sendMessage(
                        conversationId = conversation.id,
                        content = content,
                        type = "study_invite",
                        metadata = mapOf(
                            "className" to item.className,
                            "topic" to item.assignmentName,
                            "date" to item.date,
                            "time" to item.dueTime,
                            "location" to ""
                        )
                    )
                    showStudyInviteDialog = false
                }
            },
            onSendExistingSession = { session ->
                scope.launch {
                    val content = "Study invite: ${session.className} — ${session.topic} on ${session.date} at ${session.startTime}"
                    repository.sendMessage(
                        conversationId = conversation.id,
                        content = content,
                        type = "study_invite",
                        metadata = mapOf(
                            "className" to session.className,
                            "topic" to session.topic,
                            "date" to session.date,
                            "time" to session.startTime,
                            "location" to session.location
                        )
                    )
                    showStudyInviteDialog = false
                }
            },
            onSendNew = { cls, topic, date, time ->
                scope.launch {
                    val content = "Study invite: $cls — $topic on $date at $time"
                    repository.sendMessage(
                        conversationId = conversation.id,
                        content = content,
                        type = "study_invite",
                        metadata = mapOf("className" to cls, "topic" to topic, "date" to date, "time" to time, "location" to "")
                    )
                    showStudyInviteDialog = false
                }
            }
        )
    }

    // Event invite sender dialog — shows existing events with manual form option
    if (showEventInviteDialog) {
        EventInviteSenderDialog(
            existingEvents = myEvents,
            onDismiss = { showEventInviteDialog = false },
            onSend = { name, date, time, location ->
                scope.launch {
                    val content = "Event invite: $name on $date at $time"
                    repository.sendMessage(
                        conversationId = conversation.id,
                        content = content,
                        type = "event_invite",
                        metadata = mapOf("name" to name, "date" to date, "time" to time, "location" to location)
                    )
                    showEventInviteDialog = false
                }
            }
        )
    }
}

// Frontend team: restyle this bubble however you want
@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    currentUserPhotoUrl: String = "", // current user's photo for sent messages
    onAccept: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Avatar on the left for incoming messages
        if (!isCurrentUser) {
            val photoUrl = message.senderPhotoUrl.takeIf { it.isNotBlank() }
            if (photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                // Fallback initials if no photo available
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.senderNickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            if (!isCurrentUser) {
                Text(
                    text = message.senderNickname,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(4.dp)
            )

            val response = message.metadata["response"]
            when {
                // Already responded — show status label instead of buttons
                response == "accepted" -> Text(
                    text = "✓ Accepted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
                response == "declined" -> Text(
                    text = "✗ Declined",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                // Not yet responded — show buttons
                onAccept != null && onDecline != null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAccept) { Text("Accept") }
                        OutlinedButton(onClick = onDecline) { Text("Decline") }
                    }
                }
            }
        }

        // Avatar on the right for sent messages
        if (isCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            val photoUrl = currentUserPhotoUrl.takeIf { it.isNotBlank() }
            if (photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Your profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                // Fallback initials if no photo available
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.senderNickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}