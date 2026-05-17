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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Shield
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
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import kotlinx.coroutines.launch

/**
 * Bottom sheet showing group members, roles, and admin actions.
 *
 * Admin actions: rename, add member, remove member.
 * All members: leave group.
 * Each member row opens their ProfileScreen via [onOpenProfile].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoSheet(
    conversation: Conversation,
    currentUid: String,
    onDismiss: () -> Unit,
    onRenameGroup: () -> Unit,
    onLeaveGroup: () -> Unit,
    onOpenProfile: ((UserProfile) -> Unit)?,
) {
    val repository = remember { SocialRepository() }
    val scope = rememberCoroutineScope()

    // Local copy of roles so promote/demote reflects immediately without
    // waiting for a Firestore round-trip to update the parent Conversation.
    var localRoles by remember { mutableStateOf(conversation.roles) }
    val isAdmin = localRoles[currentUid] == "admin"

    // Load full UserProfile objects for each participant
    var members by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(true) }
    var showAddMemberSheet by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var removingUid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(conversation.participantIds) {
        isLoadingMembers = true
        members = repository.getConversationMemberProfiles(conversation.participantIds)
        isLoadingMembers = false
    }

    // Leave confirmation dialog
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Group?") },
            text = { Text("You will no longer receive messages in \"${conversation.groupName}\".") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirm = false
                        scope.launch {
                            repository.removeGroupMember(conversation.id, currentUid)
                            onLeaveGroup()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Leave") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Add member sheet
    if (showAddMemberSheet) {
        AddMemberSheet(
            conversation = conversation,
            currentUid = currentUid,
            onDismiss = { showAddMemberSheet = false },
            onMemberAdded = { newMember ->
                showAddMemberSheet = false
                scope.launch {
                    repository.addGroupMember(conversation.id, newMember)
                    // Refresh list
                    members = repository.getConversationMemberProfiles(
                        conversation.participantIds + newMember.uid
                    )
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Sheet title
            Text(
                text = conversation.groupName.ifBlank { "Group Chat" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            HorizontalDivider()

            // Admin actions
            if (isAdmin) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onRenameGroup(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rename Group")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showAddMemberSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Member")
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }

            // Members header
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${members.size} Members",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (isLoadingMembers) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(members, key = { it.uid }) { member ->
                        val memberRole = localRoles[member.uid] ?: "member"
                        val isSelf = member.uid == currentUid

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isSelf && onOpenProfile != null) {
                                    onOpenProfile?.invoke(member)
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Avatar
                            val photoUrl = member.photoUrl.takeIf { it.isNotBlank() }
                            if (photoUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photoUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = member.nickname + if (isSelf) " (You)" else "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (memberRole == "admin") FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (memberRole == "admin") {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = "Admin",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                if (member.major.isNotBlank()) {
                                    Text(
                                        text = member.major,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Admin actions on other members
                            if (isAdmin && !isSelf) {
                                // Promote / demote toggle
                                TextButton(onClick = {
                                    val newRole = if (memberRole == "admin") "member" else "admin"
                                    // Update local state immediately so UI reflects the change
                                    localRoles = localRoles.toMutableMap().also { it[member.uid] = newRole }
                                    scope.launch {
                                        repository.updateMemberRole(conversation.id, member.uid, newRole)
                                    }
                                }) {
                                    Text(
                                        text = if (memberRole == "admin") "Demote" else "Make Admin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                // Remove — only for non-admins
                                if (memberRole != "admin") {
                                    IconButton(
                                        onClick = {
                                            removingUid = member.uid
                                            scope.launch {
                                                repository.removeGroupMember(conversation.id, member.uid)
                                                members = members.filter { it.uid != member.uid }
                                                removingUid = null
                                            }
                                        }
                                    ) {
                                        if (removingUid == member.uid) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                        } else {
                                            Icon(
                                                Icons.Default.PersonRemove,
                                                contentDescription = "Remove ${member.nickname}",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // Leave group — always available (unless only member, but that's an edge case)
            TextButton(
                onClick = { showLeaveConfirm = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leave Group")
            }
        }
    }
}

/**
 * Sheet for admin to pick a friend to add to the group.
 * Shows friends not already in the conversation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberSheet(
    conversation: Conversation,
    currentUid: String,
    onDismiss: () -> Unit,
    onMemberAdded: (UserProfile) -> Unit,
) {
    val repository = remember { SocialRepository() }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        repository.getFriends().onSuccess { allFriends ->
            // Only show friends not already in the group
            friends = allFriends.filter { it.uid !in conversation.participantIds }
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Member",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(80.dp), Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            } else if (friends.isEmpty()) {
                Text(
                    text = "No friends available to add.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(friends, key = { it.uid }) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMemberAdded(friend) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FriendAvatar(friend = friend, size = 44)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    friend.nickname,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                if (friend.major.isNotBlank()) {
                                    Text(
                                        friend.major,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}