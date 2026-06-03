package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val CyberDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonViolet,
    tertiary = NeonAmber,
    background = CyberBlack,
    surface = CyberSlate,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = CyberSlateLight,
    onPrimaryContainer = TextPrimary
)

private val CyberLightColorScheme = darkColorScheme( // Default to elegant dark theme since user specified dark & minimal!
    primary = NeonCyan,
    secondary = NeonViolet,
    tertiary = NeonAmber,
    background = CyberBlack,
    surface = CyberSlate,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    primaryContainer = CyberSlateLight,
    onPrimaryContainer = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark theme by default
    dynamicColor: Boolean = false, // Preserve our cyber colors instead of overriding them dynamically
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CyberDarkColorScheme else CyberLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
