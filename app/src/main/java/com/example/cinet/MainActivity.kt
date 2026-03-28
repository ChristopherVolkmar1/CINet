package com.example.cinet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.ui.theme.CINetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a Notification Channel for this app which is required to make notifications appear
        NotificationHelper.createChannel(this)

        // Requests Notification Permission, required for android 13+
        if (!PermissionManager.hasAllPermissions(this)) {
            PermissionManager.requestAllPermissions(this)
        }

        enableEdgeToEdge()
        setContent {
            CINetTheme {
                // State to keep track of the current screen
                NavigationHandler()
            }
        }
    }
}