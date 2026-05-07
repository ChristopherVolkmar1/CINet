package com.example.cinet.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.settings.AppSettings

enum class AppThemeColor(
    val light: Color,
    val dark: Color,
    val lightButton: Color,
    val darkButton: Color,
    val displayName: String
) {
    Green(
        light = GreenLight,
        dark = GreenDark,
        lightButton = CINetButtonGreenLight,
        darkButton = CINetButtonGreenDark,
        displayName = "Green"
    ),
    Orange(
        light = OrangeLight,
        dark = OrangeDark,
        lightButton = ButtonOrangeLight,
        darkButton = ButtonOrangeDark,
        displayName = "Orange"
    ),
    Yellow(
        light = YellowLight,
        dark = YellowDark,
        lightButton = ButtonYellowLight,
        darkButton = ButtonYellowDark,
        displayName = "Yellow"
    ),
    Purple(
        light = PurpleLight,
        dark = PurpleDark,
        lightButton = ButtonPurpleLight,
        darkButton = ButtonPurpleDark,
        displayName = "Purple"
    ),
    Blue(
        light = BlueLight,
        dark = BlueDark,
        lightButton = ButtonBlueLight,
        darkButton = ButtonBlueDark,
        displayName = "Blue"
    )
}

fun getDynamicColorScheme(selectedTheme: AppThemeColor, isDark: Boolean): ColorScheme {
    val themeColor = if (isDark) selectedTheme.dark else selectedTheme.light
    val buttonColor = if (isDark) selectedTheme.darkButton else selectedTheme.lightButton

    return if (isDark) {
        darkColorScheme(
            primary = themeColor,
            onPrimary = Color.White,
            secondary = themeColor,
            onSecondary = Color.White,
            tertiary = CINetTertiaryDark,
            onTertiary = Color.White,
            secondaryContainer = buttonColor,
            onSecondaryContainer = Color.White,
            background = Color(0xFF121212),
            onBackground = Color.White,
            surface = Color(0xFF1E1E1E),
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = themeColor,
            onPrimary = Color.White,
            secondary = themeColor,
            onSecondary = Color.White,
            tertiary = CINetTertiaryLight,
            onTertiary = Color.White,
            secondaryContainer = buttonColor,
            onSecondaryContainer = Color.White,
            background = CINetBackground,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black
        )
    }
}

@Composable
fun ThemeSelector(
    selectedTheme: AppThemeColor,
    onThemeChange: (AppThemeColor) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 4.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Color",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    selectedTheme.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppThemeColor.entries.forEach { themeOption ->
                    val isSelected = themeOption == selectedTheme

                    Button(
                        shape = CircleShape,
                        onClick = { onThemeChange(themeOption) },
                        modifier = Modifier
                            .size(42.dp)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Transparent,
                                shape = CircleShape
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeOption.light
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Preview() {
    CINetTheme(darkTheme = true) {
        Box(Modifier.padding(16.dp)) {
            ThemeSelector(
                selectedTheme = AppThemeColor.Orange,
                onThemeChange = {}
            )
        }
    }
}

@Composable
fun CINetTheme(
    darkTheme: Boolean = AppSettings.isDarkMode,
    selectedColor: AppThemeColor = AppThemeColor.Green,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> getDynamicColorScheme(selectedColor, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
