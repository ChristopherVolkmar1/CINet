package com.example.cinet.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    user: UserProfile,
    currentUserProfile: UserProfile,
    onOpenConversation: (Conversation) -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit = {},
) {
    val isOwnProfile = user.uid == currentUserProfile.uid
    val repository = remember { SocialRepository() }
    val scope = rememberCoroutineScope()

    var isFriend by remember { mutableStateOf(false) }
    var requestSent by remember { mutableStateOf(false) }
    var isLoadingAction by remember { mutableStateOf(false) }

    LaunchedEffect(user.uid) {
        if (!isOwnProfile) {
            repository.getFriends().onSuccess { friends ->
                isFriend = friends.any { it.uid == user.uid }
            }
            repository.getSentRequests().onSuccess { requests ->
                requestSent = requests.any { it.receiverId == user.uid }
            }
        }
    }

    // Intercept system back so it always calls onBack regardless of
    // NavigationBackHandler's priority order — ensures returning to
    // the conversation rather than home when opened from inside a chat.
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Avatar
            val photoUrl = user.photoUrl.takeIf { it.isNotBlank() }
            if (photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl).crossfade(true).build(),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            Text(
                text = user.nickname,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // Pronouns
            if (user.pronouns.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.pronouns,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bio
            if (user.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info cards — Major and Year
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (user.major.isNotBlank()) {
                    ProfileInfoCard(
                        icon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = "Major",
                        value = user.major
                    )
                }
                if (user.minor.isNotBlank()) {
                    ProfileInfoCard(
                        icon = {
                            Icon(
                                Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = "Minor",
                        value = user.minor
                    )
                }
                if (user.year.isNotBlank()) {
                    ProfileInfoCard(
                        icon = {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = "Year",
                        value = user.year
                    )
                }
            }

            // Interests chips
            if (user.interests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Interests",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        user.interests.forEach { interest ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(interest) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isOwnProfile) {
                    Button(
                        onClick = onEditProfile,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile")
                    }
                } else {
                    if (isFriend) {
                        Button(
                            onClick = {
                                isLoadingAction = true
                                scope.launch {
                                    repository.getOrCreateConversation(
                                        participantIds = listOf(
                                            currentUserProfile.uid, user.uid
                                        ),
                                        participantNicknames = mapOf(
                                            currentUserProfile.uid to currentUserProfile.nickname,
                                            user.uid to user.nickname
                                        )
                                    ).onSuccess { onOpenConversation(it) }
                                    isLoadingAction = false
                                }
                            },
                            enabled = !isLoadingAction,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            if (isLoadingAction) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Message")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (!requestSent) {
                                    isLoadingAction = true
                                    scope.launch {
                                        repository.sendFriendRequest(user).onSuccess {
                                            requestSent = true
                                        }
                                        isLoadingAction = false
                                    }
                                }
                            },
                            enabled = !requestSent && !isLoadingAction,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(
                                when {
                                    isLoadingAction -> "Sending..."
                                    requestSent -> "Request Sent"
                                    else -> "Add Friend"
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileInfoCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}