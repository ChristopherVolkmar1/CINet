package com.example.cinet.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.cinet.AppSettings

private val DarkColorScheme = darkColorScheme(
    primary = CINetPrimaryDark,
    secondary = CINetSecondaryDark,
    tertiary = CINetTertiaryDark,
    onPrimary = CINetTertiaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = CINetPrimaryLight,
    secondary = CINetSecondaryLight,
    tertiary = CINetTertiaryLight,
    onPrimary = CINetTertiaryLight
)

@Composable
fun CINetTheme(
    darkTheme: Boolean = AppSettings.isDarkMode || isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}