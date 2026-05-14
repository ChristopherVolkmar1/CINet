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
import com.example.cinet.feature.settings.canvas.viewmodel.CanvasSyncViewModel

/**
 * Settings sub-screen for connecting CINet to Canvas.
 *
 * Flow:
 *   1. User generates a Personal Access Token at csuci.instructure.com → Account → Settings → "+ New Access Token".
 *   2. Pastes it here and taps Save & Test.
 *   3. Once connected, can tap Sync Now to pull courses/assignments/events into the calendar.
 *   4. Can tap Disconnect at any time to wipe the local token.
 *
 * The actual sync logic lives in CanvasSyncService — this screen is pure UI
 * over CanvasSyncViewModel state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasConnectionScreen(
    onBack: () -> Unit,
    viewModel: CanvasSyncViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
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
                            "CINet can read your courses, assignments, calendar events, announcements, and to-dos."
                        else
                            "Paste a Personal Access Token to let CINet pull your Canvas data into the calendar.",
                        style = MaterialTheme.typography.bodySmall
                    )
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
                    // Mask like a password — tokens are credentials.
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
                // Connected — show sync + disconnect actions
                Button(
                    onClick = viewModel::onSyncNow,
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

            // Progress / status row
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

            // Skipped-items detail, only when present — keeps the calm path clean.
            val skipped = state.lastResult?.skipped.orEmpty()
            if (skipped.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Skipped",
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
