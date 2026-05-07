package com.example.cinet.feature.map

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults.Thumb
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import com.example.cinet.feature.social.FriendSelectRow
import com.example.cinet.ui.theme.CINetTheme
import kotlinx.coroutines.launch


/**
 * ShareLocationManager.kt
 *
 * Handles the social logic for sharing real-time campus locations with peers,
 * This includes managing the friend selection state and triggering the
 * "Share Location with Friends" popup as seen in watermarked_img_11462799985366364782.png.
 */
// -------------------- Location Sharing --------------------

@Composable
fun ShareLocation( friends: List<UserProfile>, location: CampusLocation ) {
    var showDialog by remember { mutableStateOf(false) }
    Surface(
        onClick = { showDialog = true },
        shape = RoundedCornerShape(36.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ShareLocation,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Quick Share",
                style = MaterialTheme.typography.headlineSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
    if (showDialog) {
        Share(friends = friends, location = location, onDismiss = { showDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Share(friends: List<UserProfile>, location: CampusLocation, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repository = remember { SocialRepository() }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val lazyListState = rememberLazyListState()
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 6.dp,
            border = BorderStroke(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .widthIn(min = 320.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Send to...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(state = lazyListState, modifier = Modifier.heightIn(max = 400.dp)) {
                    items(friends) { friend ->
                        FriendSelectRow(
                            friend = friend,
                            isSelected = selectedIds.contains(friend.uid),
                            onToggle = {
                                selectedIds = if (selectedIds.contains(friend.uid)) {
                                    selectedIds - friend.uid
                                } else {
                                    selectedIds + friend.uid
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ElevatedButton(
                        onClick = { onDismiss() },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.width(75.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    ElevatedButton(
                        onClick = {
                            scope.launch {
                                android.util.Log.d("ShareLocation", "Selected IDs: $selectedIds")
                                selectedIds.forEach { uid ->
                                    val selectedFriend = friends.find { it.uid == uid }
                                    val nickname = selectedFriend?.nickname ?: "User"
                                    val result = repository.getOrCreateConversation(
                                        participantIds = listOf(currentUserId, uid),
                                        participantNicknames = mapOf(uid to nickname)
                                    )
                                    result.onSuccess { conversation ->
                                        android.util.Log.d("ShareLocation", "Conversation ID: ${conversation.id}")
                                        repository.sendMessage(
                                            conversationId = conversation.id,
                                            content = "Shared a location with you!",
                                            type = "location_share",
                                            metadata = mapOf(
                                                "locationName" to location.name
                                            )
                                        )
                                    }
                                }
                                onDismiss()
                            }
                        },
                        enabled = selectedIds.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.width(75.dp)
                    ) {
                        Text(
                            text = "Send",
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LocationSharePreview() {
    val location = CampusLocation("Aliso Hall", "Academic", description = "null")
    val mockFriends = listOf(
        UserProfile(uid = "1", nickname = "Ian", major = "Computer Science", pronouns = "he/him"),
        UserProfile(uid = "2", nickname = "Maddi", major = "Computer Science", pronouns = "she/they")
    )
    CINetTheme(darkTheme = true) {
        Share(friends  = mockFriends, location = location, onDismiss = {})
    }
}