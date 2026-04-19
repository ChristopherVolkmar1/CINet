package com.example.cinet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.FriendRequest
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.layout.size

@Composable
fun SocialScreen(
    onOpenProfile: (UserProfile) -> Unit,
) {
    val repository = remember { SocialRepository() }
    val scope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var sentRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var sentRequestNicknames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
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
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Text("Social", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isSearching = true
                                repository.searchUsersByNickname(searchQuery)
                                    .onSuccess { searchResults = it }
                                isSearching = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Search")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { refreshKey++ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Refresh")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (searchResults.isNotEmpty()) {
                    item {
                        Text("Search Results", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(searchResults) { user ->
                        UserRow(
                            user = user,
                            onClick = { onOpenProfile(user) },
                            trailingContent = {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        repository.sendFriendRequest(user)
                                        searchResults = searchResults - user
                                        repository.getSentRequests().onSuccess { requests ->
                                            sentRequests = requests
                                            val nicknames = mutableMapOf<String, String>()
                                            requests.forEach { request ->
                                                nicknames[request.receiverId] =
                                                    repository.getUserNickname(request.receiverId)
                                            }
                                            sentRequestNicknames = nicknames
                                        }
                                    }
                                }) {
                                    Text("Add")
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                if (pendingRequests.isNotEmpty()) {
                    item {
                        Text("Friend Requests", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(pendingRequests) { request ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = request.senderNickname,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {
                                scope.launch {
                                    repository.acceptFriendRequest(request)
                                    pendingRequests = pendingRequests - request
                                    repository.getFriends().onSuccess { friends = it }
                                }
                            }) {
                                Text("Accept")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                scope.launch {
                                    repository.declineFriendRequest(request)
                                    pendingRequests = pendingRequests - request
                                }
                            }) {
                                Text("Decline")
                            }
                        }
                        HorizontalDivider()
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                item {
                    Text("Friends", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (friends.isEmpty()) {
                    item {
                        Text(
                            "No friends yet — search for someone above",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(friends) { friend ->
                        UserRow(
                            user = friend,
                            onClick = { onOpenProfile(friend) }
                        )
                        HorizontalDivider()
                    }
                }

                if (sentRequests.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Pending Sent Requests", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(sentRequests) { request ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Initials fallback only — sent requests don't carry photoUrl
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val nickname = sentRequestNicknames[request.receiverId] ?: "?"
                                Text(
                                    text = nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sentRequestNicknames[request.receiverId] ?: request.receiverId,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Request pending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// Frontend: restyle this row however you want
// Frontend: restyle this row however you want
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile avatar — shows Google photo or initials fallback
        val photoUrl = user.photoUrl.takeIf { it.isNotBlank() }
        if (photoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        } else {
            // Fallback: circle with first letter of nickname
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                    text = user.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = user.nickname, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${user.major} · ${user.pronouns}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingContent?.invoke()
    }
}