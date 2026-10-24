package com.kynectia.app.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kynectia.app.data.api.ApiClient
import com.kynectia.app.data.model.Exercise
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
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val isTvConnected by TvCastManager.isTvConnected.collectAsState()

    // Modales y menús interactivos
    var showTvCastDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showSwapDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showAiCoachDialog by remember { mutableStateOf(false) }
    var showRestTimerPicker by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var editingSetIndex by remember { mutableStateOf<Int?>(null) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

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

    if (routine == null || routine.exercises.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = KynectiaBlueLight)
        }
        return
    }

    val currentExercise = routine.exercises.getOrNull(state.currentExerciseIndex) ?: routine.exercises.first()
    val currentSets = state.exerciseSetsMap[currentExercise.exercise_id] ?: emptyList()
    val activeSet = currentSets.getOrNull(state.selectedSetIndex)

    // Cálculo dinámico de 1RM según fórmula Epley: Weight * (1 + Reps / 30)
    val currentWeight = activeSet?.weightKg ?: state.weightKgInput
    val currentReps = activeSet?.reps ?: state.completedRepsInput
    val calculatedOneRm = remember(currentWeight, currentReps) {
        if (currentWeight <= 0.0 || currentReps <= 0) 0
        else if (currentReps == 1) currentWeight.toInt()
        else (currentWeight * (1.0 + currentReps / 30.0)).toInt()
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            // Barra inferior con Nota, AI Coach y botón grande azul Log Set
            WorkoutBottomControlBar(
                isAllSetsCompleted = currentSets.isNotEmpty() && currentSets.all { it.isCompleted },
                isLastExercise = state.currentExerciseIndex == routine.exercises.size - 1,
                onNoteClick = { showNotesDialog = true },
                onAiCoachClick = { showAiCoachDialog = true },
                onLogSetClick = {
                    if (currentSets.isNotEmpty() && currentSets.all { it.isCompleted }) {
                        if (state.currentExerciseIndex < routine.exercises.size - 1) {
                            viewModel.nextExercise()
                        } else {
                            viewModel.finishWorkout()
                        }
                    } else {
                        viewModel.logActiveSet()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. BANNER MULTIMEDIA SUPERIOR CON BOTONES FLOTANTES Y SEGMENTOS DE PROGRESO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .background(Color.Black)
                ) {
                    val resolvedVideoUrl = ApiClient.resolveMediaUrl(context, currentExercise.demo_video_url)
                    val resolvedImageUrl = ApiClient.resolveMediaUrl(context, currentExercise.demo_image_url)

                    if (!resolvedVideoUrl.isNullOrBlank()) {
                        ExerciseVideoPlayer(
                            videoUrl = resolvedVideoUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!resolvedImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = resolvedImageUrl,
                            contentDescription = currentExercise.exercise_name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder visual elegante si no hay video ni imagen
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Color(0xFF2A3442),
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    // Sombra/Gradiente inferior sutil
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, DarkBackground)
                                )
                            )
                    )

                    // Botones de acción flotantes en la parte superior
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Volver / Atrás (<)
                        FloatingTopIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            onClick = {
                                if (state.completedSetsByExercise.isNotEmpty()) {
                                    showExitConfirmDialog = true
                                } else {
                                    onBack()
                                }
                            }
                        )

                        // Grupo de botones a la derecha: Estadísticas, Reemplazar y Más
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Estadísticas / Gráfica (Bar Chart)
                            FloatingTopIconButton(
                                icon = Icons.Default.Leaderboard,
                                contentDescription = "Estadísticas del ejercicio",
                                onClick = { showStatsDialog = true }
                            )

                            // Sustituir ejercicio (Sync / Swap)
                            FloatingTopIconButton(
                                icon = Icons.Default.Autorenew,
                                contentDescription = "Sustituir ejercicio",
                                onClick = { showSwapDialog = true }
                            )

                            // Menú más opciones (...)
                            Box {
                                FloatingTopIconButton(
                                    icon = Icons.Default.MoreHoriz,
                                    contentDescription = "Más opciones",
                                    onClick = { showMoreMenu = true }
                                )

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier.background(DarkSurfaceVariant)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("📺 Transmitir a TV", color = TextPrimary) },
                                        onClick = {
                                            showMoreMenu = false
                                            showTvCastDialog = true
                                        }
                                    )
                                    if (isTvConnected) {
                                        DropdownMenuItem(
                                            text = { Text("🔄 Girar Pantalla TV", color = TextPrimary) },
                                            onClick = {
                                                showMoreMenu = false
                                                TvCastManager.rotateTvScreen(context)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (state.isPaused) "▶️ Reanudar Rutina" else "⏸️ Pausar Rutina",
                                                color = TextPrimary
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.togglePause()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🏁 Finalizar Entrenamiento", color = KynectiaOrange) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.finishWorkout()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Segmentos de progreso de los ejercicios de la rutina (como en la imagen)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        routine.exercises.forEachIndexed { index, _ ->
                            val isActive = index == state.currentExerciseIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isActive) KynectiaBlueLight else Color(0x55FFFFFF)
                                    )
                                    .clickable { viewModel.jumpToExercise(index) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. CABECERA DEL EJERCICIO: NOMBRE Y 1RM
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentExercise.exercise_name ?: "Ejercicio #${currentExercise.sequence_order}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // 1RM: 76
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "1RM: ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = "$calculatedOneRm",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = KynectiaBlueLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // CHIPS RÁPIDOS: Rest timer 01:00 e Interval
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Rest timer 01:00
                        val restDurationSeconds = if (state.isResting) state.restSecondsRemaining else state.restSecondsTotal
                        val restMinutes = restDurationSeconds / 60
                        val restSecs = restDurationSeconds % 60
                        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", restMinutes, restSecs)

                        QuickTimerChip(
                            icon = Icons.Default.AccessTime,
                            title = "Rest timer ",
                            value = formattedTime,
                            isHighlighted = state.isResting,
                            onClick = { showRestTimerPicker = true }
                        )

                        // Interval
                        QuickTimerChip(
                            icon = Icons.Default.Timer,
                            title = "Interval",
                            value = "",
                            isHighlighted = false,
                            onClick = { showIntervalDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. TABLA DE SERIES (Set | Kg | Reps (i) | Check)
                    SetsTableHeader()

                    Spacer(modifier = Modifier.height(6.dp))

                    // Lista de series
                    currentSets.forEachIndexed { index, setItem ->
                        val isSelected = index == state.selectedSetIndex
                        SetTableRow(
                            set = setItem,
                            isSelected = isSelected,
                            onSelectSet = { viewModel.selectSet(index) },
                            onEditClick = { editingSetIndex = index },
                            onToggleCompleted = { viewModel.toggleSetCompleted(index) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Fila de adición / eliminación (+  60  8  Delete)
                    val lastSet = currentSets.lastOrNull()
                    val ghostKg = lastSet?.weightKg?.toInt() ?: 60
                    val ghostReps = lastSet?.reps ?: 8

                    AddSetTableRow(
                        ghostKg = ghostKg,
                        ghostReps = ghostReps,
                        canDelete = currentSets.size > 1,
                        onAddSet = { viewModel.addSet() },
                        onDeleteLastSet = { viewModel.deleteLastSet() }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Alerta flotante si el descanso está activo con controles rápidos
                    AnimatedVisibility(
                        visible = state.isResting,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        RestingMiniBanner(
                            secondsRemaining = state.restSecondsRemaining,
                            onAddSeconds = { viewModel.addRestTime(it) },
                            onSkipRest = { viewModel.skipRest() }
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // ==========================================
    // MODALES Y DIÁLOGOS DE CONTROL
    // ==========================================

    // Diálogo para editar Peso y Repeticiones de una serie
    editingSetIndex?.let { setIdx ->
        val targetSet = currentSets.getOrNull(setIdx)
        if (targetSet != null) {
            EditSetDialog(
                setNumber = targetSet.setNumber,
                initialWeight = targetSet.weightKg,
                initialReps = targetSet.reps,
                onDismiss = { editingSetIndex = null },
                onSave = { newWeight, newReps ->
                    viewModel.updateSetWeight(setIdx, newWeight)
                    viewModel.updateSetReps(setIdx, newReps)
                    viewModel.selectSet(setIdx)
                    editingSetIndex = null
                }
            )
        }
    }

    // Selector de tiempo de descanso
    if (showRestTimerPicker) {
        RestTimerPickerDialog(
            currentSeconds = state.restSecondsTotal,
            onDismiss = { showRestTimerPicker = false },
            onSelectSeconds = { newSeconds ->
                viewModel.setRestSecondsTotal(newSeconds)
                showRestTimerPicker = false
            }
        )
    }

    // Diálogo informativo de Intervalo y Cadencia
    if (showIntervalDialog) {
        IntervalInfoDialog(onDismiss = { showIntervalDialog = false })
    }

    // Diálogo de estadísticas del ejercicio actual
    if (showStatsDialog) {
        ExerciseStatsDialog(
            exerciseName = currentExercise.exercise_name ?: "Ejercicio",
            oneRm = calculatedOneRm,
            totalSets = currentSets.size,
            completedSets = currentSets.count { it.isCompleted },
            totalVolumeKg = currentSets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps },
            onDismiss = { showStatsDialog = false }
        )
    }

    // Diálogo para sustituir ejercicio
    if (showSwapDialog) {
        SwapExerciseDialog(
            currentExerciseName = currentExercise.exercise_name ?: "Ejercicio",
            catalog = state.availableExercisesCatalog,
            onDismiss = { showSwapDialog = false },
            onSelectExercise = { replacement ->
                viewModel.swapCurrentExercise(replacement)
                showSwapDialog = false
            }
        )
    }

    // Diálogo para notas del ejercicio
    if (showNotesDialog) {
        val existingNote = state.exerciseNotes[currentExercise.exercise_id] ?: ""
        ExerciseNotesDialog(
            exerciseName = currentExercise.exercise_name ?: "Ejercicio",
            initialNote = existingNote,
            onDismiss = { showNotesDialog = false },
            onSave = { note ->
                viewModel.saveExerciseNote(note)
                showNotesDialog = false
            }
        )
    }

    // Diálogo de Consejos AI Coach
    if (showAiCoachDialog) {
        AiCoachTipsDialog(
            exerciseName = currentExercise.exercise_name ?: "Ejercicio",
            instructions = currentExercise.specific_instructions,
            onDismiss = { showAiCoachDialog = false }
        )
    }

    // Diálogo de confirmación para salir
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("¿Abandonar entrenamiento?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("El progreso no guardado de la sesión se perderá.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        onBack()
                    }
                ) {
                    Text("Abandonar", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Continuar", color = TextPrimary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

// ==========================================
// COMPONENTES AUXILIARES DE LA UI
// ==========================================

@Composable
fun FloatingTopIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x80252830))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun QuickTimerChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHighlighted) Color(0xFF1E2838) else Color(0xFF1A1F26))
            .border(
                width = 1.dp,
                color = if (isHighlighted) KynectiaBlueLight else Color(0xFF28313D),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlighted) KynectiaBlueLight else TextSecondary,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = KynectiaBlueLight
                )
            }
        }
    }
}

@Composable
fun SetsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Set",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF7A8696),
            modifier = Modifier.width(50.dp)
        )

        Text(
            text = "Kg",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF7A8696),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reps",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7A8696)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = Color(0xFF7A8696),
                modifier = Modifier.size(13.dp)
            )
        }

        Box(
            modifier = Modifier.width(50.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Espacio alineado para la columna de checkmark
        }
    }
}

