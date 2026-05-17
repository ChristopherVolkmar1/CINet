package com.example.cinet.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Shows the bottom navigation bar and sends selected tabs back to MainScaffold.
@Composable
internal fun NavigationBottomBar(
    isVisible: Boolean,
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
) {
    if (!isVisible) return

    NavigationBar {
        Screen.entries.forEach { screen ->
            NavigationBarItem(
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
                label = {
                    Text(
                        text = screen.label,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
