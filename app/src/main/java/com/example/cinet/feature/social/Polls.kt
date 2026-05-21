package com.example.cinet.feature.social

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties.Selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.Message
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PollsBubble(
    message: Message,
    isCurrentUser: Boolean,
    currentUid: String,
    onVoteSubmitted: (messageId: String, selectedIndex: Int) -> Unit
) {
    val cardShape = RoundedCornerShape(
        topStart = if (isCurrentUser) 16.dp else 4.dp,
        topEnd   = if (isCurrentUser) 4.dp  else 16.dp,
        bottomStart = 16.dp,
        bottomEnd   = 16.dp,
    )
    val createdAt = remember(message.metadata) {
        (message.metadata["createdAt"] as? String)?.toLongOrNull()
            ?: (message.metadata["createdAt"] as? Long)
            ?: System.currentTimeMillis()
    }

    val durationMillis = remember(message.metadata) {
        (message.metadata["duration"] as? String)?.toLongOrNull()
            ?: (message.metadata["duration"] as? Long)
            ?: 0L
    }
    val expiryTime = createdAt + durationMillis
    var isExpired by remember(expiryTime) {
        mutableStateOf(System.currentTimeMillis() >= expiryTime)
    }

    LaunchedEffect(expiryTime) {
        if (System.currentTimeMillis() >= expiryTime) {
            isExpired = true
            return@LaunchedEffect
        }
        val timeRemaining = expiryTime - System.currentTimeMillis()
        if (timeRemaining > 0) {
            delay(timeRemaining)
            isExpired = true
        }
    }
    val votedUids = remember(message.metadata) {
        (message.metadata["votedUids"] as? String)
            ?.split("||")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }
    val hasVotedServer = votedUids.contains(currentUid)

    // userVotes: "uid1:0||uid2:2" — maps each uid to the option index they chose.
    // We look this up instead of using votedUids.indexOf(), which would give the
    // voter's position in the list (wrong) rather than their actual option choice.
    val serverVotedIndex = remember(message.metadata) {
        (message.metadata["userVotes"] as? String)
            ?.split("||")
            ?.filter { it.isNotBlank() }
            ?.firstOrNull { it.startsWith("$currentUid:") }
            ?.substringAfter(":")
            ?.toIntOrNull()
            ?: -1
    }

    var localSelectedIndex by remember(message.id) { mutableIntStateOf(-1) }
    val finalSelectedIndex = if (hasVotedServer) serverVotedIndex else localSelectedIndex

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.widthIn(min = 220.dp, max = 280.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (!isExpired && !hasVotedServer && localSelectedIndex == -1) {
                PollVoting(
                    message = message,
                    selectedIndex = localSelectedIndex,
                    onVoteSelected = { index  ->
                        localSelectedIndex = index
                        onVoteSubmitted(message.id, index)
                    }
                )
            } else {
                PollResults(
                    message = message,
                    selectedIndex = finalSelectedIndex,
                    voting = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollCreation(
    onDismiss: () -> Unit,
    onSend: (question: String, answers: List<String>, durationMillis: Long) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("24 Hours") }
    var expanded by remember { mutableStateOf(false) }
    val times = listOf("5 Minutes", "1 Hours", "4 Hours", "8 Hours", "24 Hours")
    val answers = remember { mutableStateListOf("", "") }

    BasicAlertDialog (onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
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
                OutlinedTextField(
                    value = question,
                    onValueChange = { if (it.length <= 200) question = it },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
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
                    Button(
                        onClick = { answers.add("") },
                        modifier = Modifier.align(Alignment.End)
                    ) {
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
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
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

                Button(
                    onClick = {
                        if (question.isNotBlank() && answers.all { it.isNotBlank() }) {
                            val durationMillis = when (duration) {
                                "5 Minutes" -> 5 * 60 * 1000L
                                "1 Hour"    -> 1 * 60 * 60 * 1000L
                                "4 Hours"   -> 4 * 60 * 60 * 1000L
                                "8 Hours"   -> 8 * 60 * 60 * 1000L
                                else        -> 24 * 60 * 60 * 1000L
                            }
                            onSend(question, answers.toList(), durationMillis)
                        }
                        //enabled = question.isNotBlank() && answers.all { it.isNotBlank() }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun PollVoting(
    message: Message,
    selectedIndex: Int,
    onVoteSelected: (Int) -> Unit
) {
    val answers = remember(message.id) {
        (message.metadata["answers"] as? String)
            ?.split("||")
            ?.toMutableStateList()
            ?: mutableStateListOf()
    }
    Text(
        text = message.content,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer
    )
    Text(
        text = "Select one answer",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer
    )

    answers.forEachIndexed { index, option ->
        OutlinedButton(
            onClick = { onVoteSelected(index) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selectedIndex == index)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else
                    Color.Transparent
            ),
            border = BorderStroke(
                width = if (selectedIndex == index) 2.dp else 1.dp,
                color = if (selectedIndex == index)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline
            )
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.RadioButtonUnchecked,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun PollResults(
    message: Message,
    selectedIndex: Int,
    voting: Boolean,
) {
    val answers = remember(message.metadata) {
        (message.metadata["answers"] as? String)
            ?.split("||") ?: emptyList()
    }
    val voteCounts = remember(message.metadata) {
        (message.metadata["votes"] as? String)
            ?.split("||")
            ?.map { it.toIntOrNull() ?: 0 }
            ?: List(answers.size) { 0 }
    }
    /*val finalCount = voteCounts.mapIndexed { index, count ->
        if (selectedIndex == index) count + 1 else count
    }*/
    val totalVotes = voteCounts.sum()
    Text(
        text = message.content,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer
    )
    Text(
        text = "Review choices",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer
    )

    answers.forEachIndexed { index, option ->
        val votes = voteCounts.getOrElse(index) { 0 }
        val percentage = if (totalVotes > 0) {
            ((votes.toFloat() / totalVotes.toFloat()) * 100).roundToInt()
        } else {
            0
        }
        val isSelected = index == selectedIndex
        OutlinedButton(
            onClick = {  },
            enabled = voting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selectedIndex == index)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else
                    Color.Transparent
            ),
            border = BorderStroke(
                width = if (selectedIndex == index) 2.dp else 1.dp,
                color = if (selectedIndex == index)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline
            )
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if(isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun Answer(value: String, onValueChange: (String) -> Unit, onDelete: (() -> Unit)?) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 200) onValueChange(it) },
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

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewPollCreation() {
    MaterialTheme {
        //PollCreation(onDismiss = {}, onSend = { _, _, _ -> })
        PollsBubble(
            message = Message(
                id = "1",
                senderId = "user1",
                content = "What should we study?",
                type = "poll",
                metadata = mapOf(
                    "question" to "What should we study?",
                    "answers" to "Algorithms||Data Structures||Operating Systems",
                    "duration" to "86400000",
                    "createdAt" to System.currentTimeMillis().toString()
                )
            ),
            isCurrentUser = false,
            currentUid = "user_test",
            onVoteSubmitted = { _, _ -> }
        )
    }
}