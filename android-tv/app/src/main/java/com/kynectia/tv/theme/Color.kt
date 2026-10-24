package com.kynectia.tv.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// PALETA OFICIAL KYNECTIA (MANUAL DE MARCA)
// ==========================================
val NegroProfundo = Color(0xFF0B0F14)  // #0B0F14
val AzulElectrico = Color(0xFF007BFF)  // #007BFF
val CianTech = Color(0xFF00E0FF)       // #00E0FF
val Blanco = Color(0xFFFFFFFF)         // #FFFFFF

// Gradiente Oficial de Marca
val KynectiaBrandGradient = Brush.linearGradient(
    colors = listOf(CianTech, AzulElectrico)
)
val KynectiaHorizontalGradient = Brush.horizontalGradient(
    colors = listOf(CianTech, AzulElectrico)
)

// Superficies Dark Tech para Android TV
val DarkBackground = NegroProfundo
val DarkSurface = Color(0xFF101620)
val DarkSurfaceVariant = Color(0xFF161F2C)
val DarkBorder = Color(0xFF1E2838)
val DarkBorderActive = CianTech.copy(alpha = 0.5f)

// Textos
val TextPrimary = Blanco
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Acentos de Rendimiento / Estado
val CalorieOrange = Color(0xFFFF5722)
val SuccessGreen = Color(0xFF00E5A3)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Compatibilidad retroactiva: mapeados a la nueva identidad de marca
val KynectiaBlue = AzulElectrico
val KynectiaBlueLight = Color(0xFF389BFF)
val KynectiaBlueDark = Color(0xFF0056B3)
val KynectiaCyan = CianTech
val KynectiaLime = AzulElectrico       // Reemplazo del verde lima antiguo por Azul Eléctrico oficial
val KynectiaLimeDark = Color(0xFF0056B3)
val KynectiaOrange = CalorieOrange
