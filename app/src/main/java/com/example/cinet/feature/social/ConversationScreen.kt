package com.example.cinet.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.example.cinet.feature.calendar.study.*
import com.example.cinet.feature.calendar.event.*

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
    var showRemoveFriendDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    // Local override so rename is reflected immediately without re-navigation
    var displayGroupName by remember { mutableStateOf(conversation.groupName) }
    var myScheduleItems by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }
    var myStudySessions by remember { mutableStateOf<List<StudySession>>(emptyList()) }
    var myEvents by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var otherUserPhotoUrl by remember { mutableStateOf("") }
    var currentUserPhotoUrl by remember { mutableStateOf("") }

    val otherUid = conversation.participantIds.firstOrNull { it != currentUid } ?: ""

    val conversationTitle = if (conversation.isGroup) {
        displayGroupName.ifBlank { "Group Chat" }
    } else {
        conversation.participantNicknames.entries
            .firstOrNull { it.key != currentUid }?.value ?: "Conversation"
    }

    // Load both participants' photos on open
    LaunchedEffect(conversation.id) {
        if (otherUid.isNotBlank()) {
            val otherSnapshot = FirebaseFirestore.getInstance()
                .collection("users").document(otherUid).get().await()
            otherUserPhotoUrl = otherSnapshot.getString("photoUrl") ?: ""
        }
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

    // Remove Friend confirmation dialog
    if (showRemoveFriendDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveFriendDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove $conversationTitle as a friend?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveFriendDialog = false
                        scope.launch {
                            repository.removeFriend(otherUid)
                            onBack()
                        }
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveFriendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename group dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Group") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotBlank()) {
                            showRenameDialog = false
                            scope.launch {
                                repository.renameConversation(conversation.id, newName)
                                displayGroupName = newName
                            }
                        }
                    },
                    enabled = renameInput.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(12.dp))

                // Avatar — uses secondaryContainer for consistent green branding
                val headerPhoto = otherUserPhotoUrl.takeIf { it.isNotBlank() && !conversation.isGroup }
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
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversationTitle.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Group name is tappable to rename; DM name is static
                if (conversation.isGroup) {
                    Text(
                        text = conversationTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                renameInput = displayGroupName
                                showRenameDialog = true
                            }
                    )
                } else {
                    Text(
                        text = conversationTitle,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Remove Friend button — only for direct (non-group) conversations
                if (!conversation.isGroup && otherUid.isNotBlank()) {
                    OutlinedButton(
                        onClick = { showRemoveFriendDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Remove Friend", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    // Add to sender's calendar as a study session (distinct from the assignment entry)
                    if (item.date.isNotBlank()) {
                        calendarRepository.addStudySession(item.date, item.className, item.assignmentName, item.dueTime, "")
                    }
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
                    // Session is already in sender's studySessions — no add needed
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
                    // New session — save to sender's calendar immediately
                    if (date.isNotBlank()) {
                        calendarRepository.addStudySession(date, cls, topic, time, "")
                    }
                    showStudyInviteDialog = false
                }
            }
        )
    }

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
                    // Save to sender's calendar so they don't have to accept their own invite
                    if (date.isNotBlank()) {
                        calendarRepository.addEvent(date, name, time, location)
                    }
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
    currentUserPhotoUrl: String = "",
    onAccept: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
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
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            if (message.type == "study_invite" || message.type == "event_invite") {
                InviteBubble(
                    message = message,
                    isCurrentUser = isCurrentUser,
                    onAccept = onAccept,
                    onDecline = onDecline,
                )
            } else {
                val bubbleColor = if (isCurrentUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (isCurrentUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isCurrentUser) 16.dp else 4.dp,
                        topEnd = if (isCurrentUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    color = bubbleColor,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
            }
        }

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
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Card-style bubble for study_invite and event_invite messages.
 * Shows type icon, title, subtitle (study only), date/time/location rows,
 * and either Accept/Decline buttons or the recorded response.
 */
@Composable
fun InviteBubble(
    message: Message,
    isCurrentUser: Boolean,
    onAccept: (() -> Unit)?,
    onDecline: (() -> Unit)?,
) {
    val isStudy = message.type == "study_invite"
    val meta = message.metadata

    val typeLabel  = if (isStudy) "Study Session" else "Event Invite"
    val typeIcon   = if (isStudy) Icons.Default.School else Icons.Default.Event
    val title      = if (isStudy) meta["className"] ?: "" else meta["name"] ?: ""
    val subtitle   = if (isStudy) meta["topic"] ?: "" else ""
    val date       = meta["date"] ?: ""
    val time       = meta["time"] ?: ""
    val location   = meta["location"] ?: ""
    val response   = meta["response"]

    val cardShape = RoundedCornerShape(
        topStart = if (isCurrentUser) 16.dp else 4.dp,
        topEnd   = if (isCurrentUser) 4.dp  else 16.dp,
        bottomStart = 16.dp,
        bottomEnd   = 16.dp,
    )

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.widthIn(min = 220.dp, max = 280.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Header: icon + type label ────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = typeLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                )
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                thickness = 0.5.dp,
            )
            Spacer(Modifier.height(8.dp))

            // ── Title + subtitle ─────────────────────────────────────
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Detail rows: date / time / location ──────────────────
            @Composable
            fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
                if (text.isBlank()) return
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    )
                }
            }

            DetailRow(Icons.Default.CalendarToday, date)
            DetailRow(Icons.Default.Schedule, time)
            DetailRow(Icons.Default.LocationOn, location)

            // ── Response status or Accept / Decline buttons ──────────
            when {
                response == "accepted" -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "✓ Accepted",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    )
                }
                response == "declined" -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "✗ Declined",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    )
                }
                onAccept != null && onDecline != null -> {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                        ) { Text("Accept") }
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f),
                        ) { Text("Decline") }
                    }
                }
            }
        }
    }
}