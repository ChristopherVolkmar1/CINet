package com.example.cinet.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import kotlinx.coroutines.launch

private enum class NewConvStep { Selecting, Naming }

@Composable
fun NewConversationScreen(
    currentUserProfile: UserProfile,
    onBack: () -> Unit,
    onOpenConversation: (Conversation) -> Unit,
) {
    val repository = remember { SocialRepository() }
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(NewConvStep.Selecting) }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFriends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var groupName by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        repository.getFriends().onSuccess { friends = it }
        isLoading = false
    }

    val filteredFriends = remember(friends, searchQuery) {
        if (searchQuery.isBlank()) friends
        else friends.filter { it.nickname.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    when (step) {
                        NewConvStep.Selecting -> onBack()
                        NewConvStep.Naming -> step = NewConvStep.Selecting
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = when (step) {
                        NewConvStep.Selecting -> "New Message"
                        NewConvStep.Naming -> "New Group"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // Action button — changes label based on step and selection
                when (step) {
                    NewConvStep.Selecting -> {
                        val canProceed = selectedFriends.isNotEmpty() && !isCreating
                        TextButton(
                            onClick = {
                                when {
                                    selectedFriends.size == 1 -> {
                                        // Single friend → open DM directly
                                        val friend = selectedFriends.first()
                                        isCreating = true
                                        scope.launch {
                                            repository.getOrCreateConversation(
                                                participantIds = listOf(currentUserProfile.uid, friend.uid),
                                                participantNicknames = mapOf(
                                                    currentUserProfile.uid to currentUserProfile.nickname,
                                                    friend.uid to friend.nickname
                                                )
                                            ).onSuccess { onOpenConversation(it) }
                                            isCreating = false
                                        }
                                    }
                                    selectedFriends.size >= 2 -> {
                                        // Multiple → go to naming step
                                        step = NewConvStep.Naming
                                    }
                                }
                            },
                            enabled = canProceed
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text(
                                    text = when {
                                        selectedFriends.size >= 2 -> "Next"
                                        else -> "Chat"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    NewConvStep.Naming -> {
                        TextButton(
                            onClick = {
                                if (groupName.isNotBlank() && !isCreating) {
                                    isCreating = true
                                    scope.launch {
                                        val allIds = listOf(currentUserProfile.uid) + selectedFriends.map { it.uid }
                                        val nicknames = (listOf(currentUserProfile) + selectedFriends)
                                            .associate { it.uid to it.nickname }
                                        repository.getOrCreateConversation(
                                            participantIds = allIds,
                                            participantNicknames = nicknames,
                                            isGroup = true,
                                            groupName = groupName.trim()
                                        ).onSuccess { onOpenConversation(it) }
                                        isCreating = false
                                    }
                                }
                            },
                            enabled = groupName.isNotBlank() && !isCreating
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text("Create", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Step: Friend Selection ───────────────────────────────
            if (step == NewConvStep.Selecting) {

                // Selected friends chips
                if (selectedFriends.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedFriends, key = { it.uid }) { friend ->
                            SelectedFriendChip(
                                friend = friend,
                                onRemove = {
                                    selectedFriends = selectedFriends - friend
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search friends…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(24.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (friends.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Add some friends first to start a conversation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredFriends, key = { it.uid }) { friend ->
                            val isSelected = selectedFriends.any { it.uid == friend.uid }
                            FriendSelectRow(
                                friend = friend,
                                isSelected = isSelected,
                                onToggle = {
                                    selectedFriends = if (isSelected) {
                                        selectedFriends - friend
                                    } else {
                                        selectedFriends + friend
                                    }
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                        }
                    }
                }
            }

            // ── Step: Group Name ─────────────────────────────────────
            if (step == NewConvStep.Naming) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Member preview
                    Text(
                        text = "${selectedFriends.size + 1} members",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selectedFriends) { friend ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FriendAvatar(friend = friend, size = 44)
                                Text(
                                    text = friend.nickname,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group name") },
                        placeholder = { Text("e.g. Study crew, COMP 101…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "You can rename it later from the conversation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendSelectRow(
    friend: UserProfile,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FriendAvatar(friend = friend, size = 48)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.nickname,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${friend.major} · ${friend.pronouns}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun SelectedFriendChip(
    friend: UserProfile,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = friend.nickname,
                style = MaterialTheme.typography.labelMedium
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove ${friend.nickname}",
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
fun FriendAvatar(friend: UserProfile, size: Int) {
    val photoUrl = friend.photoUrl.takeIf { it.isNotBlank() }
    if (photoUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = friend.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}