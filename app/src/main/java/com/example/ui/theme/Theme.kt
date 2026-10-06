package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GlowPrimary,
    onPrimary = GlowWhite,
    primaryContainer = GlowDarkPlum,
    onPrimaryContainer = GlowSurfaceLight,
    secondary = GlowSecondary,
    onSecondary = GlowWhite,
    secondaryContainer = GlowDarkPlum,
    onSecondaryContainer = GlowBgLight,
    tertiary = GlowSecondary,
    background = GlowDark,
    onBackground = GlowBgLight,
    surface = Color(0xFF381230),
    onSurface = GlowWhite,
    surfaceVariant = GlowDarkPlum,
    onSurfaceVariant = GlowSurfaceLight,
    outline = Color(0xFF863B74)
)

private val LightColorScheme = lightColorScheme(
    primary = GlowPrimary,
    onPrimary = GlowWhite,
    primaryContainer = GlowSurfaceLight,
    onPrimaryContainer = GlowDark,
    secondary = GlowSecondary,
    onSecondary = GlowWhite,
    secondaryContainer = GlowSurfaceLight,
    onSecondaryContainer = GlowDarkPlum,
    tertiary = GlowDarkPlum,
    background = GlowBgLight,
    onBackground = GlowDark,
    surface = GlowWhite,
    onSurface = GlowDark,
    surfaceVariant = GlowSurfaceLight,
    onSurfaceVariant = GlowDarkPlum,
    outline = GlowBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
