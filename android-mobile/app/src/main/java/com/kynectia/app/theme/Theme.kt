package com.kynectia.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KynectiaDarkColorScheme = darkColorScheme(
    primary = KynectiaLime,
    onPrimary = Color(0xFF0D1117),
    primaryContainer = Color(0xFF1E3300),
    onPrimaryContainer = KynectiaLime,

    secondary = KynectiaOrange,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF3B150A),
    onSecondaryContainer = Color(0xFFFFB4A1),

    tertiary = KynectiaCyan,
    onTertiary = Color(0xFF00363D),

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = Color.White
)

@Composable
fun KynectiaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KynectiaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
