package com.example.cinet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DateInputBox(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(40.dp),
        color = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(text = label, color = Color.Gray)
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    items: List<Pair<String, String>>,
    titleColor: Color = MaterialTheme.colorScheme.tertiary
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondary,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
            
            items.forEach { (label, desc) ->
                ListItem(label = label, description = desc, tintColor = titleColor)
            }
        }
    }
}

@Composable
fun ListItem(
    label: String,
    description: String,
    tintColor: Color = MaterialTheme.colorScheme.tertiary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.StarBorder,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = tintColor
            )
            Text(text = description, color = Color.Gray, fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.NorthWest,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tintColor
            )
            Text(
                text = "A",
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
        }
    }
}
