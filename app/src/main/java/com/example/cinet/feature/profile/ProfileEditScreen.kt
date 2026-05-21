package com.example.cinet.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.feature.profile.viewmodel.ProfileEditState
import com.example.cinet.feature.profile.viewmodel.ProfileEditViewModel
import com.example.cinet.feature.profile.viewmodel.UploadState

private val yearOptions = listOf(
    "Freshman", "Sophomore", "Junior", "Senior", "Graduate", "Transfer"
)

private val interestOptions = listOf(
    "Gaming", "Music", "Sports", "Fitness", "Art", "Photography",
    "Cooking", "Reading", "Travel", "Hiking", "Movies", "Technology",
    "Coffee", "Anime", "Dance", "Yoga", "Volunteering", "Study Groups"
)

private val AVATAR_SIZE    = 100.dp
private val AVATAR_OVERLAP = 50.dp   // how far the avatar hangs below the banner
private val BANNER_HEIGHT  = 180.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit = {},
    viewModel: ProfileEditViewModel = viewModel(),
    showTopBar: Boolean = true
) {
    val profile     by viewModel.profile.collectAsState()
    val state       by viewModel.state.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val isUploading = uploadState is UploadState.Loading

    var nickname by remember(profile) { mutableStateOf(profile?.nickname ?: "") }
    var major    by remember(profile) { mutableStateOf(profile?.major ?: "") }
    var minor    by remember(profile) { mutableStateOf(profile?.minor ?: "") }
    val context      = LocalContext.current
    val programList  = loadProgramsFromRaw(context).sortedBy { it.name }
    val majorList    = programList.filter { it.type == "Major" }
    val minorList    = programList.filter { it.type == "Minor" }

    var pronouns          by remember(profile) { mutableStateOf(profile?.pronouns ?: "") }
    var year              by remember(profile) { mutableStateOf(profile?.year ?: "") }
    var bio               by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var selectedInterests by remember(profile) {
        mutableStateOf(profile?.interests?.toSet() ?: emptySet())
    }
    var yearExpanded  by remember { mutableStateOf(false) }
    var majorExpanded by remember { mutableStateOf(false) }
    var minorExpanded by remember { mutableStateOf(false) }
    val validMajor = majorList.any { it.name == major }
    val validMinor = minorList.any { it.name == minor } || minor.isBlank()

    // Image pickers
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadProfilePhoto(it) } }

    val bannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadBannerPhoto(it) } }

    // Navigate back automatically once save succeeds
    LaunchedEffect(state) {
        if (state is ProfileEditState.Success) {
            viewModel.resetState()
            onSaved()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Edit Profile") },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Banner + Avatar header ────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BANNER_HEIGHT)
                        .clickable(enabled = !isUploading) { bannerLauncher.launch("image/*") }
                ) {
                    val bannerUrl = profile?.bannerUrl?.takeIf { it.isNotBlank() }
                    if (bannerUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(bannerUrl).crossfade(true).build(),
                            contentDescription = "Profile banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                )
                        )
                    }
                    // Bottom gradient so avatar has contrast
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                                )
                            )
                    )
                    // Upload spinner over banner
                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    // Camera affordance (bottom-end)
                    if (!isUploading) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change banner",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Avatar — centred, overlapping the bottom of the banner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = AVATAR_OVERLAP)
                        .size(AVATAR_SIZE)
                        .clip(CircleShape)
                        .clickable(enabled = !isUploading) { photoLauncher.launch("image/*") }
                ) {
                    val photoUrl = profile?.photoUrl?.takeIf { it.isNotBlank() }
                    if (photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoUrl).crossfade(true).build(),
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile?.nickname?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Semi-transparent camera overlay on avatar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.30f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isUploading) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change photo",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            // Space for the overlapping avatar
            Spacer(Modifier.height(AVATAR_OVERLAP + 4.dp))

            // Upload error — shown persistently so the user can read it
            if (uploadState is UploadState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = (uploadState as UploadState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.resetUploadError() }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // ── Form fields ───────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Nickname
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Major
                ExposedDropdownMenuBox(
                    expanded = majorExpanded,
                    onExpandedChange = { majorExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = major,
                        onValueChange = { major = it },
                        isError = major.isNotBlank() && !validMajor,
                        supportingText = {
                            if (major.isNotBlank() && !validMajor) Text("Please select a valid major.")
                        },
                        readOnly = false,
                        label = { Text("Major") },
                        placeholder = { Text("Select your major") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = majorExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    val filtering = majorList.filter { it.name.contains(major, ignoreCase = true) }
                    if (filtering.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = majorExpanded,
                            onDismissRequest = { majorExpanded = false }
                        ) {
                            filtering.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = { major = option.name; majorExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Minor
                ExposedDropdownMenuBox(
                    expanded = minorExpanded,
                    onExpandedChange = { minorExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = minor,
                        onValueChange = { minor = it },
                        isError = minor.isNotBlank() && !validMinor,
                        supportingText = {
                            if (minor.isNotBlank() && !validMinor) Text("Please select a valid minor.")
                        },
                        readOnly = false,
                        label = { Text("Minor") },
                        placeholder = { Text("Select your minor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minorExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    val filtering = minorList.filter { it.name.contains(minor, ignoreCase = true) }
                    if (filtering.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = minorExpanded,
                            onDismissRequest = { minorExpanded = false }
                        ) {
                            filtering.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = { minor = option.name; minorExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Pronouns
                OutlinedTextField(
                    value = pronouns,
                    onValueChange = { pronouns = it },
                    label = { Text("Pronouns") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Year dropdown
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Year / Class Standing") },
                        placeholder = { Text("Select year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false }
                    ) {
                        yearOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { year = option; yearExpanded = false }
                            )
                        }
                    }
                }

                // Bio
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 150) bio = it },
                    label = { Text("Bio") },
                    placeholder = { Text("Tell other students a bit about yourself…") },
                    minLines = 3,
                    maxLines = 4,
                    supportingText = { Text("${bio.length}/150") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Interests
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                        interestOptions.forEach { interest ->
                            FilterChip(
                                selected = interest in selectedInterests,
                                onClick = {
                                    selectedInterests = if (interest in selectedInterests)
                                        selectedInterests - interest
                                    else
                                        selectedInterests + interest
                                },
                                label = { Text(interest) }
                            )
                        }
                    }
                }

                if (state is ProfileEditState.Error) {
                    Text(
                        text = (state as ProfileEditState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        viewModel.saveProfile(
                            nickname = nickname,
                            major = major,
                            minor = minor,
                            pronouns = pronouns,
                            year = year,
                            bio = bio,
                            interests = selectedInterests.toList()
                        )
                    },
                    enabled = state !is ProfileEditState.Loading
                            && !isUploading
                            && nickname.isNotBlank()
                            && validMajor
                            && validMinor,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (state is ProfileEditState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes")
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}