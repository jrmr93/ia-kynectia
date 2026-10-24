package com.kynectia.tv.ui.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kynectia.tv.data.TvApiClient
import com.kynectia.tv.data.TvLiveWorkoutState
import com.kynectia.tv.theme.*
import com.kynectia.tv.ui.TvDisplayOrientation
import com.kynectia.tv.ui.TvExerciseVideoPlayer
import java.util.Locale

/**
 * Función auxiliar para cálculo de 1RM según fórmula de Epley: Peso * (1 + Reps / 30)
 */
fun calculateOneRm(weightKg: Double, reps: Int): Int {
    if (weightKg <= 0.0 || reps <= 0) return 0
    if (reps == 1) return weightKg.toInt()
    return (weightKg * (1.0 + reps / 30.0)).toInt()
}

@Composable
fun TvLiveWorkoutScreen(
    athleteName: String,
    state: TvLiveWorkoutState,
    orientationMode: TvDisplayOrientation,
    isSoundEnabled: Boolean,
    onRotateScreen: () -> Unit,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit,
    onSkipRest: () -> Unit,
    onNextExercise: () -> Unit,
    onPrevExercise: () -> Unit,
    onDisconnect: () -> Unit
) {
    if (orientationMode.isVerticalLayout) {
        TvLiveWorkoutVerticalContent(
            athleteName = athleteName,
            state = state,
            orientationMode = orientationMode,
            isSoundEnabled = isSoundEnabled,
            onRotateScreen = onRotateScreen,
            onToggleSound = onToggleSound,
            onTogglePause = onTogglePause,
            onSkipRest = onSkipRest,
            onNextExercise = onNextExercise,
            onPrevExercise = onPrevExercise,
            onDisconnect = onDisconnect
        )
    } else {
        TvLiveWorkoutHorizontalContent(
            athleteName = athleteName,
            state = state,
            orientationMode = orientationMode,
            isSoundEnabled = isSoundEnabled,
            onRotateScreen = onRotateScreen,
            onToggleSound = onToggleSound,
            onTogglePause = onTogglePause,
            onSkipRest = onSkipRest,
            onNextExercise = onNextExercise,
            onPrevExercise = onPrevExercise,
            onDisconnect = onDisconnect
        )
    }
}

/**
 * DISEÑO HORIZONTAL (16:9 tradicional para TV estándar)
 */
