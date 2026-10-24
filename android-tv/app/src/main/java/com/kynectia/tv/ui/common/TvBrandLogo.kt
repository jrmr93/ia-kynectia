package com.kynectia.tv.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kynectia.tv.theme.*

/**
 * Logotipo oficial de KYNECTIA para Google TV y pantallas grandes.
 */
@Composable
fun TvBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showText: Boolean = true,
    subtitle: String? = "Tu entrenador en casa",
    horizontal: Boolean = true
) {
    if (horizontal) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TvIsotype(size = size)
            if (showText) {
                Column {
                    Text(
                        text = "KYNECTIA",
                        fontFamily = Exo2,
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.52f).sp,
                        letterSpacing = 3.sp,
                        color = Blanco,
                        lineHeight = (size.value * 0.55f).sp
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontFamily = Exo2,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (size.value * 0.22f).coerceAtLeast(11f).sp,
                            letterSpacing = 1.2.sp,
                            color = CianTech
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvIsotype(size = size)
            if (showText) {
                Text(
                    text = "KYNECTIA",
                    fontFamily = Exo2,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.48f).sp,
                    letterSpacing = 3.5.sp,
                    color = Blanco,
                    textAlign = TextAlign.Center
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = Exo2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (size.value * 0.22f).coerceAtLeast(11f).sp,
                        letterSpacing = 1.5.sp,
                        color = CianTech,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Isotipo: K estilizada con punto de cabeza atleta en gradiente Cian Tech -> Azul Eléctrico.
 */
@Composable
fun TvIsotype(
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val gradient = Brush.linearGradient(
            colors = listOf(CianTech, AzulElectrico),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        // Punto / Cabeza de la silueta atlética
        drawCircle(
            brush = gradient,
            radius = w * 0.09f,
            center = Offset(w * 0.76f, h * 0.18f)
        )

        // Columna vertical izquierda
        val colPath = Path().apply {
            moveTo(w * 0.18f, h * 0.14f)
            lineTo(w * 0.34f, h * 0.14f)
            lineTo(w * 0.34f, h * 0.86f)
            lineTo(w * 0.18f, h * 0.86f)
            close()
        }
        drawPath(colPath, gradient)

        // Brazo diagonal superior
        val topArm = Path().apply {
            moveTo(w * 0.34f, h * 0.50f)
            lineTo(w * 0.64f, h * 0.24f)
            lineTo(w * 0.84f, h * 0.24f)
            lineTo(w * 0.48f, h * 0.56f)
            lineTo(w * 0.34f, h * 0.50f)
            close()
        }
        drawPath(topArm, gradient)

        // Pierna diagonal inferior
        val bottomLeg = Path().apply {
            moveTo(w * 0.42f, h * 0.51f)
            lineTo(w * 0.78f, h * 0.86f)
            lineTo(w * 0.60f, h * 0.86f)
            lineTo(w * 0.34f, h * 0.60f)
            lineTo(w * 0.34f, h * 0.50f)
            close()
        }
        drawPath(bottomLeg, gradient)
    }
}

/**
 * Franja y Slogan oficial: ENTRENA • EVOLUCIONA • CONECTA
 */
@Composable
fun TvSloganBanner(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "ENTRENA",
            fontFamily = Exo2,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            letterSpacing = 2.sp,
            color = Blanco
        )
        Text(
            text = "EVOLUCIONA",
            fontFamily = Exo2,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            letterSpacing = 2.sp,
            color = Blanco
        )
        Text(
            text = "CONECTA",
            fontFamily = Exo2,
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            letterSpacing = 2.sp,
            color = CianTech
        )
    }
}
