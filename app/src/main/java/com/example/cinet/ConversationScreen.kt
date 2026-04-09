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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.Message
import com.example.cinet.data.remote.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

// Preset message content — easy to add more types here
private val presetMessages = listOf(
    "study_invite" to "Want to study together?",
    "event_invite" to "You should come to this event!",
)

@Composable
fun ConversationScreen(
    conversation: Conversation,
    onBack: () -> Unit,
) {
    val repository = remember { SocialRepository() }
    val scope = rememberCoroutineScope()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }

    // Real-time listener — updates messages instantly when Firestore changes
    DisposableEffect(conversation.id) {
        val listener = FirebaseFirestore.getInstance()
            .collection("conversations")
            .document(conversation.id)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.toObjects(Message::class.java)
                }
            }
        onDispose { listener.remove() }
    }

    // Scroll to bottom whenever new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
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
            // Header — frontend team can restyle this
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack) { Text("Back") }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = conversationTitle, style = MaterialTheme.typography.titleLarge)
            }

            HorizontalDivider()

            // Preset message buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetMessages.forEach { (type, label) ->
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                repository.sendMessage(conversation.id, label, type)
                            }
                        }
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider()

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == currentUid
                    )
                }
            }

            // Text input
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
                ) {
                    Text("Send")
                }
            }
        }
    }
}

// Frontend team: restyle this bubble however you want
@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
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
    }
}