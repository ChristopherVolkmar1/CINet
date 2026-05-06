package com.example.cinet.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.feature.profile.viewmodel.ProfileEditState
import com.example.cinet.feature.profile.viewmodel.ProfileEditViewModel

private val yearOptions = listOf(
    "Freshman", "Sophomore", "Junior", "Senior", "Graduate", "Transfer"
)

private val interestOptions = listOf(
    "Gaming", "Music", "Sports", "Fitness", "Art", "Photography",
    "Cooking", "Reading", "Travel", "Hiking", "Movies", "Technology",
    "Coffee", "Anime", "Dance", "Yoga", "Volunteering", "Study Groups"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit = {},
    viewModel: ProfileEditViewModel = viewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val state by viewModel.state.collectAsState()

    var nickname by remember(profile) { mutableStateOf(profile?.nickname ?: "") }
    var major by remember(profile) { mutableStateOf(profile?.major ?: "") }
    var pronouns by remember(profile) { mutableStateOf(profile?.pronouns ?: "") }
    var year by remember(profile) { mutableStateOf(profile?.year ?: "") }
    var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var selectedInterests by remember(profile) {
        mutableStateOf(profile?.interests?.toSet() ?: emptySet())
    }
    var yearExpanded by remember { mutableStateOf(false) }

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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Avatar (display only — upload coming later)
            val photoUrl = profile?.photoUrl?.takeIf { it.isNotBlank() }
            if (photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl).crossfade(true).build(),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(100.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile?.nickname?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }
            Text(
                "Profile photo synced from Google",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Nickname
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Major
            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                label = { Text("Major") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                        pronouns = pronouns,
                        year = year,
                        bio = bio,
                        interests = selectedInterests.toList()
                    )
                },
                enabled = state !is ProfileEditState.Loading && nickname.isNotBlank(),
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