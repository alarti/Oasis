package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val OasisDarkScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    secondary = Color(0xFF49454F),
    onSecondary = Color(0xFFE8DEF8),
    tertiary = Color(0xFF64B5F6),
    background = Color(0xFF0F1113),
    surface = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark serene theme
    dynamicColor: Boolean = false, // Disable dynamic to keep the custom mood
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OasisDarkScheme,
        typography = Typography,
        content = content
    )
}
