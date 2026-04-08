package com.example.cinet

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.FriendRequest
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import kotlinx.coroutines.launch

@Composable
fun SocialScreen(
    onOpenProfile: (UserProfile) -> Unit,
) {
    val repository = remember { SocialRepository() }
    val scope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var sentRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repository.getFriends().onSuccess { friends = it }
        repository.getPendingRequests().onSuccess { pendingRequests = it }
        repository.getSentRequests().onSuccess { sentRequests = it }
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
                                        repository.getSentRequests()
                                            .onSuccess { sentRequests = it }
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = request.receiverId,
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

// Frontend team: restyle this row however you want
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