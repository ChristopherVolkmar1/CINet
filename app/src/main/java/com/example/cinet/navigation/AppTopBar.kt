package com.example.cinet.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Draws the persistent page title bar used across the app.
@Composable
internal fun AppTopBar(
    state: NavigationTopBarState,
    onBack: () -> Unit,
    onFriendsClick: () -> Unit,
    onNewMessageClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(start = 20.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = state.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 50.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (state.showSocialActions) {
                SocialTopBarActions(
                    pendingRequestCount = state.pendingRequestCount,
                    onFriendsClick = onFriendsClick,
                    onNewMessageClick = onNewMessageClick
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

// Shows the friends and new-message actions used by the Messages page.
@Composable
private fun SocialTopBarActions(
    pendingRequestCount: Int,
    onFriendsClick: () -> Unit,
    onNewMessageClick: () -> Unit,
) {
    BadgedBox(
        badge = {
            if (pendingRequestCount > 0) {
                Badge { Text(pendingRequestCount.toString()) }
            }
        }
    ) {
        IconButton(onClick = onFriendsClick) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = "Friends",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Box(contentAlignment = Alignment.Center) {
        IconButton(onClick = onNewMessageClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "New conversation",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
