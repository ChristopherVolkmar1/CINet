package com.example.cinet.feature.auth


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val pronounOptions = listOf(
    "He/Him",
    "He/They",
    "She/Her",
    "She/They",
    "They/Them",
    "Ze/Zir",
    "Xe/Xem",
    "Prefer not to say",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onSaveProfile: (nickname: String, major: String, pronouns: String) -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var selectedPronouns by remember { mutableStateOf(pronounOptions.first()) }
    var pronounsExpanded by remember { mutableStateOf(false) }
    var nicknameError by remember { mutableStateOf(false) }
    var majorError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set up your profile",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This is how other students will see you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    nicknameError = false
                },
                label = { Text("Nickname") },
                singleLine = true,
                isError = nicknameError,
                supportingText = if (nicknameError) {
                    { Text("Nickname is required") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = major,
                onValueChange = {
                    major = it
                    majorError = false
                },
                label = { Text("Major") },
                singleLine = true,
                isError = majorError,
                supportingText = if (majorError) {
                    { Text("Major is required") }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = pronounsExpanded,
                onExpandedChange = { pronounsExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPronouns,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pronouns") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = pronounsExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = pronounsExpanded,
                    onDismissRequest = { pronounsExpanded = false }
                ) {
                    pronounOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedPronouns = option
                                pronounsExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    nicknameError = nickname.isBlank()
                    majorError = major.isBlank()
                    if (!nicknameError && !majorError) {
                        onSaveProfile(nickname.trim(), major.trim(), selectedPronouns)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        }
    }
}