package com.example.auramusic.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// True OLED Monochrome Scheme (Pitch Black with High-Contrast White and Grayscale Tiers)
private val MonochromeColorScheme = darkColorScheme(
    primary = MonochromeWhite,
    onPrimary = MonochromeBlack,
    primaryContainer = MonochromeSurfaceHighest,
    onPrimaryContainer = MonochromeWhite,
    secondary = MonochromeLightGrey,
    onSecondary = MonochromeBlack,
    secondaryContainer = MonochromeSurfaceContainer,
    onSecondaryContainer = MonochromeWhite,
    tertiary = MonochromeSilver,
    onTertiary = MonochromeBlack,
    background = MonochromeBlack,
    onBackground = MonochromeWhite,
    surface = MonochromeSurface,
    onSurface = MonochromeWhite,
    surfaceVariant = MonochromeSurfaceContainer,
    onSurfaceVariant = MonochromeLightGrey,
    outline = MonochromeOutline,
    outlineVariant = MonochromeOutlineVariant
)

@Composable
fun AuraMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We enforce the Google Pixel Expressive Monochrome theme (OLED pitch black)
    MaterialTheme(
        colorScheme = MonochromeColorScheme,
        typography = Typography,
        content = content
    )
}
