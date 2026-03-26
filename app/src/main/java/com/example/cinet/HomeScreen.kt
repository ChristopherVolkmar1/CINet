package com.example.cinet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {

            TopAppBar(
                title = { Text("CINet") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.tertiary,
                    actionIconContentColor = MaterialTheme.colorScheme.tertiary
                ),
                actions = {
                    IconButton(onClick = { expanded = true }) { //creates dropdown menu for settings
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Profile") },
                            onClick = { expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                expanded = false
                                onSettingsClick() //goes to settings page
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Log Out") },
                            onClick = { expanded = false }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.secondary)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome back to CINet!",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onMapClick,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 24.dp), // adjustable formatting
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("Maps")
            }
        }
    }
}
