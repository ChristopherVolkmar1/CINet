package com.example.cinet.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.feature.social.ConversationTopBarState
import androidx.compose.foundation.layout.RowScope


// Draws the persistent page title bar used across the app.
@Composable
internal fun AppTopBar(
    state: NavigationTopBarState,
    isHomeScreen: Boolean = false,
    nickname: String = "",
    mapTopBarContent: (@Composable RowScope.() -> Unit)? = null,
    calendarTopBarContent: (@Composable RowScope.() -> Unit)? = null,
    settingsTopBarActions: (@Composable RowScope.() -> Unit)? = null,
    onBack: () -> Unit,
    onFriendsClick: () -> Unit,
    onNewMessageClick: () -> Unit,
    onCanvasMessagesClick: () -> Unit,
    onSettingsCanvasClick: () -> Unit,
    onSettingsSignOutClick: () -> Unit,
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

                calendarTopBarContent != null -> {
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

                    calendarTopBarContent()
                }

                mapTopBarContent != null -> {
                    mapTopBarContent()
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


                        when {
                conversationTopBarState != null ->
                    ConversationTopBarActions(state = conversationTopBarState)

                newConversationTopBarState != null ->
                    NewConversationTopBarAction(state = newConversationTopBarState)

                state.showSocialActions ->
                    SocialTopBarActions(
                        pendingRequestCount = state.pendingRequestCount,
                        showCanvasMessages = state.showCanvasMessagesAction,
                        onFriendsClick = onFriendsClick,
                        onNewMessageClick = onNewMessageClick,
                        onCanvasMessagesClick = onCanvasMessagesClick
                    )

                state.showSettingsActions ->
                    SettingsTopBarActions(
                        onCanvasSyncClick = onSettingsCanvasClick,
                        onSignOutClick = onSettingsSignOutClick
                    )

                settingsTopBarActions != null ->
                    settingsTopBarActions()
            }

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
        modifier = modifier.then(
            if (state.onTitleClick != null) {
                Modifier.clickable { state.onTitleClick.invoke() }
            } else {
                Modifier
            }
        ),
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
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
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

// Shows the action buttons for an open conversation.
@Composable
private fun ConversationTopBarActions(state: ConversationTopBarState) {
    if (state.isGroup) {
        if (state.onPinnedClick != null) {
            IconButton(onClick = state.onPinnedClick) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned messages",
                    tint = Color.White
                )
            }
        }

        IconButton(onClick = state.onSearchClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search messages",
                tint = Color.White
            )
        }

        if (state.onInfoClick != null) {
            IconButton(onClick = state.onInfoClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Group info",
                    tint = Color.White
                )
            }
        }
    } else {
        var expanded by remember { mutableStateOf(false) }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (state.onPinnedClick != null) {
                    DropdownMenuItem(
                        text = { Text("Pinned Messages") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            expanded = false
                            state.onPinnedClick.invoke()
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Search Messages") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expanded = false
                        state.onSearchClick()
                    }
                )

                if (state.onRemoveFriendClick != null) {
                    DropdownMenuItem(
                        text = { Text("Remove Friend") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PersonRemove,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            expanded = false
                            state.onRemoveFriendClick.invoke()
                        }
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
    showCanvasMessages: Boolean,
    onFriendsClick: () -> Unit,
    onNewMessageClick: () -> Unit,
    onCanvasMessagesClick: () -> Unit,
) {
    if (showCanvasMessages) {
        IconButton(onClick = onCanvasMessagesClick) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Canvas inbox",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
    }

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

// Shows the Canvas Sync and Sign Out actions used by the Settings page.
@Composable
private fun SettingsTopBarActions(
    onCanvasSyncClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    IconButton(onClick = onCanvasSyncClick) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Canvas Sync",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }

    Spacer(modifier = Modifier.width(12.dp))

    IconButton(onClick = onSignOutClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Sign out",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}