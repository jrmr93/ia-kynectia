package com.kynectia.tv.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvDarkColorScheme = darkColorScheme(
    primary = AzulElectrico,
    onPrimary = Blanco,
    primaryContainer = Color(0xFF003875),
    onPrimaryContainer = CianTech,

    secondary = CianTech,
    onSecondary = NegroProfundo,

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
fun KynectiaTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = TvTypography,
        content = content
    )
}