@Composable
private fun TvLiveWorkoutHorizontalContent(
    athleteName: String,
    state: TvLiveWorkoutState,
    orientationMode: TvDisplayOrientation,
    isSoundEnabled: Boolean,
    onRotateScreen: () -> Unit,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit,
    onSkipRest: () -> Unit,
    onNextExercise: () -> Unit,
    onPrevExercise: () -> Unit,
    onDisconnect: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val calculatedOneRm = remember(state.weightKg, state.reps) {
        calculateOneRm(state.weightKg, state.reps)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1B2E00),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = KynectiaLime,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = state.routineName.ifBlank { "RUTINA EN VIVO" }.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ATLETA: ${athleteName.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = KynectiaLime,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Indicador de estado, 1RM en cabecera y cronómetro total
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Badge 1RM en la barra superior
                if (calculatedOneRm > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF101B2B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF203248)),
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1RM: ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = "$calculatedOneRm kg",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = KynectiaBlueLight
                            )
                        }
                    }
                }

                // Live Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (state.isPaused) KynectiaOrange else KynectiaLime.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isPaused) "EN PAUSA" else "EN VIVO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (state.isPaused) KynectiaOrange else KynectiaLime,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Total Workout Timer Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val mins = state.elapsedWorkoutSeconds / 60
                        val secs = state.elapsedWorkoutSeconds % 60
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // MAIN CONTENT AREA (Split Left / Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LEFT COLUMN: Ejercicio actual, Video/Imagen y Barra de Segmentos
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cabecera del ejercicio con orden y 1RM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant
                        ) {
                            Text(
                                text = "EJERCICIO ${state.currentExerciseIndex + 1} DE ${state.totalExercises.coerceAtLeast(1)}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SERIES: ${state.totalSets}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            if (calculatedOneRm > 0) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF101B2B)
                                ) {
                                    Text(
                                        text = "1RM: $calculatedOneRm kg",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = KynectiaBlueLight
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = state.exerciseName.ifBlank { "Preparando ejercicio..." },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    // Video / Imagen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val resolvedVideoUrl = TvApiClient.resolveMediaUrl(state.demoVideoUrl)
                        val resolvedImageUrl = TvApiClient.resolveMediaUrl(state.demoImageUrl)

                        if (!resolvedVideoUrl.isNullOrBlank()) {
                            TvExerciseVideoPlayer(
                                videoUrl = resolvedVideoUrl,
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = 16.dp
                            )
                        } else if (!resolvedImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = resolvedImageUrl,
                                contentDescription = state.exerciseName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Demostración en Pantalla",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // INDICADOR DE SEGMENTOS DE LA RUTINA (Como en móvil)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progreso de ejercicios",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            val progressPct = if (state.totalExercises > 0) {
                                (((state.currentExerciseIndex + 1).toFloat() / state.totalExercises) * 100).toInt()
                            } else 0
                            Text(
                                text = "$progressPct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KynectiaBlueLight
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        TvExerciseSegmentsIndicator(
                            totalExercises = state.totalExercises,
                            currentIndex = state.currentExerciseIndex
                        )
                    }
                }
            }

            // RIGHT COLUMN: Modo Descanso o Métricas Activas + Tabla de Series
            Card(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isResting) Color(0xFF0F1A08) else DarkSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (state.isResting) KynectiaLime.copy(alpha = 0.6f) else DarkBorder
                )
            ) {
                if (state.isResting) {
                    TvRestModeContent(
                        restRemaining = state.restSecondsRemaining,
                        restTotal = state.restSecondsTotal,
                        nextSet = state.currentSetNumber,
                        totalSets = state.totalSets,
                        weightKg = state.weightKg,
                        reps = state.reps
                    )
                } else {
                    TvActiveSetContent(
                        currentSet = state.currentSetNumber,
                        totalSets = state.totalSets,
                        reps = state.reps,
                        weightKg = state.weightKg,
                        oneRm = calculatedOneRm
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // BARRA INFERIOR DE CONTROL REMOTO Y ROTACIÓN
        TvRemoteControlBar(
            isPaused = state.isPaused,
            isResting = state.isResting,
            isSoundEnabled = isSoundEnabled,
            orientationLabel = orientationMode.label,
            onTogglePause = onTogglePause,
            onSkipRest = onSkipRest,
            onNextExercise = onNextExercise,
            onPrevExercise = onPrevExercise,
            onRotateScreen = onRotateScreen,
            onToggleSound = onToggleSound,
            onDisconnect = onDisconnect
        )
    }
}

/**
 * DISEÑO VERTICAL (Adaptado para pantallas montadas verticalmente 9:16 y smart mirrors)
 */
@Composable
private fun TvLiveWorkoutVerticalContent(
    athleteName: String,
    state: TvLiveWorkoutState,
    orientationMode: TvDisplayOrientation,
    isSoundEnabled: Boolean,
    onRotateScreen: () -> Unit,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit,
    onSkipRest: () -> Unit,
    onNextExercise: () -> Unit,
    onPrevExercise: () -> Unit,
    onDisconnect: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_vertical")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val calculatedOneRm = remember(state.weightKg, state.reps) {
        calculateOneRm(state.weightKg, state.reps)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. HEADER VERTICAL (100% ANCHO)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1B2E00),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = KynectiaLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = state.routineName.ifBlank { "RUTINA EN VIVO" }.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ATLETA: ${athleteName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = KynectiaLime,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (calculatedOneRm > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF101B2B),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "1RM: $calculatedOneRm kg",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = KynectiaBlueLight,
                            fontSize = 9.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (state.isPaused) KynectiaOrange else KynectiaLime.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isPaused) "PAUSA" else "VIVO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (state.isPaused) KynectiaOrange else KynectiaLime,
                            fontSize = 10.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurfaceVariant
                ) {
                    val mins = state.elapsedWorkoutSeconds / 60
                    val secs = state.elapsedWorkoutSeconds % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", mins, secs),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. TARJETA PRINCIPAL DEL EJERCICIO CON INDICADOR DE SEGMENTOS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(4.8f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceVariant
                    ) {
                        Text(
                            text = "EJERCICIO ${state.currentExerciseIndex + 1} DE ${state.totalExercises.coerceAtLeast(1)}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }

                    Text(
                        text = "SERIES: ${state.totalSets}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }

                Text(
                    text = state.exerciseName.ifBlank { "Preparando ejercicio..." },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // IMAGEN / VIDEO DEL EJERCICIO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val resolvedVideoUrl = TvApiClient.resolveMediaUrl(state.demoVideoUrl)
                    val resolvedImageUrl = TvApiClient.resolveMediaUrl(state.demoImageUrl)

                    if (!resolvedVideoUrl.isNullOrBlank()) {
                        TvExerciseVideoPlayer(
                            videoUrl = resolvedVideoUrl,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 8.dp
                        )
                    } else if (!resolvedImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = resolvedImageUrl,
                            contentDescription = state.exerciseName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = KynectiaLime.copy(alpha = 0.6f),
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Demostración del Ejercicio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Segmentos de progreso de rutina
                TvExerciseSegmentsIndicator(
                    totalExercises = state.totalExercises,
                    currentIndex = state.currentExerciseIndex
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. TARJETA COMPACTA DE DESCANSO O MÉTRICAS ACTIVAS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isResting) Color(0xFF0F1A08) else DarkSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (state.isResting) KynectiaLime.copy(alpha = 0.7f) else DarkBorder
            )
        ) {
            if (state.isResting) {
                TvRestModeVerticalContent(
                    restRemaining = state.restSecondsRemaining,
                    restTotal = state.restSecondsTotal,
                    nextSet = state.currentSetNumber,
                    totalSets = state.totalSets
                )
            } else {
                TvActiveSetVerticalContent(
                    currentSet = state.currentSetNumber,
                    totalSets = state.totalSets,
                    reps = state.reps,
                    weightKg = state.weightKg,
                    oneRm = calculatedOneRm
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. BARRA DE CONTROL REMOTO COMPACTA
        TvRemoteControlBar(
            isPaused = state.isPaused,
            isResting = state.isResting,
            isSoundEnabled = isSoundEnabled,
            orientationLabel = orientationMode.label,
            onTogglePause = onTogglePause,
            onSkipRest = onSkipRest,
            onNextExercise = onNextExercise,
            onPrevExercise = onPrevExercise,
            onRotateScreen = onRotateScreen,
            onToggleSound = onToggleSound,
            onDisconnect = onDisconnect,
            isCompact = true
        )
    }
}

/**
 * COMPONENTE: Barra de segmentos horizontales de ejercicios
 */
@Composable
fun TvExerciseSegmentsIndicator(
    totalExercises: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val total = maxOf(1, totalExercises)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until total) {
            val isActive = i == currentIndex
            val isCompleted = i < currentIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when {
                            isActive -> KynectiaBlueLight
                            isCompleted -> KynectiaLime
                            else -> Color(0x35FFFFFF)
                        }
                    )
            )
        }
    }
}

