package com.example.cinet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun WeatherDisplay(modifier: Modifier = Modifier, temp: String = "72°F", condition: String = "Partly Cloudy") {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isNight = hour < 6 || hour >= 18 // Simple rule: 6 PM to 6 AM is night

    // Adjust condition text if it's night and sunny
    val displayCondition = if (isNight && (condition.contains("Sunny", ignoreCase = true) || condition.contains("Clear", ignoreCase = true))) {
        "Clear Sky"
    } else {
        condition
    }

    val weatherIcon = when {
        displayCondition.contains("Clear Sky", ignoreCase = true) -> Icons.Default.NightsStay
        displayCondition.contains("Sunny", ignoreCase = true) || displayCondition.contains("Clear", ignoreCase = true) -> Icons.Default.WbSunny
        displayCondition.contains("Partly Cloudy", ignoreCase = true) -> Icons.Default.WbCloudy
        displayCondition.contains("Cloudy", ignoreCase = true) || displayCondition.contains("Cloud", ignoreCase = true) -> Icons.Default.Cloud
        displayCondition.contains("Rain", ignoreCase = true) || displayCondition.contains("Shower", ignoreCase = true) -> Icons.Default.Umbrella
        displayCondition.contains("Thunder", ignoreCase = true) -> Icons.Default.Thunderstorm
        else -> if (isNight) Icons.Default.NightsStay else Icons.Default.Cloud
    }

    Surface(
        modifier = modifier.height(60.dp),
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = weatherIcon,
                contentDescription = displayCondition,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = temp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = displayCondition,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    items: List<Pair<String, String>>,
    onAddClick: (() -> Unit)? = null,
    onItemClick: ((Int) -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSecondary
                )
                if (onAddClick != null) {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item",
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                thickness = 1.dp, 
                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.3f)
            )
            
            // Fixed height with scroll support when list is long
            Box(modifier = Modifier.heightIn(max = 250.dp)) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    items.forEachIndexed { index, (label, desc) ->
                        ListItem(
                            label = label, 
                            description = desc, 
                            onStarClick = { /* Action for favorite */ },
                            onArrowClick = { /* Action for navigation */ },
                            modifier = Modifier.clickable(enabled = onItemClick != null) {
                                onItemClick?.invoke(index)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListItem(
    label: String,
    description: String,
    onStarClick: () -> Unit = {},
    onArrowClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onSecondary
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Star Icon Button
        IconButton(
            onClick = onStarClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.StarBorder,
                contentDescription = "Favorite",
                modifier = Modifier.size(28.dp),
                tint = Color.Black // Dark contrast as shown in the image
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Text Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = contentColor
            )
            Text(
                text = description,
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
        
        // Arrow and Info Button
        IconButton(
            onClick = onArrowClick,
            modifier = Modifier.width(48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NorthWest,
                    contentDescription = "Navigate",
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
                Text(
                    text = "",
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = 16.sp
                )
            }
        }
    }
}
