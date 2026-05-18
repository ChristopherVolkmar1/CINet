package com.example.cinet.feature.settings.canvas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.remote.canvas.CanvasDisplaySettings
import com.example.cinet.feature.settings.canvas.viewmodel.CanvasSyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasConnectionScreen(
    onBack: () -> Unit,
    onSyncComplete: () -> Unit = {},
    viewModel: CanvasSyncViewModel = viewModel(),
    showTopBar: Boolean = true
) {
    val state by viewModel.uiState.collectAsState()
    val showCanvasInCalendar = CanvasDisplaySettings.showCanvasInCalendar

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("Canvas Sync") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection status card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (state.hasToken) "Canvas connected" else "Canvas not connected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (state.hasToken)
                            "CINet pulls your starred Canvas courses, their assignments, calendar events, announcements, and to-dos."
                        else
                            "Paste a Personal Access Token to let CINet pull your Canvas data into the calendar.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // How favorites work — only show once connected, when it's relevant.
            if (state.hasToken) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Which courses show up?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Only the courses you've starred on Canvas's \"All Courses\" page " +
                                    "appear in the calendar. To add or remove a course, toggle its star " +
                                    "in Canvas, then tap Sync Now here.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (!state.hasToken) {
                // Token entry flow
                Text(
                    text = "Generate a token",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "In Canvas, go to Account → Settings → \"+ New Access Token\". " +
                            "Give it a name like \"CINet\" and (optionally) an expiry date, then copy the value " +
                            "that appears once — Canvas won't show it again.",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = state.tokenInput,
                    onValueChange = viewModel::onTokenInputChange,
                    label = { Text("Canvas access token") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = viewModel::onSaveAndTest,
                    enabled = !state.isBusy && state.tokenInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isBusy) "Working…" else "Save & Test")
                }
            } else {
                Button(
                    onClick = { viewModel.onSyncNow(onSyncComplete) },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isBusy) "Syncing…" else "Sync Now")
                }
                OutlinedButton(
                    onClick = viewModel::onDisconnect,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Disconnect Canvas")
                }
            }

            // Master toggle: hides ALL Canvas content from the calendar at once.
            // Stays useful even with per-course favorites — it's a single switch
            // to silence everything Canvas-related without un-starring each course.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Canvas items in calendar",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (showCanvasInCalendar)
                                "Starred Canvas courses and their assignments appear in the calendar."
                            else
                                "All Canvas content is hidden from the calendar (data is still stored).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = showCanvasInCalendar,
                        onCheckedChange = { CanvasDisplaySettings.showCanvasInCalendar = it }
                    )
                }
            }

            // Progress / status
            if (state.isBusy) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(state.statusMessage ?: "Working…")
                }
            } else {
                state.statusMessage?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            val skipped = state.lastResult?.skipped.orEmpty()
            if (skipped.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Notes from this sync",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        skipped.forEach { item ->
                            Text(
                                text = "• $item",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