/**
 * COMPONENTE: Visualizador de series del ejercicio
 */
@Composable
fun TvSetsTracker(
    totalSets: Int,
    currentSet: Int,
    weightKg: Double,
    reps: Int,
    isResting: Boolean,
    isCompact: Boolean = false
) {
    val count = maxOf(1, totalSets)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
    ) {
        for (s in 1..count) {
            val isCompleted = if (isResting) s <= currentSet else s < currentSet
            val isCurrent = if (isResting) s == currentSet + 1 else s == currentSet

            val borderColor = when {
                isCurrent -> KynectiaBlueLight
                isCompleted -> KynectiaLime.copy(alpha = 0.5f)
                else -> DarkBorder
            }
            val bgColor = when {
                isCurrent -> Color(0xFF101B29)
                isCompleted -> Color(0xFF12200E)
                else -> DarkSurfaceVariant
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = bgColor,
                border = androidx.compose.foundation.BorderStroke(if (isCurrent) 1.5.dp else 1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = if (isCompact) 4.dp else 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "S$s",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) KynectiaBlueLight else if (isCompleted) KynectiaLime else TextSecondary,
                            fontSize = if (isCompact) 10.sp else 11.sp
                        )
                        if (isCompleted) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = KynectiaLime,
                                modifier = Modifier.size(if (isCompact) 10.dp else 12.dp)
                            )
                        }
                    }
                    if (!isCompact) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (weightKg > 0) "${weightKg.toInt()}k" else "-",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) TextPrimary else TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$reps r",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) KynectiaBlueLight else TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * CONTENIDO DE SERIE ACTIVA - HORIZONTAL
 */
