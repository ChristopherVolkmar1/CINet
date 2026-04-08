package com.example.cinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    user: UserProfile,
    currentUserProfile: UserProfile,
    onOpenConversation: (Conversation) -> Unit,
    onBack: () -> Unit,
) {
    val repository = SocialRepository()
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Frontend team: add avatar/photo here using user.photoUrl

            Text(text = user.nickname, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.pronouns,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.major,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        repository.getOrCreateConversation(
                            participantIds = listOf(currentUserProfile.uid, user.uid),
                            participantNicknames = mapOf(
                                currentUserProfile.uid to currentUserProfile.nickname,
                                user.uid to user.nickname
                            )
                        ).onSuccess { onOpenConversation(it) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Message")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}