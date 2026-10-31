package com.sunless.lynq.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkScheme = darkColorScheme(
    primary = DarkPalette.accentCyan,
    onPrimary = DarkPalette.bg,
    primaryContainer = DarkPalette.surfaceElevated,
    onPrimaryContainer = DarkPalette.textPrimary,
    secondary = DarkPalette.accentBlue,
    onSecondary = DarkPalette.bg,
    background = DarkPalette.bg,
    onBackground = DarkPalette.textPrimary,
    surface = DarkPalette.surface,
    onSurface = DarkPalette.textPrimary,
    surfaceVariant = DarkPalette.surfaceMuted,
    onSurfaceVariant = DarkPalette.textSecondary,
    outline = DarkPalette.border,
    outlineVariant = DarkPalette.borderSubtle,
    error = DarkPalette.emergencyRed,
    onError = DarkPalette.onEmergency,
)

private val LightScheme = lightColorScheme(
    primary = LightPalette.accentCyan,
    onPrimary = LightPalette.bg,
    primaryContainer = LightPalette.surfaceElevated,
    onPrimaryContainer = LightPalette.textPrimary,
    secondary = LightPalette.accentBlue,
    onSecondary = Color.White,
    background = LightPalette.bg,
    onBackground = LightPalette.textPrimary,
    surface = LightPalette.surface,
    onSurface = LightPalette.textPrimary,
    surfaceVariant = LightPalette.surfaceMuted,
    onSurfaceVariant = LightPalette.textSecondary,
    outline = LightPalette.border,
    outlineVariant = LightPalette.borderSubtle,
    error = LightPalette.emergencyRed,
    onError = LightPalette.onEmergency,
)

/** Modern refined corner curves following Apple HIG specifications. */
private val ModernShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun LynqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val scheme = if (darkTheme) DarkScheme else LightScheme

    CompositionLocalProvider(LocalLynqPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = LynqTypography,
            shapes = ModernShapes,
            content = content,
        )
    }
}