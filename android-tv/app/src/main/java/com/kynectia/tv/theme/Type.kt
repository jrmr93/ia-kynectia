package com.kynectia.tv.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kynectia.tv.R

// Tipografía Oficial de Marca: Exo 2 ("Moderna, fuerte y tecnológica")
val Exo2 = FontFamily(
    Font(R.font.exo2_regular, FontWeight.Normal),
    Font(R.font.exo2_medium, FontWeight.Medium),
    Font(R.font.exo2_semibold, FontWeight.SemiBold),
    Font(R.font.exo2_bold, FontWeight.Bold),
    Font(R.font.exo2_black, FontWeight.Black)
)

val TvTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Black,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 48.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Exo2,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)
