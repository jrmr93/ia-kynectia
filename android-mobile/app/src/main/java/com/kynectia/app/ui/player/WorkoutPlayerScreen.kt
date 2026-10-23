package com.kynectia.app.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kynectia.app.data.model.WorkoutSummaryResult
import com.kynectia.app.data.tv.TvCastManager
import com.kynectia.app.theme.*
import com.kynectia.app.ui.tv.TvCastDialog
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlayerScreen(
    viewModel: WorkoutPlayerViewModel,
    onWorkoutFinished: (WorkoutSummaryResult) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isTvConnected by TvCastManager.isTvConnected.collectAsState()
    var showTvCastDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isWorkoutFinished, state.summaryResult) {
        if (state.isWorkoutFinished && state.summaryResult != null) {
            onWorkoutFinished(state.summaryResult!!)
        }
    }

    if (showTvCastDialog) {
        TvCastDialog(
            athleteName = "Atleta Kynectia",
            onDismissRequest = {
                showTvCastDialog = false
                viewModel.syncStateToTv()
            }
        )
    }

    val routine = state.routine

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = routine?.name ?: "Entrenamiento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                        val minutes = state.elapsedWorkoutSeconds / 60
                        val seconds = state.elapsedWorkoutSeconds % 60
                        Text(
                            text = String.format(Locale.getDefault(), "⏱️ %02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = KynectiaLime
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Salir", tint = TextPrimary)
                    }
                },
                actions = {
                    // Botón para Transmitir a Pantalla Android TV
                    IconButton(onClick = { showTvCastDialog = true }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = "Transmitir a TV",
                                tint = if (isTvConnected) KynectiaLime else TextSecondary
                            )
                            if (isTvConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(KynectiaLime)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.togglePause() }) {
                        Icon(
                            if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isPaused) "Reanudar" else "Pausar",
                            tint = KynectiaLime
                        )
                    }
                    IconButton(onClick = { viewModel.finishWorkout() }) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Finalizar", tint = KynectiaOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },

        containerColor = DarkBackground
    ) { paddingValues ->
        if (routine == null || routine.exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = KynectiaLime)
            }
            return@Scaffold
        }

        val currentExercise = routine.exercises.getOrNull(state.currentExerciseIndex)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Barra de progreso de la rutina
                val progress = (state.currentExerciseIndex + 1).toFloat() / routine.exercises.size.toFloat()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ejercicio ${state.currentExerciseIndex + 1} de ${routine.exercises.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = KynectiaLime
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = KynectiaLime,
                        trackColor = DarkSurfaceVariant
                    )
                }

                if (currentExercise != null) {
                    // Tarjeta Principal del Ejercicio
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!currentExercise.demo_image_url.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentExercise.demo_image_url,
                                    contentDescription = currentExercise.exercise_name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Text(
                                text = currentExercise.exercise_name ?: "Ejercicio #${currentExercise.sequence_order}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Orden #${currentExercise.sequence_order}", color = KynectiaLime) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurfaceVariant),
                                    border = null
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Descanso: ${currentExercise.rest_seconds}s", color = TextSecondary) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurfaceVariant),
                                    border = null
                                )
                            }

                            if (!currentExercise.specific_instructions.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "💡 ${currentExercise.specific_instructions}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Tarjeta de Registro de la Serie Actual
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SERIE ${state.currentSetNumber} DE ${currentExercise.sets}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = KynectiaLime,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Stepper de Repeticiones
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Repeticiones", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.updateRepsInput(-1) },
                                            modifier = Modifier.size(36.dp).background(DarkSurface, CircleShape)
                                        ) {
                                            Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                        Text(
                                            text = "${state.completedRepsInput}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 14.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.updateRepsInput(+1) },
                                            modifier = Modifier.size(36.dp).background(DarkSurface, CircleShape)
                                        ) {
                                            Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                    }
                                }

                                // Stepper de Carga (kg)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Carga (kg)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.updateWeightInput(-2.5) },
                                            modifier = Modifier.size(36.dp).background(DarkSurface, CircleShape)
                                        ) {
                                            Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1f", state.weightKgInput),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 14.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.updateWeightInput(+2.5) },
                                            modifier = Modifier.size(36.dp).background(DarkSurface, CircleShape)
                                        ) {
                                            Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Botón de Completar Serie
                            Button(
                                onClick = { viewModel.completeSet() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KynectiaLime,
                                    contentColor = Color(0xFF0D1117)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "COMPLETAR SERIE",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // Navegación entre ejercicios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.previousExercise() },
                            enabled = state.currentExerciseIndex > 0,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anterior")
                        }

                        Button(
                            onClick = { viewModel.nextExercise() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Siguiente Ejercicio")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // MODAL / OVERLAY INTERACTIVO DEL TEMPORIZADOR DE DESCANSO
            AnimatedVisibility(
                visible = state.isResting,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE60D1117))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, KynectiaLime)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⏱️ TIEMPO DE DESCANSO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = KynectiaLime
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Reloj circular interactivo
                            val restProgress = state.restSecondsRemaining.toFloat() / maxOf(1, state.restSecondsTotal).toFloat()
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(160.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { restProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 10.dp,
                                    color = KynectiaLime,
                                    trackColor = DarkSurfaceVariant
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${state.restSecondsRemaining}",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "segundos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Ajustes rápidos de tiempo (+15s, -15s)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.addRestTime(-15) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Text("-15s")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.addRestTime(+15) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Text("+15s")
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Botón Saltar Descanso
                            Button(
                                onClick = { viewModel.skipRest() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KynectiaLime, contentColor = Color(0xFF0D1117)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("SALTAR DESCANSO", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
