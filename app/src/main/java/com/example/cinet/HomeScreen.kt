package com.example.cinet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cinet.ui.theme.CINetTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Welcome back to CINet, [user]", //hardcoded until usernames are implemented
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Weather replaces the Date Input Boxes
                WeatherDisplay(
                    modifier = Modifier.fillMaxWidth(),
                    temp = "72°F",
                    condition = "Sunny - Camarillo, CA"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Today's Schedule Section hardcoded till calendar works
        InfoSection(title = "Today's Schedule", items = listOf(
            "CS 450: Software Engineering" to "10:00 AM - Bell Tower 1302",
            "Lunch with Study Group" to "12:30 PM - Student Union",
            "Math 300: Discrete Structures" to "2:00 PM - Sierra Hall 110"
        ))

        Spacer(modifier = Modifier.height(16.dp))

        // Upcoming Events Section
        InfoSection(title = "Upcoming Events", items = listOf(
            "Career Fair" to "Tomorrow - Petit Salon",
            "Pizza Night" to "Friday - Central Mall"
        ))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CINetTheme {
        HomeScreen()
    }
}
