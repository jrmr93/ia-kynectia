package com.kynectia.tv.ui.workout

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 36.dp, vertical = 22.dp)
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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

            // Indicador de estado y controles rápidos
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // LEFT COLUMN: Ejercicio actual e Imagen
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
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
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

                        Text(
                            text = "SERIES: ${state.totalSets}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }

                    Text(
                        text = state.exerciseName.ifBlank { "Preparando ejercicio..." },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progreso de rutina
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progreso de rutina",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            val progressPct = if (state.totalExercises > 0) {
                                ((state.currentExerciseIndex.toFloat() / state.totalExercises) * 100).toInt()
                            } else 0
                            Text(
                                text = "$progressPct%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = KynectiaLime
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (state.totalExercises > 0) {
                                    (state.currentExerciseIndex.toFloat() / state.totalExercises).coerceIn(0f, 1f)
                                } else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = KynectiaLime,
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }
            }

            // RIGHT COLUMN: Modo Descanso o Métricas
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
                        totalSets = state.totalSets
                    )
                } else {
                    TvActiveSetContent(
                        currentSet = state.currentSetNumber,
                        totalSets = state.totalSets,
                        reps = state.reps,
                        weightKg = state.weightKg
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
 * Ocupa el 100% del alto y ancho con tipografía y proporciones maximizadas.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
                Spacer(modifier = Modifier.width(10.dp))
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
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (state.isPaused) KynectiaOrange else KynectiaLime.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. TARJETA PRINCIPAL DEL EJERCICIO - IMAGEN GRANDE Y VISIBLE AL 100% QUE DOMINA LA PANTALLA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2.2f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Fila superior de badges
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "SERIE ${state.currentSetNumber} DE ${state.totalSets}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = KynectiaLime
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = state.exerciseName.ifBlank { "Preparando ejercicio..." },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // IMAGEN DEL EJERCICIO - OCUPA EL ESPACIO PRINCIPAL DE LA PANTALLA
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = KynectiaLime.copy(alpha = 0.6f),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Demostración del Ejercicio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Barra de progreso de rutina
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progreso de rutina",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        val progressPct = if (state.totalExercises > 0) {
                            ((state.currentExerciseIndex.toFloat() / state.totalExercises) * 100).toInt()
                        } else 0
                        Text(
                            text = "$progressPct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = KynectiaLime,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (state.totalExercises > 0) {
                                (state.currentExerciseIndex.toFloat() / state.totalExercises).coerceIn(0f, 1f)
                            } else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = KynectiaLime,
                        trackColor = DarkSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. TARJETA COMPACTA DE DESCANSO O MÉTRICAS ACTIVAS (DISTRIBUCIÓN 100% HORIZONTAL)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f),
            shape = RoundedCornerShape(20.dp),
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
                    weightKg = state.weightKg
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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

@Composable
private fun TvActiveSetVerticalContent(
    currentSet: Int,
    totalSets: Int,
    reps: Int,
    weightKg: Double
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. SERIE
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SERIE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currentSet/$totalSets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = KynectiaLime
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "en curso",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // 2. REPETICIONES
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "REPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$reps",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "objetivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // 3. CARGA
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CARGA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (weightKg > 0) String.format(Locale.US, "%.1f", weightKg) else "0.0",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = KynectiaOrange
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF131D27),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = KynectiaLime,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Controla cada repetición con técnica estricta y rango completo.",
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Anillo compacto de cuenta regresiva
        Box(
            modifier = Modifier.size(105.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                // Pista fondo
                drawCircle(
                    color = DarkSurfaceVariant,
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Arco de progreso
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

        Spacer(modifier = Modifier.width(16.dp))

        // Info de descanso y siguiente serie
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF263D04)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = KynectiaLime,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TIEMPO DE DESCANSO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = KynectiaLime,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Siguiente: Serie $nextSet de $totalSets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Respira y recupera. Pulsa [OK] para saltar.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

@Composable
private fun TvActiveSetContent(
    currentSet: Int,
    totalSets: Int,
    reps: Int,
    weightKg: Double
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SERIE ACTUAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = TextSecondary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "$currentSet / $totalSets",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = KynectiaLime
                )
            }
        }

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
                        .padding(14.dp),
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
                        .padding(14.dp),
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
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = KynectiaLime,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Controla cada repetición con técnica estricta y rango completo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TvRestModeContent(
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
            modifier = Modifier.size(190.dp),
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
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp),
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
                        text = "Serie $nextSet de $totalSets",
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
