package com.kynectia.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KynectiaDarkColorScheme = darkColorScheme(
    primary = AzulElectrico,
    onPrimary = Blanco,
    primaryContainer = Color(0xFF003875),
    onPrimaryContainer = CianTech,

    secondary = CianTech,
    onSecondary = NegroProfundo,
    secondaryContainer = Color(0xFF003844),
    onSecondaryContainer = CianTech,

    tertiary = Color(0xFF00B4D8),
    onTertiary = Blanco,

    background = NegroProfundo,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = DarkBorder,
    outlineVariant = DarkBorderActive,

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
