package com.kynectia.app.ui.common

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
import com.kynectia.app.theme.*

/**
 * Logotipo oficial de KYNECTIA según el manual de identidad de marca.
 * Incluye la K estilizada con punto de cabeza atleta en gradiente Cian Tech -> Azul Eléctrico.
 */
@Composable
fun KynectiaBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showText: Boolean = true,
    showTagline: Boolean = false,
    horizontal: Boolean = true
) {
    if (horizontal) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KynectiaIsotype(size = size)
            if (showText) {
                Column {
                    Text(
                        text = "KYNECTIA",
                        fontFamily = Exo2,
                        fontWeight = FontWeight.Black,
                        fontSize = (size.value * 0.52f).sp,
                        letterSpacing = 2.sp,
                        color = Blanco,
                        lineHeight = (size.value * 0.55f).sp
                    )
                    if (showTagline) {
                        Text(
                            text = "ENTRENA • EVOLUCIONA • CONECTA",
                            fontFamily = Exo2,
                            fontWeight = FontWeight.Bold,
                            fontSize = (size.value * 0.22f).coerceAtLeast(9f).sp,
                            letterSpacing = 1.sp,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KynectiaIsotype(size = size)
            if (showText) {
                Text(
                    text = "KYNECTIA",
                    fontFamily = Exo2,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.45f).sp,
                    letterSpacing = 2.5.sp,
                    color = Blanco,
                    textAlign = TextAlign.Center
                )
                if (showTagline) {
                    Text(
                        text = "ENTRENA • EVOLUCIONA • CONECTA",
                        fontFamily = Exo2,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.20f).coerceAtLeast(9f).sp,
                        letterSpacing = 1.2.sp,
                        color = CianTech,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Isotipo: K tecnológica estilizada con silueta atlética y gradiente oficial.
 */
@Composable
fun KynectiaIsotype(
    size: Dp = 48.dp,
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
