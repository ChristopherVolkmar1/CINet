package com.example.cinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.Message
import com.example.cinet.data.remote.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun ConversationScreen(
    conversation: Conversation,
    onBack: () -> Unit,
) {
    val repository = remember { SocialRepository() }
    val calendarRepository = remember { CalendarFirestoreRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var showStudyInviteDialog by remember { mutableStateOf(false) }
    var showEventInviteDialog by remember { mutableStateOf(false) }
    var myScheduleItems by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }

    // State for accept dialogs — pre-filled from invite metadata
    var acceptingStudyInvite by remember { mutableStateOf<Message?>(null) }
    var acceptingEventInvite by remember { mutableStateOf<Message?>(null) }

    // Pre-filled fields for study invite acceptance
    var acceptSessionClassName by remember { mutableStateOf("") }
    var acceptSessionTopic by remember { mutableStateOf("") }
    var acceptSessionDate by remember { mutableStateOf("") }
    var acceptSessionTime by remember { mutableStateOf("") }
    var acceptSessionLocation by remember { mutableStateOf("") }

    // Pre-filled fields for event invite acceptance
    var acceptEventName by remember { mutableStateOf("") }
    var acceptEventDate by remember { mutableStateOf("") }
    var acceptEventTime by remember { mutableStateOf("") }
    var acceptEventLocation by remember { mutableStateOf("") }

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
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack) { Text("Back") }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = conversationTitle, style = MaterialTheme.typography.titleLarge)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            repository.getMyScheduleItems().onSuccess { myScheduleItems = it }
                            showStudyInviteDialog = true
                        }
                    }
                ) {
                    Text("Study Invite", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = { showEventInviteDialog = true }) {
                    Text("Event Invite", style = MaterialTheme.typography.labelSmall)
                }
            }

            HorizontalDivider()

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val alreadyResponded = message.metadata["response"] != null
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == currentUid,
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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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

    // Study invite sender dialog
    if (showStudyInviteDialog) {
        StudyInviteDialog(
            existingItems = myScheduleItems,
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

    // Event invite sender dialog
    if (showEventInviteDialog) {
        EventInviteSenderDialog(
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

    // Study invite accept dialog — pre-filled, user can edit before saving
    if (acceptingStudyInvite != null) {
        StudySessionDialog(
            editingSession = null,
            date = acceptSessionDate,
            className = acceptSessionClassName,
            onClassNameChange = { acceptSessionClassName = it },
            topic = acceptSessionTopic,
            onTopicChange = { acceptSessionTopic = it },
            startTime = acceptSessionTime,
            location = acceptSessionLocation,
            onLocationChange = { acceptSessionLocation = it },
            onPickStartTime = { openTimePicker(context) { picked -> acceptSessionTime = picked } },
            onDismiss = { acceptingStudyInvite = null },
            onConfirm = {
                if (acceptSessionClassName.isNotBlank() && acceptSessionTopic.isNotBlank() && acceptSessionDate.isNotBlank()) {
                    scope.launch {
                        calendarRepository.addStudySession(acceptSessionDate, acceptSessionClassName, acceptSessionTopic, acceptSessionTime, acceptSessionLocation)
                        repository.sendMessage(conversation.id, "Accepted your study invite!", "text")
                        acceptingStudyInvite = null
                    }
                }
            },
            onDelete = null
        )
    }

    // Event invite accept dialog — pre-filled, user can edit before saving
    if (acceptingEventInvite != null) {
        EventItemDialog(
            editingEvent = null,
            date = acceptEventDate,
            eventName = acceptEventName,
            onEventNameChange = { acceptEventName = it },
            eventTime = acceptEventTime,
            location = acceptEventLocation,
            onLocationChange = { acceptEventLocation = it },
            onPickTime = { openTimePicker(context) { picked -> acceptEventTime = picked } },
            onDismiss = { acceptingEventInvite = null },
            onConfirm = {
                if (acceptEventName.isNotBlank() && acceptEventDate.isNotBlank()) {
                    scope.launch {
                        calendarRepository.addEvent(acceptEventDate, acceptEventName, acceptEventTime, acceptEventLocation)
                        repository.sendMessage(conversation.id, "Accepted your event invite!", "text")
                        acceptingEventInvite = null
                    }
                }
            },
            onDelete = null
        )
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    onAccept: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
}