@Composable
fun SetTableRow(
    set: ExerciseSetState,
    isSelected: Boolean,
    onSelectSet: () -> Unit,
    onEditClick: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    val textColor = if (isSelected) KynectiaBlueLight else TextPrimary
    val textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onSelectSet),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna Set
        Text(
            text = "${set.setNumber}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) TextPrimary else TextSecondary,
            modifier = Modifier.width(50.dp)
        )

        // Columna Kg
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEditClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (set.weightKg % 1.0 == 0.0) "${set.weightKg.toInt()}" else String.format(Locale.getDefault(), "%.1f", set.weightKg),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = textColor,
                textDecoration = textDecoration
            )
        }

        // Columna Reps
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEditClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${set.reps}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = textColor,
                textDecoration = textDecoration
            )
        }

        // Columna Checkmark
        Box(
            modifier = Modifier
                .width(50.dp)
                .clickable(onClick = onToggleCompleted),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (set.isCompleted) "Completada" else "Pendiente",
                tint = if (set.isCompleted) KynectiaBlueLight else Color(0xFF2E343E),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AddSetTableRow(
    ghostKg: Int,
    ghostReps: Int,
    canDelete: Boolean,
    onAddSet: () -> Unit,
    onDeleteLastSet: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón +
        Box(
            modifier = Modifier
                .width(50.dp)
                .clickable(onClick = onAddSet),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar serie",
                tint = Color(0xFF7A8696),
                modifier = Modifier.size(22.dp)
            )
        }

        // Ghost Kg
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onAddSet),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$ghostKg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38404D)
            )
        }

        // Ghost Reps
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onAddSet),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$ghostReps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38404D)
            )
        }

        // Botón Delete
        Box(
            modifier = Modifier
                .width(64.dp)
                .clickable(enabled = canDelete, onClick = onDeleteLastSet),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "Delete",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (canDelete) Color(0xFF6E7A8A) else Color(0xFF38404D)
            )
        }
    }
}

