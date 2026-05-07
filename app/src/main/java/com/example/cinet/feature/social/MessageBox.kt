package com.example.cinet.feature.social

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.cinet.ui.theme.CINetTheme

@Composable
fun MessageBox(
    state: TextFieldState,
    onSendMessage: () -> Unit,
    studySelected: () -> Unit,
    eventSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.Bottom
    ) {
        // Study/Event invite pop up buttons
        Box() {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Send a study or event invite.",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

            }
            AddAttachment(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                onStudyClick = studySelected,
                onEventClick = eventSelected
            )
        }
        // Message bar section
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .weight(1f)
        ) {
            TextField(
                value = state.text.toString(),
                onValueChange = { new ->
                    state.edit { replace(0, length, new) }
                },
                placeholder = { Text("Message") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
        }

        // Send message button
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp)
        ) {
            IconButton(onClick = onSendMessage) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
@Composable
fun AddAttachment(
    expanded: Boolean,
    onStudyClick: () -> Unit,
    onEventClick: () -> Unit,
    onDismiss: () -> Unit
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            offset = DpOffset(x = 0.dp, y = -8.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Study Invite")},
                leadingIcon = { Icon(Icons.Default.School, "Study Invite") },
                onClick = {
                    onStudyClick()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text("Event Invite")},
                leadingIcon = { Icon(Icons.Default.Event, "Event Invite") },
                onClick = {
                    onEventClick()
                    onDismiss()
                }
            )
        }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewMessageBox() {
    CINetTheme(darkTheme = true) {
        val textFieldState = rememberTextFieldState()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            MessageBox(
                state = textFieldState,
                onSendMessage = { },
                studySelected = { },
                eventSelected = { }
            )
        }
    }
}