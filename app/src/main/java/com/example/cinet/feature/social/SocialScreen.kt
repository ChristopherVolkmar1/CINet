package com.example.cinet.feature.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.cinet.data.model.FriendRequest
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import kotlinx.coroutines.launch
import com.example.cinet.core.designsystem.PullToRefreshContainer

@Composable
fun SocialScreen(
    onOpenProfile: (UserProfile) -> Unit,
    onOpenConversation: (UserProfile) -> Unit,
) {
    val repository = remember { SocialRepository() }
    val scope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var sentRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var sentRequestNicknames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) isRefreshing = true
        repository.getFriends().onSuccess { friends = it }
        repository.getPendingRequests().onSuccess { pendingRequests = it }
        repository.getSentRequests().onSuccess { requests ->
            sentRequests = requests
            val nicknames = mutableMapOf<String, String>()
            requests.forEach { request ->
                nicknames[request.receiverId] = repository.getUserNickname(request.receiverId)
            }
            sentRequestNicknames = nicknames
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
        } else {
            val friendUids = friends.map { it.uid }.toSet()
            val sentUids = sentRequests.map { it.receiverId }.toSet()
            repository.searchUsersByNickname(searchQuery).onSuccess { results ->
                searchResults = results.filter { it.uid !in friendUids && it.uid !in sentUids }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh = { refreshKey++ },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // ── Header ────────────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "People",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ── Search bar ────────────────────────────────────────
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by nickname") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Search results ────────────────────────────────────
                    if (searchResults.isNotEmpty()) {
                        item {
                            SectionHeader("Search Results")
                        }
                        items(searchResults) { user ->
                            UserRow(
                                user = user,
                                onClick = {},
                                trailingContent = {
                                    FilledTonalButton(
                                        onClick = {
                                            scope.launch {
                                                repository.sendFriendRequest(user)
                                                searchResults = searchResults - user
                                                repository.getSentRequests().onSuccess { requests ->
                                                    sentRequests = requests
                                                    val nicknames = mutableMapOf<String, String>()
                                                    requests.forEach { req ->
                                                        nicknames[req.receiverId] =
                                                            repository.getUserNickname(req.receiverId)
                                                    }
                                                    sentRequestNicknames = nicknames
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null,
                                            modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add")
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // ── Pending requests ──────────────────────────────────
                    if (pendingRequests.isNotEmpty()) {
                        item { SectionHeader("Friend Requests") }
                        items(pendingRequests) { request ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = request.senderNickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(request.senderNickname,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("Wants to connect",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        repository.declineFriendRequest(request)
                                        pendingRequests = pendingRequests - request
                                    }
                                }) { Text("Decline", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Spacer(Modifier.width(4.dp))
                                Button(onClick = {
                                    scope.launch {
                                        repository.acceptFriendRequest(request)
                                        pendingRequests = pendingRequests - request
                                        repository.getFriends().onSuccess { friends = it }
                                    }
                                }) { Text("Accept") }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    // ── Friends ───────────────────────────────────────────
                    item { SectionHeader("Friends") }
                    if (friends.isEmpty()) {
                        item {
                            Text(
                                "No friends yet — search above to connect",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(friends) { friend ->
                            UserRow(
                                user = friend,
                                onClick = { onOpenConversation(friend) }
                            )
                        }
                    }

                    // ── Sent requests ─────────────────────────────────────
                    if (sentRequests.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            SectionHeader("Pending Sent Requests")
                        }
                        items(sentRequests) { request ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val nickname = sentRequestNicknames[request.receiverId] ?: "?"
                                    Text(
                                        text = nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        sentRequestNicknames[request.receiverId] ?: request.receiverId,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text("Request pending",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
fun UserRow(
    user: UserProfile,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val photoUrl = user.photoUrl.takeIf { it.isNotBlank() }
        if (photoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl).crossfade(true).build(),
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.nickname,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            val subtitle = listOfNotNull(
                user.major.takeIf { it.isNotBlank() },
                user.pronouns.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailingContent?.invoke()
    }
}