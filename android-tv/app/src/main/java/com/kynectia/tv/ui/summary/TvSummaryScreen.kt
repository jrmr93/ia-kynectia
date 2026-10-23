package com.kynectia.tv.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kynectia.tv.data.TvWorkoutSummary
import com.kynectia.tv.theme.*
import com.kynectia.tv.ui.TvDisplayOrientation
import java.util.Locale

@Composable
fun TvSummaryScreen(
    athleteName: String,
    routineName: String,
    summary: TvWorkoutSummary,
    orientationMode: TvDisplayOrientation = TvDisplayOrientation.HORIZONTAL,
    onReturnToPairing: () -> Unit
) {
    val mins = summary.totalDurationSeconds / 60
    val secs = summary.totalDurationSeconds % 60
    val formattedDuration = String.format(Locale.US, "%02d:%02d", mins, secs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(if (orientationMode.isVerticalLayout) 24.dp else 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Medalla / Trofeo animado
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF263D04)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = KynectiaLime,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¡ENTRENAMIENTO COMPLETADO!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = KynectiaLime,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gran sesión, ${athleteName.ifBlank { "Atleta" }}. Has superado la rutina $routineName.",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (orientationMode.isVerticalLayout) {
                // DISTRIBUCIÓN VERTICAL COMPLETA: Tarjetas apiladas ocupando el 100% del ancho
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TvStatCard(
                        icon = Icons.Default.Timer,
                        label = "DURACIÓN TOTAL",
                        value = formattedDuration,
                        unit = "minutos de entrenamiento",
                        accentColor = KynectiaLime,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TvStatCard(
                        icon = Icons.Default.CheckCircle,
                        label = "SERIES COMPLETADAS",
                        value = "${summary.totalSetsCompleted}",
                        unit = "series totales realizadas",
                        accentColor = Color(0xFF00E5FF),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TvStatCard(
                        icon = Icons.Default.FitnessCenter,
                        label = "VOLUMEN LEVANTADO",
                        value = String.format(Locale.US, "%.1f", summary.totalVolumeKg),
                        unit = "kilogramos acumulados",
                        accentColor = KynectiaOrange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // DISTRIBUCIÓN HORIZONTAL: 3 Tarjetas en fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TvStatCard(
                        icon = Icons.Default.Timer,
                        label = "DURACIÓN TOTAL",
                        value = formattedDuration,
                        unit = "minutos",
                        accentColor = KynectiaLime,
                        modifier = Modifier.weight(1f)
                    )

                    TvStatCard(
                        icon = Icons.Default.CheckCircle,
                        label = "SERIES COMPLETADAS",
                        value = "${summary.totalSetsCompleted}",
                        unit = "series totales",
                        accentColor = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f)
                    )

                    TvStatCard(
                        icon = Icons.Default.FitnessCenter,
                        label = "VOLUMEN LEVANTADO",
                        value = String.format(Locale.US, "%.1f", summary.totalVolumeKg),
                        unit = "kg acumulados",
                        accentColor = KynectiaOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onReturnToPairing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KynectiaLime,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(if (orientationMode.isVerticalLayout) 1f else 0.6f)
                    .height(54.dp)
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Finalizar y Preparar Nueva Sesión",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TvStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}