@Composable
private fun TvActiveSetContent(
    currentSet: Int,
    totalSets: Int,
    reps: Int,
    weightKg: Double,
    oneRm: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // SERIE ACTUAL
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SERIE ACTUAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$currentSet / $totalSets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = KynectiaLime
                    )
                }

                // 1RM Estimado Badge
                if (oneRm > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF101B2B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22344A))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "1RM ESTIMADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = "$oneRm kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = KynectiaBlueLight
                            )
                        }
                    }
                }
            }
        }

        // CARDS REPS Y CARGA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "REPETICIONES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "$reps",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "reps objetivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CARGA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = if (weightKg > 0) String.format(Locale.US, "%.1f", weightKg) else "0.0",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = KynectiaOrange
                    )
                    Text(
                        text = "kilogramos",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // TABLA DE SERIES DEL EJERCICIO (Visual Tracker)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Seguimiento de series",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            TvSetsTracker(
                totalSets = totalSets,
                currentSet = currentSet,
                weightKg = weightKg,
                reps = reps,
                isResting = false
            )
        }

        // BANNER DE CADENCIA & TEMPO
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF131D27),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Timelapse,
                    contentDescription = null,
                    tint = KynectiaBlueLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cadencia 3-1-1-0: 3s bajada excéntrica • 1s pausa isométrica • 1s empuje concéntrico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * CONTENIDO DE SERIE ACTIVA - VERTICAL
 */
@Composable
private fun TvActiveSetVerticalContent(
    currentSet: Int,
    totalSets: Int,
    reps: Int,
    weightKg: Double,
    oneRm: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. SERIE
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SERIE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "$currentSet/$totalSets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = KynectiaLime
                    )
                }
            }

            // 2. REPS
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "REPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "$reps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }

            // 3. CARGA
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CARGA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (weightKg > 0) "${weightKg.toInt()}k" else "0k",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = KynectiaOrange
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Seguimiento compacto de series
        TvSetsTracker(
            totalSets = totalSets,
            currentSet = currentSet,
            weightKg = weightKg,
            reps = reps,
            isResting = false,
            isCompact = true
        )
    }
}

/**
 * CONTENIDO DE MODO DESCANSO - HORIZONTAL
 */
