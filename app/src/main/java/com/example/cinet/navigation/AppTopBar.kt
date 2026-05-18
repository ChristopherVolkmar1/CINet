package com.example.cinet.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.feature.social.ConversationTopBarState

// Draws the persistent page title bar used across the app.
@Composable
internal fun AppTopBar(
    state: NavigationTopBarState,
    isHomeScreen: Boolean = false,
    nickname: String = "",
    onBack: () -> Unit,
    onFriendsClick: () -> Unit,
    onNewMessageClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(start = 20.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val newConversationTopBarState = state.newConversationTopBarState
            val conversationTopBarState = state.conversationTopBarState
            if (state.showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            when {
                conversationTopBarState != null -> {
                    ConversationTopBarContent(
                        state = conversationTopBarState,
                        modifier = Modifier.weight(1f)
                    )
                }

                newConversationTopBarState != null -> {
                    Text(
                        text = newConversationTopBarState.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 50.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                isHomeScreen -> {
                    WelcomeHeader(
                        nickname = nickname,
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    Text(
                        text = state.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 50.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (conversationTopBarState != null) {
                ConversationTopBarActions(state = conversationTopBarState)
            } else if (newConversationTopBarState != null) {
                NewConversationTopBarAction(state = newConversationTopBarState)
            } else if (state.showSocialActions) {
                SocialTopBarActions(
                    pendingRequestCount = state.pendingRequestCount,
                    onFriendsClick = onFriendsClick,
                    onNewMessageClick = onNewMessageClick
                )
            }
        }
    }
}

// Shows the right-side action used by the New Message page.
@Composable
private fun NewConversationTopBarAction(
    state: com.example.cinet.feature.social.NewConversationTopBarState,
) {
    TextButton(
        onClick = state.onActionClick,
        enabled = state.actionEnabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.45f)
        )
    ) {
        if (state.isActionLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = state.actionLabel,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Renders the avatar + title section for an open conversation in the persistent top bar.
@Composable
private fun ConversationTopBarContent(
    state: ConversationTopBarState,
    modifier: Modifier = Modifier,
) {
    val photoUrl = state.photoUrl.takeIf { it.isNotBlank() }
    val initial = state.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Row(
        modifier = modifier
            .then(if (state.onTitleClick != null) Modifier.clickable { state.onTitleClick.invoke() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!state.isGroup && photoUrl != null) {
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
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = initial, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = state.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Shows the action buttons for an open conversation (search, info, or more-options menu).
@Composable
private fun ConversationTopBarActions(state: ConversationTopBarState) {
    if (state.isGroup) {
        // Group: Search + Info
        IconButton(onClick = state.onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Search messages", tint = Color.White)
        }
        if (state.onInfoClick != null) {
            IconButton(onClick = state.onInfoClick) {
                Icon(Icons.Default.Info, contentDescription = "Group info", tint = Color.White)
            }
        }
    } else {
        // DM: MoreVert dropdown — Search Messages, Remove Friend
        var expanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Search Messages") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    onClick = { expanded = false; state.onSearchClick() }
                )
                if (state.onRemoveFriendClick != null) {
                    DropdownMenuItem(
                        text = { Text("Remove Friend") },
                        leadingIcon = { Icon(Icons.Default.PersonRemove, null) },
                        onClick = { expanded = false; state.onRemoveFriendClick.invoke() }
                    )
                }
            }
        }
    }
}

// Shows the friend greeting inside the persistent top bar.
@Composable
private fun WelcomeHeader(
    nickname: String,
    modifier: Modifier = Modifier
) {
    val displayName = nickname.ifBlank { "there" }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Welcome back, $displayName 👋",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = greetingFontSizeFor(displayName),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Chooses a smaller greeting size when the name is longer.
private fun greetingFontSizeFor(displayName: String) = when {
    displayName.length > 22 -> 20.sp
    displayName.length > 16 -> 22.sp
    displayName.length > 10 -> 24.sp
    else -> 26.sp
}

// Shows the friends and new-message actions used by the Messages page.
@Composable
private fun SocialTopBarActions(
    pendingRequestCount: Int,
    onFriendsClick: () -> Unit,
    onNewMessageClick: () -> Unit,
) {
    BadgedBox(
        badge = {
            if (pendingRequestCount > 0) {
                Badge { Text(pendingRequestCount.toString()) }
            }
        }
    ) {
        IconButton(onClick = onFriendsClick) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = "Friends",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Box(contentAlignment = Alignment.Center) {
        IconButton(onClick = onNewMessageClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "New conversation",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}