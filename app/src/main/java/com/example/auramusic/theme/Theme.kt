package com.example.auramusic.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Data class representing Google Pixel Material 3 Expressive Monochrome Theme Tokens
data class PixelThemeColors(
    val isDark: Boolean,
    val background: Color,
    val surfaceDim: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val outline: Color,
    val outlineVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val cardBackground: Color,
    val activePillBackground: Color,
    val activePillText: Color,
    val miniPlayerBackground: Color,
    val bottomNavBackground: Color
)

// True OLED Pitch Black Theme (Dark Mode)
val DarkPixelTheme = PixelThemeColors(
    isDark = true,
    background = Color(0xFF000000),             // OLED True Pitch Black
    surfaceDim = Color(0xFF0A0A0A),
    surface = Color(0xFF121212),                // Standard Cards
    surfaceContainer = Color(0xFF181818),       // Tonal Containers
    surfaceHigh = Color(0xFF222222),            // Elevated Cards
    surfaceHighest = Color(0xFF2E2E2E),         // Active Pills
    outline = Color(0xFF383838),
    outlineVariant = Color(0xFF262626),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA0A0A0),
    textTertiary = Color(0xFF666666),
    cardBackground = Color(0xFF121212),
    activePillBackground = Color(0xFFFFFFFF),
    activePillText = Color(0xFF000000),
    miniPlayerBackground = Color(0xFF161616),
    bottomNavBackground = Color(0xFF000000)
)

// Clean Material 3 Expressive Monochrome Light Theme (Light Mode)
val LightPixelTheme = PixelThemeColors(
    isDark = false,
    background = Color(0xFFF7F7F7),             // Clean Porcelain Light
    surfaceDim = Color(0xFFEFEFEF),
    surface = Color(0xFFFFFFFF),                // Crisp White Cards
    surfaceContainer = Color(0xFFEEEEEE),       // Soft Light Gray
    surfaceHigh = Color(0xFFE2E2E2),            // Elevated Containers
    surfaceHighest = Color(0xFFD6D6D6),         // Active Pills
    outline = Color(0xFFD0D0D0),
    outlineVariant = Color(0xFFE4E4E4),
    textPrimary = Color(0xFF111111),            // Stark Black Text
    textSecondary = Color(0xFF555555),          // Graphite Secondary
    textTertiary = Color(0xFF888888),
    cardBackground = Color(0xFFFFFFFF),
    activePillBackground = Color(0xFF111111),
    activePillText = Color(0xFFFFFFFF),
    miniPlayerBackground = Color(0xFFFFFFFF),
    bottomNavBackground = Color(0xFFF7F7F7)
)

val LocalPixelColors = staticCompositionLocalOf { DarkPixelTheme }

object PixelTheme {
    val colors: PixelThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPixelColors.current
}


@Composable
fun AuraMusicTheme(
    isDarkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val pixelColors = if (isDarkMode) DarkPixelTheme else LightPixelTheme
    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = pixelColors.textPrimary,
            onPrimary = pixelColors.background,
            primaryContainer = pixelColors.surfaceHighest,
            onPrimaryContainer = pixelColors.textPrimary,
            secondary = pixelColors.textSecondary,
            onSecondary = pixelColors.background,
            background = pixelColors.background,
            onBackground = pixelColors.textPrimary,
            surface = pixelColors.surface,
            onSurface = pixelColors.textPrimary,
            surfaceVariant = pixelColors.surfaceContainer,
            outline = pixelColors.outline,
            outlineVariant = pixelColors.outlineVariant
        )
    } else {
        lightColorScheme(
            primary = pixelColors.textPrimary,
            onPrimary = pixelColors.background,
            primaryContainer = pixelColors.surfaceHighest,
            onPrimaryContainer = pixelColors.textPrimary,
            secondary = pixelColors.textSecondary,
            onSecondary = pixelColors.background,
            background = pixelColors.background,
            onBackground = pixelColors.textPrimary,
            surface = pixelColors.surface,
            onSurface = pixelColors.textPrimary,
            surfaceVariant = pixelColors.surfaceContainer,
            outline = pixelColors.outline,
            outlineVariant = pixelColors.outlineVariant
        )
    }

    CompositionLocalProvider(LocalPixelColors provides pixelColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
