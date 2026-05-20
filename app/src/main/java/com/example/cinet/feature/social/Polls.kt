package com.example.cinet.feature.social

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.Message

@Composable
fun PollsBubble(
    message: Message,
    isCurrentUser: Boolean,
    currentUid: String = "",
    onAccept: (() -> Unit)?,
    onDecline: (() -> Unit)?,
    onNavigateToLocation: ((String) -> Unit)? = null,
) {
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



        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollCreation(onDismiss: () -> Unit, onDelete: () -> Unit) {
    val question = rememberTextFieldState()
    var duration by remember { mutableStateOf("24 Hours") }
    var expanded by remember { mutableStateOf(false) }
    val times = listOf("1 Hour", "4 Hours", "8 Hours", "24 Hours")
    val valid = times.any()
    val answers = remember { mutableStateListOf("", "") }
    BasicAlertDialog (onDismissRequest = onDismiss,) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                Text(
                    text = "Create a Poll",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Question",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                TextField(
                    state = question,
                    lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 2),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    placeholder = { Text("What question do you want to ask?") },
                    textStyle = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Answers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                answers.forEachIndexed { index, answer ->
                    Answer(
                        value = answer,
                        onValueChange = { answers[index] = it },
                        onDelete = if (index >= 2) ({ answers.removeAt(index) }) else null
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (answers.size < 4) {
                    Button(onClick = { answers.add("") }) {
                        Text("+ Add Answer")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        readOnly = true,
                        placeholder = { Text("Select a time duration") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            times.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { duration = option; expanded = false }
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
fun Answer(value: String, onValueChange: (String) -> Unit, onDelete: (() -> Unit)?) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Type your answer") },
        trailingIcon = {
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        textStyle = MaterialTheme.typography.titleSmall
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPollCreation() {
    MaterialTheme {
        PollCreation(onDismiss = {}, onDelete = {})
    }
}