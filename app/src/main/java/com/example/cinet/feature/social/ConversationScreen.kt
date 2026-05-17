package com.example.cinet.feature.social

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri as AndroidUri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.Message
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import com.example.cinet.feature.calendar.calendarFiles.CalendarFirestoreRepository
import com.example.cinet.feature.calendar.event.EventItem
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import com.example.cinet.feature.calendar.study.StudySession
import com.example.cinet.feature.calendar.study.StudyInviteDialog
import com.example.cinet.feature.calendar.event.EventInviteSenderDialog
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Info

/**
 * A file the user has selected but not yet sent.
 * Shown as a preview above the message box (2-step send flow).
 */
private data class PendingAttachment(
    val uri: AndroidUri,
    val fileName: String,
    val mimeType: String,
)

@Composable
fun ConversationScreen(
    conversation: Conversation,
    onBack: () -> Unit,
    onNavigateToLocation: ((String) -> Unit)? = null,
    onNavigateToCoordinates: ((Double, Double, String, String) -> Unit)? = null,
    onOpenProfile: ((UserProfile) -> Unit)? = null,
) {
    val repository = remember { SocialRepository() }
    val calendarRepository = remember { CalendarFirestoreRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val listState = rememberLazyListState()
    var isUploadingAttachment by remember { mutableStateOf(false) }
    // Holds a picked file that hasn't been sent yet — shown as a preview
    // above the message box. Cleared on send or on the user pressing X.
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }

    // File picker — resolves metadata client-side, stores as pending so the
    // user sees a preview before the file is actually uploaded or sent.
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            var fileName = "attachment"
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (col >= 0 && cursor.moveToFirst()) fileName = cursor.getString(col)
                }
            }
            pendingAttachment = PendingAttachment(uri, fileName, mimeType)
        }
    }

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var conversationCount by remember { mutableIntStateOf(0) }
    val entryTime = remember { System.currentTimeMillis() }
    var messageInput by remember { mutableStateOf("") }
    var showStudyInviteDialog by remember { mutableStateOf(false) }
    var showEventInviteDialog by remember { mutableStateOf(false) }
    var showRemoveFriendDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showGroupInfo by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    // Local override so rename is reflected immediately without re-navigation
    var displayGroupName by remember { mutableStateOf(conversation.groupName) }
    var myScheduleItems by remember { mutableStateOf<List<ScheduleItem>>(emptyList()) }
    var myStudySessions by remember { mutableStateOf<List<StudySession>>(emptyList()) }
    var myEvents by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var otherUserPhotoUrl by remember { mutableStateOf("") }
    var otherUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    // For group chats: uid → UserProfile so message avatars can open the right profile
    var memberProfiles by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var currentUserPhotoUrl by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // null = preview closed; non-null = show full-screen image preview for this message
    var previewMessage by remember { mutableStateOf<Message?>(null) }
    var currentUserNickname by remember { mutableStateOf("") }
    // Filtered messages — client-side only, no extra Firestore reads.
    // Matches text content for regular messages; falls back to invite
    // title/class fields and location name for structured bubble types.
    val displayedMessages by remember {
        derivedStateOf {
            val q = searchQuery.trim()
            if (q.isBlank()) messages
            else messages.filter { msg ->
                when (msg.type) {
                    "study_invite" -> {
                        val cls  = msg.metadata["className"] as? String ?: ""
                        val topic = msg.metadata["topic"]    as? String ?: ""
                        cls.contains(q, ignoreCase = true) || topic.contains(q, ignoreCase = true)
                    }
                    "event_invite" -> {
                        val name = msg.metadata["name"] as? String ?: ""
                        name.contains(q, ignoreCase = true)
                    }
                    "location_share" -> {
                        val loc = msg.metadata["locationName"] as? String ?: ""
                        loc.contains(q, ignoreCase = true)
                    }
                    "attachment" -> {
                        val name = msg.metadata["fileName"] as? String ?: ""
                        name.contains(q, ignoreCase = true)
                    }
                    else -> msg.content.contains(q, ignoreCase = true)
                }
            }
        }
    }

    // When the query changes and results exist, jump to the first match.
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && displayedMessages.isNotEmpty()) {
            val firstIndex = messages.indexOfFirst { it.id == displayedMessages.first().id }
            if (firstIndex >= 0) listState.animateScrollToItem(firstIndex)
        }
    }

    // Returns true if the same invite was sent in this conversation within
    // the last 5 minutes, preventing accidental double-sends.
    fun isDuplicateInvite(type: String, name: String, date: String): Boolean {
        val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000L
        return messages.any { msg ->
            msg.type == type &&
                    (msg.metadata["name"] as? String ?: msg.metadata["className"] as? String ?: "") == name &&
                    (msg.metadata["date"] as? String ?: "") == date &&
                    (msg.createdAt?.time ?: 0L) >= fiveMinutesAgo
        }
    }

    val otherUid = conversation.participantIds.firstOrNull { it != currentUid } ?: ""

    val conversationTitle = if (conversation.isGroup) {
        displayGroupName.ifBlank { "Group Chat" }
    } else {
        conversation.participantNicknames.entries
            .firstOrNull { it.key != currentUid }?.value ?: "Conversation"
    }

    // Load both participants' photos on open
    LaunchedEffect(conversation.id) {
        if (conversation.isGroup) {
            val profiles = repository.getConversationMemberProfiles(conversation.participantIds)
            memberProfiles = profiles.associateBy { it.uid }
        } else if (otherUid.isNotBlank()) {
            val otherSnapshot = FirebaseFirestore.getInstance()
                .collection("users").document(otherUid).get().await()
            otherUserPhotoUrl = otherSnapshot.getString("photoUrl") ?: ""
            otherUserProfile = otherSnapshot.toObject(UserProfile::class.java)
        }
        val currentSnapshot = FirebaseFirestore.getInstance()
            .collection("users").document(currentUid).get().await()
        currentUserPhotoUrl = currentSnapshot.getString("photoUrl") ?: ""

        currentUserNickname = currentSnapshot.getString("nickname") ?: ""  // add
    }

    DisposableEffect(conversation.id) {
        val listener = FirebaseFirestore.getInstance()
            .collection("conversations")
            .document(conversation.id)
            .collection("messages")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.toObjects(Message::class.java)
                        .sortedBy { it.createdAt }
                }
            }
        onDispose { listener.remove() }
    }

    // Real-time listener: keeps conversation count badge in sync
    DisposableEffect(currentUid) {
        val listener = FirebaseFirestore.getInstance()
            .collection("conversations")
            .whereArrayContains("participantIds", currentUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    conversationCount = snapshot.documents.count { doc ->
                        val docId = doc.id
                        val lastUpdated = doc.getTimestamp("lastUpdated")?.toDate()?.time ?: 0L
                        docId != conversation.id && lastUpdated > entryTime
                    }
                }
            }
        onDispose { listener.remove() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // User location
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { userLocation = LatLng(it.latitude, it.longitude) }
        }
    } catch (_: SecurityException) {
        Log.e("Location", "No Permission")
    }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getFriends().onSuccess {
            friends = it
        }
    }

    // Remove Friend confirmation dialog
    if (showRemoveFriendDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveFriendDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove $conversationTitle as a friend?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveFriendDialog = false
                        scope.launch {
                            repository.removeFriend(otherUid)
                            onBack()
                        }
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRemoveFriendDialog = false },
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename group dialog
    // Group Info bottom sheet
    if (showGroupInfo && conversation.isGroup) {
        GroupInfoSheet(
            conversation = conversation,
            currentUid = currentUid,
            onDismiss = { showGroupInfo = false },
            onRenameGroup = {
                showGroupInfo = false
                renameInput = displayGroupName
                showRenameDialog = true
            },
            onLeaveGroup = {
                showGroupInfo = false
                onBack()
            },
            onOpenProfile = onOpenProfile,
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Group") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotBlank()) {
                            showRenameDialog = false
                            scope.launch {
                                repository.renameConversation(conversation.id, newName)
                                displayGroupName = newName
                            }
                        }
                    },
                    enabled = renameInput.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // iOS-style back: bare chevron + conversation count pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onBack),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        if (conversationCount > 0) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = conversationCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))

                    if (conversation.isGroup) {
                        // Group: avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversationTitle.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = conversationTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        // Search messages
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search messages",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        // Group info sheet
                        IconButton(onClick = { showGroupInfo = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Group info",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    } else {
                        // DM: avatar + name — tappable to open the other user's ProfileScreen
                        val headerPhoto = otherUserPhotoUrl.takeIf { it.isNotBlank() }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (otherUserProfile != null && onOpenProfile != null)
                                        Modifier.clickable { onOpenProfile(otherUserProfile!!) }
                                    else Modifier
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (headerPhoto != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(headerPhoto)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = conversationTitle.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = conversationTitle,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // DM MoreVert: search + remove friend
                        if (otherUid.isNotBlank()) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    offset = DpOffset(x = 0.dp, y = 4.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Search Messages") },
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
                                        onClick = { expanded = false; showSearchBar = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Remove Friend") },
                                        leadingIcon = { Icon(Icons.Default.PersonRemove, null) },
                                        onClick = { expanded = false; showRemoveFriendDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // ── Search bar — animates in/out below the header divider ──────
                AnimatedVisibility(
                    visible = showSearchBar,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    val matchCount = displayedMessages.size
                    val totalCount = messages.size
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search messages…") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            // Dismiss search entirely
                            IconButton(onClick = {
                                showSearchBar = false
                                searchQuery = ""
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        // Match count pill — only shown when query is active
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = if (matchCount == 0) "No results"
                                else "$matchCount of $totalCount message${if (totalCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, bottom = 4.dp),
                            )
                        }
                        HorizontalDivider()
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedMessages) { message ->
                        // Per-user check: has THIS user already accepted or declined?
                        // acceptedBy/declinedBy are comma-separated UIDs stored in metadata.
                        val acceptedBy = (message.metadata["acceptedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val declinedBy = (message.metadata["declinedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val alreadyResponded = currentUid in acceptedBy || currentUid in declinedBy
                        MessageBubble(
                            message = message,
                            isCurrentUser = message.senderId == currentUid,
                            currentUid = currentUid,
                            currentUserPhotoUrl = currentUserPhotoUrl,
                            highlightQuery = searchQuery.trim(),
                            onNavigateToLocation = onNavigateToLocation,
                            onNavigateToCoordinates = onNavigateToCoordinates,
                            onPreviewImage = { previewMessage = it },
                            onOpenSenderProfile = if (message.senderId != currentUid && onOpenProfile != null) {
                                {
                                    val profile = if (conversation.isGroup)
                                        memberProfiles[message.senderId]
                                    else
                                        otherUserProfile
                                    profile?.let { onOpenProfile(it) }
                                }
                            } else null,
                            onDeleteMessage = if (
                                conversation.roles[currentUid] == "admin" ||
                                message.senderId == currentUid
                            ) {
                                { scope.launch { repository.deleteMessage(conversation.id, message.id) } }
                            } else null,
                            onAccept = if (!alreadyResponded && message.senderId != currentUid &&
                                (message.type == "study_invite" || message.type == "event_invite")) {
                                {
                                    scope.launch {
                                        if (message.type == "study_invite") {
                                            val className = message.metadata["className"] as? String ?: ""
                                            val topic = message.metadata["topic"] as? String ?: ""
                                            val date = message.metadata["date"] as? String ?: ""
                                            val time = message.metadata["time"] as? String ?: ""
                                            val location = message.metadata["location"] as? String ?: ""
                                            android.util.Log.d("CalendarSave", "Saving study session: $className $topic $date $time")
                                            if (date.isNotBlank()) {
                                                android.util.Log.d("CalendarSave", "metadata: ${message.metadata}")
                                                android.util.Log.d("CalendarSave", "date: ${message.metadata["date"]}")
                                                calendarRepository.addStudySession(date, className, topic, time, location)
                                                android.util.Log.d("CalendarSave", "Study session saved successfully")
                                            } else {
                                                android.util.Log.e("CalendarSave", "Date is blank — metadata: ${message.metadata}")
                                            }
                                        } else {
                                            val name = message.metadata["name"] as? String ?: ""
                                            val date = message.metadata["date"] as? String ?: ""
                                            val time = message.metadata["time"] as? String ?: ""
                                            val location = message.metadata["location"] as? String ?: ""
                                            if (date.isNotBlank()) {
                                                calendarRepository.addEvent(date, name, time, location)
                                            }
                                        }
                                        repository.respondToInvite(conversation.id, message.id, "accepted")
                                        repository.sendMessage(conversation.id, "Accepted your invite!", "text")
                                    }
                                }
                            } else null,
                            onDecline = if (!alreadyResponded && message.senderId != currentUid &&
                                (message.type == "study_invite" || message.type == "event_invite")) {
                                {
                                    scope.launch {
                                        repository.respondToInvite(conversation.id, message.id, "declined")
                                    }
                                }
                            } else null
                        )
                    }
                }

                val textFieldState = rememberTextFieldState()

                // ── Step 1: pending attachment preview ───────────────────────────
                // Shown after the user picks a file but before they tap Send.
                AnimatedVisibility(
                    visible = pendingAttachment != null,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    pendingAttachment?.let { pa ->
                        PendingAttachmentPreview(
                            attachment = pa,
                            onCancel = { pendingAttachment = null },
                        )
                    }
                }

                // Upload progress bar — shown only during the actual upload
                if (isUploadingAttachment) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // ── Step 2: send ─────────────────────────────────────────────────
                // If a pending attachment exists, Send uploads + sends it.
                // Otherwise Send behaves as normal text send.
                MessageBox(
                    state = textFieldState,
                    onSendMessage = {
                        val pa = pendingAttachment
                        if (pa != null) {
                            val caption = textFieldState.text.toString().trim()
                            pendingAttachment = null
                            textFieldState.clearText()
                            isUploadingAttachment = true
                            scope.launch {
                                repository.sendAttachment(
                                    conversationId = conversation.id,
                                    uri = pa.uri,
                                    context = context,
                                    caption = caption,
                                )
                                isUploadingAttachment = false
                            }
                        } else {
                            val content = textFieldState.text.toString().trim()
                            if (content.isNotBlank()) {
                                scope.launch {
                                    repository.sendMessage(conversation.id, content)
                                    textFieldState.clearText()
                                }
                            }
                        }
                    },
                    studySelected = { showStudyInviteDialog = true },
                    eventSelected = { showEventInviteDialog = true },
                    onAttachmentClick = {
                        // Don't allow picking a new file while one is already
                        // staged or being uploaded
                        if (pendingAttachment == null && !isUploadingAttachment) {
                            attachmentLauncher.launch("*/*")
                        }
                    },
                    sendUserLocation = {
                        scope.launch {
                            val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000L
                            val recentlySent = messages.any { msg ->
                                msg.type == "location_share" &&
                                        msg.senderId == currentUid &&
                                        (msg.createdAt?.time ?: 0L) >= fiveMinutesAgo
                            }
                            if (recentlySent) {
                                snackbarHostState.showSnackbar("You already shared your location recently — try again in a few minutes.")
                                return@launch
                            }
                            val location = userLocation
                            if (location != null) {
                                repository.sendMessage(
                                    conversationId = conversation.id,
                                    content = "Shared their location.",
                                    type = "location_share",
                                    metadata = mapOf(
                                        "lat" to location.latitude.toString(),
                                        "lng" to location.longitude.toString(),
                                        "locationName" to "Current Location",
                                        "senderNickname" to currentUserNickname,
                                        "senderPhotoUrl" to currentUserPhotoUrl
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.imePadding()
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .padding(horizontal = 16.dp)
        ) { data ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    } // outer Box

    // Dismiss preview on back gesture
    BackHandler(enabled = previewMessage != null) { previewMessage = null }

    // Full-screen image preview overlay
    AnimatedVisibility(
        visible = previewMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        previewMessage?.let { msg ->
            ImagePreviewOverlay(
                message = msg,
                onDismiss = { previewMessage = null },
            )
        }
    }

    if (showStudyInviteDialog) {
        StudyInviteDialog(
            existingItems = myScheduleItems,
            existingStudySessions = myStudySessions,
            onDismiss = { showStudyInviteDialog = false },
            onSendExisting = { item ->
                scope.launch {
                    if (isDuplicateInvite("study_invite", item.className, item.date)) {
                        showStudyInviteDialog = false
                        snackbarHostState.showSnackbar("Already sent this study invite recently — try again in a few minutes.")
                        return@launch
                    }
                    val content = "Study invite: ${item.className} — ${item.assignmentName} on ${item.date} at ${item.dueTime}"
                    repository.sendMessage(
                        conversationId = conversation.id,
                        content = content,
                        type = "study_invite",
                        metadata = mapOf(
                            "className" to item.className,
                            "topic" to item.assignmentName,
                            "date" to item.date,
                            "time" to item.dueTime,
                            "location" to ""
                        )
                    )
                    // Add to sender's calendar as a study session (distinct from the assignment entry)
                    if (item.date.isNotBlank()) {
                        calendarRepository.addStudySession(item.date, item.className, item.assignmentName, item.dueTime, "")
                    }
                    showStudyInviteDialog = false
                }
            },
            onSendExistingSession = { session ->
                scope.launch {
                    if (isDuplicateInvite("study_invite", session.className, session.date)) {
                        showStudyInviteDialog = false
                        snackbarHostState.showSnackbar("Already sent this study invite recently — try again in a few minutes.")
                        return@launch
                    }
                    val content = "Study invite: ${session.className} — ${session.topic} on ${session.date} at ${session.startTime}"
                    repository.sendMessage(
                        conversationId = conversation.id,
                        content = content,
                        type = "study_invite",
                        metadata = mapOf(
                            "className" to session.className,
                            "topic" to session.topic,
                            "date" to session.date,
                            "time" to session.startTime,
                            "location" to session.location
                        )
                    )
                    // Session is already in sender's studySessions — no add needed
                    showStudyInviteDialog = false
                }
            },
            onSendNew = { cls, topic, date, time, location ->
                scope.launch {
                    if (isDuplicateInvite("study_invite", cls, date)) {
                        showStudyInviteDialog = false
                        snackbarHostState.showSnackbar("Already sent this study invite recently — try again in a few minutes.")
                        return@launch
                    }
                    val content = "Study invite: $cls — $topic on $date at $time"
                    repository.sendMessage(
                        conversationId = conversation.id,
                        content = content,
                        type = "study_invite",
                        metadata = mapOf("className" to cls, "topic" to topic, "date" to date, "time" to time, "location" to location)
                    )
                    // New session — save to sender's calendar immediately
                    if (date.isNotBlank()) {
                        calendarRepository.addStudySession(date, cls, topic, time, location)
                    }
                    showStudyInviteDialog = false
                }
            }
        )
    }

    if (showEventInviteDialog) {
        EventInviteSenderDialog(
            existingEvents = myEvents,
            onDismiss = { showEventInviteDialog = false },
            onSend = { name, date, time, location ->
                scope.launch {
                    if (isDuplicateInvite("event_invite", name, date)) {
                        showEventInviteDialog = false
                        snackbarHostState.showSnackbar("Already sent this invite recently — try again in a few minutes.")
                        return@launch
                    }
                    val content = "Event invite: $name on $date at $time"
                    repository.sendMessage(
                        conversationId = conversation.id,
                        content = content,
                        type = "event_invite",
                        metadata = mapOf("name" to name, "date" to date, "time" to time, "location" to location)
                    )
                    // Save to sender's calendar so they don't have to accept their own invite
                    if (date.isNotBlank()) {
                        calendarRepository.addEvent(date, name, time, location)
                    }
                    showEventInviteDialog = false
                }
            }
        )
    }
}

// Frontend team: restyle this bubble however you want
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    currentUid: String = "",
    currentUserPhotoUrl: String = "",
    highlightQuery: String = "",
    onNavigateToLocation: ((String) -> Unit)? = null,
    onNavigateToCoordinates: ((Double, Double, String, String) -> Unit)? = null,
    onPreviewImage: ((Message) -> Unit)? = null,
    onAccept: (() -> Unit)? = null,
    onDecline: (() -> Unit)? = null,
    onDeleteMessage: (() -> Unit)? = null,
    onOpenSenderProfile: (() -> Unit)? = null,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && onDeleteMessage != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Message?") },
            text = { Text("This will permanently remove the message for everyone.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDeleteMessage() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onDeleteMessage != null)
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showDeleteDialog = true }
                    )
                else Modifier
            ),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isCurrentUser) {
            val photoUrl = message.senderPhotoUrl.takeIf { it.isNotBlank() }
            val avatarClickModifier = if (onOpenSenderProfile != null)
                Modifier.clickable { onOpenSenderProfile() }
            else Modifier
            if (photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = avatarClickModifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                Box(
                    modifier = avatarClickModifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.senderNickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            if (!isCurrentUser) {
                Text(
                    text = message.senderNickname,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            if (message.type == "study_invite" || message.type == "event_invite") {
                InviteBubble(
                    message = message,
                    isCurrentUser = isCurrentUser,
                    currentUid = currentUid,
                    onAccept = onAccept,
                    onDecline = onDecline,
                    onNavigateToLocation = onNavigateToLocation,
                )
            } else if (message.type == "location_share") {
                LocationShareBubble(
                    message = message,
                    isCurrentUser = isCurrentUser,
                    onNavigateToLocation = onNavigateToLocation,
                    onNavigateToCoordinates = onNavigateToCoordinates
                )
            } else if (message.type == "attachment") {
                AttachmentBubble(
                    message = message,
                    isCurrentUser = isCurrentUser,
                    onPreviewImage = onPreviewImage,
                )
            } else {
                val bubbleColor = if (isCurrentUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (isCurrentUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isCurrentUser) 16.dp else 4.dp,
                        topEnd = if (isCurrentUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    color = bubbleColor,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        // Build an AnnotatedString that bolds + backgrounds any
                        // portion of the text matching the current search query.
                        val annotated = remember(message.content, highlightQuery, textColor) {
                            buildAnnotatedString {
                                if (highlightQuery.isBlank()) {
                                    append(message.content)
                                } else {
                                    val lower = message.content.lowercase()
                                    val query = highlightQuery.lowercase()
                                    var cursor = 0
                                    while (cursor < message.content.length) {
                                        val hit = lower.indexOf(query, cursor)
                                        if (hit == -1) {
                                            append(message.content.substring(cursor))
                                            break
                                        }
                                        append(message.content.substring(cursor, hit))
                                        withStyle(
                                            SpanStyle(
                                                background = Color(0xFFFFEB3B),
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        ) {
                                            append(message.content.substring(hit, hit + query.length))
                                        }
                                        cursor = hit + query.length
                                    }
                                }
                            }
                        }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                        )
                    }
                }
            }
        }

        if (isCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            val photoUrl = currentUserPhotoUrl.takeIf { it.isNotBlank() }
            if (photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Your profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.senderNickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Card-style bubble for study_invite and event_invite messages.
 * Shows type icon, title, subtitle (study only), date/time/location rows,
 * and either Accept/Decline buttons or the recorded response.
 */
@Composable
fun InviteBubble(
    message: Message,
    isCurrentUser: Boolean,
    currentUid: String = "",
    onAccept: (() -> Unit)?,
    onDecline: (() -> Unit)?,
    onNavigateToLocation: ((String) -> Unit)? = null,
) {
    val isStudy = message.type == "study_invite"
    val meta = message.metadata

    val typeLabel  = if (isStudy) "Study Session" else "Event Invite"
    val typeIcon   = if (isStudy) Icons.Default.School else Icons.Default.Event
    val title      = if (isStudy) meta["className"] as? String ?: "" else meta["name"] as? String ?: ""
    val subtitle   = if (isStudy) meta["topic"] as? String ?: "" else ""
    val date       = meta["date"] as? String ?: ""
    val time       = meta["time"] as? String ?: ""
    val location   = meta["location"] as? String ?: ""
    val response   = meta["response"] as? String

    val cardShape = RoundedCornerShape(
        topStart = if (isCurrentUser) 16.dp else 4.dp,
        topEnd   = if (isCurrentUser) 4.dp  else 16.dp,
        bottomStart = 16.dp,
        bottomEnd   = 16.dp,
    )

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.widthIn(min = 220.dp, max = 280.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Header: type icon + label + optional map pin ────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = typeLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f),
                )
                // Map pin button — taps into Map tab directions for this location
                if (location.isNotBlank() && onNavigateToLocation != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.18f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onNavigateToLocation(location) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "View on map",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                thickness = 0.5.dp,
            )
            Spacer(Modifier.height(8.dp))

            // ── Title + subtitle ─────────────────────────────────────
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Detail rows: date / time / location ──────────────────
            @Composable
            fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
                if (text.isBlank()) return
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    )
                }
            }

            DetailRow(Icons.Default.CalendarToday, date)
            DetailRow(Icons.Default.Schedule, time)
            DetailRow(Icons.Default.LocationOn, location)

            // ── Response status or Accept / Decline buttons ──────────
            when {
                response == "accepted" || currentUid in ((message.metadata["acceptedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()) -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "✓ Accepted",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    )
                }
                response == "declined" || currentUid in ((message.metadata["declinedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()) -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "✗ Declined",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    )
                }
                onAccept != null && onDecline != null -> {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                        ) { Text("Accept", color = MaterialTheme.colorScheme.onSecondaryContainer) }
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f),
                        ) { Text("Decline", color = MaterialTheme.colorScheme.onSecondaryContainer) }
                    }
                }
            }
        }
    }
}
@Composable
fun LocationShareBubble(
    message: Message,
    isCurrentUser: Boolean,
    onNavigateToLocation: ((String) -> Unit)? = null,
    onNavigateToCoordinates: ((Double, Double, String, String) -> Unit)? = null,
) {
    val locationName = message.metadata["locationName"] as? String ?: ""
    val lat = (message.metadata["lat"] as? String)?.toDoubleOrNull()
    val lng = (message.metadata["lng"] as? String)?.toDoubleOrNull()
    val senderNickname = message.metadata["senderNickname"] as? String ?: "Friend"
    val senderPhotoUrl = message.metadata["senderPhotoUrl"] as? String ?: ""
    val cardShape = RoundedCornerShape(
        topStart = if (isCurrentUser) 16.dp else 4.dp,
        topEnd = if (isCurrentUser) 4.dp else 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.widthIn(min = 220.dp, max = 280.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "LOCATION SHARE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(6.dp))

            if (lat != null && lng != null && onNavigateToCoordinates != null) {
                Button(
                    onClick = { onNavigateToCoordinates(lat, lng, senderNickname, senderPhotoUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text("View", color = MaterialTheme.colorScheme.onSecondaryContainer) }
            } else if (locationName.isNotBlank() && onNavigateToLocation != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onNavigateToLocation(locationName) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

/**
 * Renders an attachment message bubble.
 *
 * • Image MIME types  → inline [AsyncImage] (Coil), tap to open in system viewer
 * • All other types   → compact file card with [AttachFile] icon, file name, and an
 *                       "Open" button that fires [Intent.ACTION_VIEW]
 *
 * Shape, alignment, and sizing are consistent with the rest of the bubble family.
 */
@Composable
fun AttachmentBubble(
    message: Message,
    isCurrentUser: Boolean,
    onPreviewImage: ((Message) -> Unit)? = null,
) {
    val context = LocalContext.current
    val url      = message.metadata["url"]      as? String ?: ""
    val fileName = message.metadata["fileName"] as? String ?: "attachment"
    val mimeType = message.metadata["mimeType"] as? String ?: "application/octet-stream"
    val caption  = message.metadata["caption"]  as? String ?: ""

    val cardShape = RoundedCornerShape(
        topStart    = if (isCurrentUser) 16.dp else 4.dp,
        topEnd      = if (isCurrentUser) 4.dp  else 16.dp,
        bottomStart = 16.dp,
        bottomEnd   = 16.dp,
    )

    fun openUrl() {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, AndroidUri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    if (mimeType.startsWith("image/")) {
        // ── Image bubble — tap opens full-screen preview ──────────────────────────
        Surface(
            shape = cardShape,
            color = if (isCurrentUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .widthIn(min = 120.dp, max = 260.dp)
                .clickable {
                    if (onPreviewImage != null) onPreviewImage(message) else openUrl()
                },
        ) {
            Column {
                // Round bottom corners only when there's no caption below the image
                val imageShape = if (caption.isNotBlank()) RoundedCornerShape(
                    topStart    = if (isCurrentUser) 16.dp else 4.dp,
                    topEnd      = if (isCurrentUser) 4.dp  else 16.dp,
                    bottomStart = 0.dp,
                    bottomEnd   = 0.dp,
                ) else cardShape
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 240.dp)
                        .clip(imageShape),
                )
                if (caption.isNotBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrentUser)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    } else {
        // ── Generic file card ─────────────────────────────────────────────────────
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header row — icon + "ATTACHMENT" label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "ATTACHMENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    )
                }
                // File name sits directly under the label, above the divider
                Spacer(Modifier.height(2.dp))
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                    thickness = 0.5.dp,
                )
                Spacer(Modifier.height(8.dp))
                // Open button
                Button(
                    onClick = { openUrl() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Open")
                }
                // Caption — shown below the Open button when the sender added one
                if (caption.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
                        thickness = 0.5.dp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

/**
 * Full-screen image preview overlay, shown when the user taps an image attachment.
 *
 * Layout (mirrors Discord's viewer):
 *   • Near-black background — tap it to dismiss
 *   • Image fills available space with ContentScale.Fit (no cropping)
 *   • Floating top toolbar: [✕ close]  [filename]  [↓ download]
 *
 * Download uses DownloadManager so the file lands in the device's Downloads
 * folder and a system notification confirms completion — no extra permissions
 * needed on API 29+ (Android 10+).
 */
@Composable
fun ImagePreviewOverlay(
    message: Message,
    onDismiss: () -> Unit,
) {
    val context  = LocalContext.current
    val url      = message.metadata["url"]      as? String ?: ""
    val fileName = message.metadata["fileName"] as? String ?: "image"
    val mimeType = message.metadata["mimeType"] as? String ?: "image/*"

    fun downloadImage() {
        if (url.isBlank()) return
        runCatching {
            val request = DownloadManager.Request(AndroidUri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading via CINet")
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType(mimeType)
            val dm = context.getSystemService(DownloadManager::class.java)
            dm.enqueue(request)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0000000)) // ~94 % opaque black
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Image — consume clicks so tapping the photo doesn't dismiss the overlay
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = fileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 64.dp) // clear space for toolbar at top
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { /* consume — don't dismiss */ },
                ),
        )

        // ── Floating top toolbar ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0x88000000)) // translucent black scrim
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Close
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close preview",
                    tint = Color.White,
                )
            }

            // Filename — truncated in the middle of the toolbar
            Text(
                text = fileName,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )

            // Download
            IconButton(onClick = { downloadImage() }) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download image",
                    tint = Color.White,
                )
            }
        }
    }
}

/**
 * Preview card shown above the message box after the user picks a file
 * but before they tap Send (the 2-step attachment send flow).
 *
 * • Images  → thumbnail (loaded from the local URI via Coil) + filename
 * • Files   → AttachFile icon + filename
 * • ✕ button cancels the pending attachment without sending anything
 */
@Composable
private fun PendingAttachmentPreview(
    attachment: PendingAttachment,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (attachment.mimeType.startsWith("image/")) {
                // Thumbnail loaded directly from the local URI — no network needed
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(attachment.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = attachment.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                // Generic file icon in a small container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (attachment.mimeType.startsWith("image/")) "Image · tap ➤ to send"
                    else "File · tap ➤ to send",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                )
            }

            // Cancel — clears the pending attachment without sending
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel attachment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}