@Composable
private fun TvRestModeContent(
    restRemaining: Int,
    restTotal: Int,
    nextSet: Int,
    totalSets: Int,
    weightKg: Double,
    reps: Int
) {
    val progress = if (restTotal > 0) {
        (restRemaining.toFloat() / restTotal).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "restProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF263D04)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = KynectiaLime,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TIEMPO DE RECUPERACIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = KynectiaLime,
                    letterSpacing = 1.sp
                )
            }
        }

        // Anillo de Cuenta Regresiva Adaptado
        Box(
            modifier = Modifier.size(175.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                drawCircle(
                    color = Color(0xFF1B2A10),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(KynectiaLime, Color(0xFF70FF00), KynectiaLime)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${restRemaining}s",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 46.sp),
                    fontWeight = FontWeight.Black,
                    color = KynectiaLime
                )
                Text(
                    text = "DESCANSO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }

        // Seguimiento de series en descanso
        TvSetsTracker(
            totalSets = totalSets,
            currentSet = nextSet - 1,
            weightKg = weightKg,
            reps = reps,
            isResting = true
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PREPARAR SIGUIENTE SERIE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Serie $nextSet de $totalSets • ${weightKg.toInt()} kg x $reps reps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = KynectiaLime,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * CONTENIDO DE MODO DESCANSO - VERTICAL
 */
@Composable
private fun TvRestModeVerticalContent(
    restRemaining: Int,
    restTotal: Int,
    nextSet: Int,
    totalSets: Int
) {
    val progress = if (restTotal > 0) {
        (restRemaining.toFloat() / restTotal).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "restProgressVert"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Anillo compacto de cuenta regresiva
        Box(
            modifier = Modifier.size(95.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                drawCircle(
                    color = DarkSurfaceVariant,
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(KynectiaLime, Color(0xFF00E5FF), KynectiaLime)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${restRemaining}s",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info de descanso y siguiente serie
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF263D04)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = KynectiaLime,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DESCANSO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = KynectiaLime,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Siguiente: Serie $nextSet de $totalSets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 13.sp
            )

            Text(
                text = "Pulsa [OK] para saltar",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Barra Interactiva de Control Remoto de TV
 */
@Composable
private fun TvRemoteControlBar(
    isPaused: Boolean,
    isResting: Boolean,
    isSoundEnabled: Boolean,
    orientationLabel: String,
    onTogglePause: () -> Unit,
    onSkipRest: () -> Unit,
    onNextExercise: () -> Unit,
    onPrevExercise: () -> Unit,
    onRotateScreen: () -> Unit,
    onToggleSound: () -> Unit,
    onDisconnect: () -> Unit,
    isCompact: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Botón Acción Principal: Pausa / Continuar / Saltar descanso
                TvRemoteActionButton(
                    icon = if (isResting) Icons.AutoMirrored.Filled.ArrowForward else if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    label = if (isResting) "Saltar" else if (isPaused) "Continuar" else "Pausar",
                    shortcutKey = "OK",
                    onClick = {
                        if (isResting) onSkipRest() else onTogglePause()
                    },
                    isPrimary = true
                )

                // Anterior
                TvRemoteActionButton(
                    icon = Icons.Default.SkipPrevious,
                    label = if (!isCompact) "Anterior" else "",
                    shortcutKey = "◀",
                    onClick = onPrevExercise
                )

                // Siguiente
                TvRemoteActionButton(
                    icon = Icons.Default.SkipNext,
                    label = if (!isCompact) "Siguiente" else "",
                    shortcutKey = "▶",
                    onClick = onNextExercise
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Girar Pantalla
                TvRemoteActionButton(
                    icon = Icons.Default.ScreenRotation,
                    label = if (!isCompact) "Girar" else "",
                    shortcutKey = "MENU",
                    onClick = onRotateScreen
                )

                // Sonido ON/OFF
                TvRemoteActionButton(
                    icon = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    label = if (!isCompact) (if (isSoundEnabled) "Audio" else "Mudo") else "",
                    shortcutKey = "2",
                    onClick = onToggleSound
                )

                // Salir
                TvRemoteActionButton(
                    icon = Icons.Default.Close,
                    label = if (!isCompact) "Salir" else "",
                    shortcutKey = "ATRÁS",
                    onClick = onDisconnect
                )
            }
        }
    }
}

@Composable
fun TvRemoteActionButton(
    icon: ImageVector,
    label: String,
    shortcutKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(10.dp),
        color = if (isFocused) KynectiaLime else if (isPrimary) Color(0xFF1B2E00) else DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color.White else if (isPrimary) KynectiaLime else DarkBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isFocused) Color.Black else if (isPrimary) KynectiaLime else TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            if (label.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color.Black else TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isFocused) Color(0x33000000) else DarkSurface
            ) {
                Text(
                    text = shortcutKey,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isFocused) Color.Black else TextMuted
                )
            }
        }
    }
}