@Composable
fun RestingMiniBanner(
    secondsRemaining: Int,
    onAddSeconds: (Int) -> Unit,
    onSkipRest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E28)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(KynectiaBlueLight)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = KynectiaBlueLight,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Descanso activo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${secondsRemaining}s restantes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KynectiaBlueLight
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAddSeconds(15) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3542))
                ) {
                    Text("+15s", fontSize = 12.sp)
                }

                Button(
                    onClick = onSkipRest,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KynectiaBlueLight)
                ) {
                    Text("Saltar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun WorkoutBottomControlBar(
    isAllSetsCompleted: Boolean,
    isLastExercise: Boolean,
    onNoteClick: () -> Unit,
    onAiCoachClick: () -> Unit,
    onLogSetClick: () -> Unit
) {
    Surface(
        color = DarkBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de Notas (Cuadrado redondeado oscuro con icono nota)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B2028))
                    .border(1.dp, Color(0xFF28313D), RoundedCornerShape(14.dp))
                    .clickable(onClick = onNoteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Notas del ejercicio",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Botón de AI Coach (Cuadrado redondeado oscuro con icono destello)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B2028))
                    .border(1.dp, Color(0xFF28313D), RoundedCornerShape(14.dp))
                    .clickable(onClick = onAiCoachClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Consejos IA",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Botón Principal Azul: Log Set / Siguiente Ejercicio / Finalizar
            Button(
                onClick = onLogSetClick,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KynectiaBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isAllSetsCompleted) {
                        if (isLastExercise) "Finalizar Entrenamiento" else "Siguiente Ejercicio"
                    } else {
                        "Log Set"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

// ==========================================
// DIÁLOGOS DETALLADOS
// ==========================================

@Composable
fun EditSetDialog(
    setNumber: Int,
    initialWeight: Double,
    initialReps: Int,
    onDismiss: () -> Unit,
    onSave: (Double, Int) -> Unit
) {
    var weight by remember { mutableDoubleStateOf(initialWeight) }
    var reps by remember { mutableIntStateOf(initialReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modificar Serie #$setNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stepper de Carga (Kg)
                Column {
                    Text("Carga (Kg)", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { weight = maxOf(0.0, weight - 2.5) },
                            modifier = Modifier.size(40.dp).background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Text("-2.5", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { weight = maxOf(0.0, weight - 0.5) },
                            modifier = Modifier.size(36.dp).background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Text(
                            text = if (weight % 1.0 == 0.0) "${weight.toInt()} kg" else String.format(Locale.getDefault(), "%.1f kg", weight),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KynectiaBlueLight
                        )
                        IconButton(
                            onClick = { weight += 0.5 },
                            modifier = Modifier.size(36.dp).background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        IconButton(
                            onClick = { weight += 2.5 },
                            modifier = Modifier.size(40.dp).background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Text("+2.5", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Stepper de Repeticiones
                Column {
                    Text("Repeticiones", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { reps = maxOf(1, reps - 5) },
                            modifier = Modifier.size(40.dp).background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Text("-5", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { reps = maxOf(1, reps - 1) },
                            modifier = Modifier.size(36.dp).background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Text(
                            text = "$reps reps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = { reps += 1 },
                            modifier = Modifier.size(36.dp).background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        IconButton(
                            onClick = { reps += 5 },
                            modifier = Modifier.size(40.dp).background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Text("+5", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(weight, reps) },
                colors = ButtonDefaults.buttonColors(containerColor = KynectiaBlue)
            ) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun RestTimerPickerDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onSelectSeconds: (Int) -> Unit
) {
    val presets = listOf(30, 45, 60, 90, 120, 180)
    var selected by remember { mutableIntStateOf(currentSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("⏱️ Tiempo de Descanso", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Selecciona los segundos de pausa entre series:", color = TextSecondary, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.take(3).forEach { sec ->
                        OutlinedButton(
                            onClick = { selected = sec },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected == sec) KynectiaBlueLight else Color.Transparent,
                                contentColor = if (selected == sec) Color.White else TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected == sec) KynectiaBlueLight else DarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("${sec}s", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.drop(3).forEach { sec ->
                        OutlinedButton(
                            onClick = { selected = sec },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected == sec) KynectiaBlueLight else Color.Transparent,
                                contentColor = if (selected == sec) Color.White else TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected == sec) KynectiaBlueLight else DarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("${sec}s", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSelectSeconds(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = KynectiaBlue)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun IntervalInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("⏱️ Modo Intervalo & Tempo", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Cadencia recomendada: 3-1-1-0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KynectiaBlueLight
                )
                Text(
                    text = "• 3s Fase excéntrica (bajada controlada)\n• 1s Pausa isométrica (estiramiento muscular)\n• 1s Fase concéntrica (empuje explosivo)\n• 0s Pausa arriba",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = KynectiaBlue)) {
                Text("Entendido", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun ExerciseStatsDialog(
    exerciseName: String,
    oneRm: Int,
    totalSets: Int,
    completedSets: Int,
    totalVolumeKg: Double,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("📊 Estadísticas: $exerciseName", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1RM Estimado (Fórmula Epley)", color = TextSecondary, fontSize = 12.sp)
                        Text("$oneRm kg", color = KynectiaBlueLight, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Series", color = TextSecondary, fontSize = 12.sp)
                            Text("$completedSets / $totalSets", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Volumen Sesión", color = TextSecondary, fontSize = 12.sp)
                            Text("${totalVolumeKg.toInt()} kg", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = KynectiaBlue)) {
                Text("Cerrar", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun SwapExerciseDialog(
    currentExerciseName: String,
    catalog: List<Exercise>,
    onDismiss: () -> Unit,
    onSelectExercise: (Exercise) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, catalog) {
        if (searchQuery.isBlank()) catalog else catalog.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sustituir: $currentExerciseName", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar ejercicio alternativo...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = KynectiaBlueLight,
                        unfocusedBorderColor = DarkBorder
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { ex ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectExercise(ex) },
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text(
                                        ex.primary_muscle_name ?: ex.exercise_type,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = KynectiaBlueLight)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun ExerciseNotesDialog(
    exerciseName: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("📝 Notas: $exerciseName", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Anota sensaciones, ajustes de agarre, o carga para la próxima sesión:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Ej. Agarre ligeramente más abierto. Buen estímulo en pectoral medio.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = KynectiaBlueLight,
                        unfocusedBorderColor = DarkBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(note) }, colors = ButtonDefaults.buttonColors(containerColor = KynectiaBlue)) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun AiCoachTipsDialog(
    exerciseName: String,
    instructions: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = KynectiaBlueLight)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kynectia AI Coach ⚡", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Claves biomecánicas para $exerciseName:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "🎯 Retracción escapular firme contra el respaldo para proteger los hombros.\n🎯 Leg drive: empuja el suelo con los talones para máxima estabilidad.\n🎯 Codos a 45° con respecto al torso para optimizar la activación del pectoral mayor.\n🎯 Respiración: Inhala profundo en la bajada, exhala con fuerza al empujar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                if (!instructions.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 Instrucción del Entrenador: $instructions",
                        style = MaterialTheme.typography.bodySmall,
                        color = KynectiaLime
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = KynectiaBlue)) {
                Text("Entendido", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}
