package com.kynectia.app.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kynectia.app.data.model.WorkoutSummaryResult
import com.kynectia.app.theme.*
import java.util.Locale

@Composable
fun WorkoutSummaryScreen(
    summary: WorkoutSummaryResult,
    onFinish: () -> Unit
) {
    val scrollState = rememberScrollState()

    val minutes = summary.totalDurationSeconds / 60
    val seconds = summary.totalDurationSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Cabecera de Celebración
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xFF1E3300)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = KynectiaLime,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            text = "¡ENTRENAMIENTO COMPLETADO!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = KynectiaLime,
            textAlign = TextAlign.Center
        )

        Text(
            text = summary.routineName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        // Resumen de Métricas Clave
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Tiempo Total",
                value = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = KynectiaLime) },
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Series Hechas",
                value = "${summary.totalSetsCompleted}",
                icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = KynectiaOrange) },
                modifier = Modifier.weight(1f)
            )
        }

        MetricCard(
            title = "Volumen Total Levantado",
            value = String.format(Locale.getDefault(), "%,.1f kg", summary.totalVolumeKg),
            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = KynectiaCyan) },
            modifier = Modifier.fillMaxWidth()
        )

        // Desglose por ejercicio
        Text(
            text = "Desglose por Ejercicio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )

        summary.exercisesSummary.forEach { exercise ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${exercise.sets.size} series completadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        exercise.sets.forEach { set ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "${set.completedReps}r @ ${set.weightKg}k",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KynectiaLime
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurfaceVariant),
                                border = null
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KynectiaLime,
                contentColor = Color(0xFF0D1117)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("VOLVER AL INICIO", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
